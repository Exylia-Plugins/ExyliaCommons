package net.exylia.commons.region.schematic;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RegenerationScheduler {
    private static final int MAX_CONCURRENT_OPERATIONS = 4;
    private static final int TASK_PROCESS_INTERVAL_TICKS = 1;
    private static final int MEMORY_CHECK_INTERVAL_TICKS = 200;
    private static final long LOW_MEMORY_THRESHOLD_MB = 256;

    private final JavaPlugin plugin;
    private final BlockPlacer blockPlacer;
    private final SchematicIOManager ioManager;

    private final PriorityBlockingQueue<RegenerationTask> taskQueue;
    private final ConcurrentMap<String, Boolean> activeOperations;

    private final AtomicInteger activeTasks;
    private final AtomicBoolean isShuttingDown;

    private ScheduledTask taskProcessor;
    private ScheduledTask memoryMonitor;

    private final RegenerationMetrics metrics;

    public RegenerationScheduler(JavaPlugin plugin, BlockPlacer blockPlacer, SchematicIOManager ioManager) {
        this.plugin = plugin;
        this.blockPlacer = blockPlacer;
        this.ioManager = ioManager;

        this.taskQueue = new PriorityBlockingQueue<>(100, Comparator.comparingInt(t -> t.priority));
        this.activeOperations = new ConcurrentHashMap<>();
        this.activeTasks = new AtomicInteger(0);
        this.isShuttingDown = new AtomicBoolean(false);
        this.metrics = new RegenerationMetrics();

        startTaskProcessor();
        startMemoryMonitor();
    }

    private void startTaskProcessor() {
        taskProcessor = Schedulers.syncTimer(() -> {
            if (isShuttingDown.get()) return;
            processNextTask();
        }, TASK_PROCESS_INTERVAL_TICKS, TASK_PROCESS_INTERVAL_TICKS);
    }

    private void startMemoryMonitor() {
        memoryMonitor = Schedulers.asyncTimer(() -> {
            checkMemoryAndCleanup();
        }, MEMORY_CHECK_INTERVAL_TICKS, MEMORY_CHECK_INTERVAL_TICKS);
    }

    private void processNextTask() {
        if (activeTasks.get() >= MAX_CONCURRENT_OPERATIONS) {
            return;
        }

        RegenerationTask task = taskQueue.poll();
        if (task == null) {
            return;
        }

        activeTasks.incrementAndGet();

        Schedulers.runAsyncTask(() -> {
            try {
                executeTask(task);
            } catch (Exception e) {
                DebugUtils.logInternalError("Error executing regeneration task: " + e.getMessage());
                task.future.completeExceptionally(e);
            } finally {
                activeTasks.decrementAndGet();
                activeOperations.remove(task.id);
            }
        });
    }

    private void executeTask(RegenerationTask task) {
        long startTime = System.currentTimeMillis();

        try {
            DebugUtils.logInternalDebug("[RegenerationScheduler] === TASK START: " + task.type + " for " + task.id);
            DebugUtils.logInternalDebug("[RegenerationScheduler] Active operations: " + activeTasks.get() + "/" + MAX_CONCURRENT_OPERATIONS);

            long phaseStart = System.currentTimeMillis();
            switch (task.type) {
                case SAVE:
                    DebugUtils.logInternalDebug("[RegenerationScheduler] Executing SAVE task");
                    boolean saveSuccess = executeSave(task);
                    long saveDuration = System.currentTimeMillis() - phaseStart;
                    DebugUtils.logInternalDebug("[RegenerationScheduler] SAVE completed in " + saveDuration + "ms");
                    task.future.complete(saveSuccess);
                    break;
                case PASTE:
                    DebugUtils.logInternalDebug("[RegenerationScheduler] Executing PASTE task");
                    boolean pasteSuccess = executePaste(task);
                    long pasteDuration = System.currentTimeMillis() - phaseStart;
                    DebugUtils.logInternalDebug("[RegenerationScheduler] PASTE completed in " + pasteDuration + "ms");
                    task.future.complete(pasteSuccess);
                    break;
                case REGENERATE:
                    DebugUtils.logInternalDebug("[RegenerationScheduler] Executing REGENERATE task");
                    boolean regenSuccess = executeRegenerate(task);
                    long regenDuration = System.currentTimeMillis() - phaseStart;
                    DebugUtils.logInternalDebug("[RegenerationScheduler] REGENERATE completed in " + regenDuration + "ms");
                    task.future.complete(regenSuccess);
                    break;
            }

            long duration = System.currentTimeMillis() - startTime;
            DebugUtils.logInternalDebug("[RegenerationScheduler] === TASK END: " + task.type + " for " + task.id + " (took " + duration + "ms)");
            metrics.recordSuccess(duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            DebugUtils.logInternalError("[RegenerationScheduler] ERROR in task execution (after " + duration + "ms): " + e.getMessage());
            e.printStackTrace();
            metrics.recordFailure();
            task.future.completeExceptionally(e);
        }
    }

    private boolean executeSave(RegenerationTask task) {
        try {
            DebugUtils.logInternalDebug("Saving schematic: " + task.id);

            SchematicData data = new SchematicData(
                task.maxBlock.getX() - task.minBlock.getX() + 1,
                task.maxBlock.getY() - task.minBlock.getY() + 1,
                task.maxBlock.getZ() - task.minBlock.getZ() + 1
            );

            SchematicFormat.captureRegion(data, task.minBlock, task.maxBlock);

            return ioManager.saveSchematic(task.id, data).join();

        } catch (Exception e) {
            DebugUtils.logInternalError("Error saving schematic " + task.id + ": " + e.getMessage());
            return false;
        }
    }

    private boolean executePaste(RegenerationTask task) {
        try {
            long loadStart = System.currentTimeMillis();
            SchematicData data = ioManager.loadSchematic(task.id).join();
            long loadDuration = System.currentTimeMillis() - loadStart;
            DebugUtils.logInternalDebug("[RegenerationScheduler] Schematic load completed in " + loadDuration + "ms");

            if (data == null) {
                DebugUtils.logInternalError("[RegenerationScheduler] Schematic is null for " + task.id);
                return false;
            }

            long placeStart = System.currentTimeMillis();
            blockPlacer.placeSchematic(data, task.pasteLocation).join();
            long placeDuration = System.currentTimeMillis() - placeStart;
            DebugUtils.logInternalDebug("[RegenerationScheduler] Schematic placement completed in " + placeDuration + "ms");

            ioManager.invalidateFromCache(task.id);
            DebugUtils.logInternalDebug("[RegenerationScheduler] Pasted schematic: " + task.id);
            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("[RegenerationScheduler] Error pasting schematic " + task.id + ": " + e.getMessage());
            return false;
        }
    }

    private boolean executeRegenerate(RegenerationTask task) {
        try {
            long loadStart = System.currentTimeMillis();
            SchematicData data = ioManager.loadSchematic(task.id).join();
            long loadDuration = System.currentTimeMillis() - loadStart;
            DebugUtils.logInternalDebug("[RegenerationScheduler] Schematic load completed in " + loadDuration + "ms");

            if (data == null) {
                DebugUtils.logInternalError("[RegenerationScheduler] Schematic not found: " + task.id);
                return false;
            }

            long placeStart = System.currentTimeMillis();
            blockPlacer.placeSchematic(data, task.minBlock.getLocation()).join();
            long placeDuration = System.currentTimeMillis() - placeStart;
            DebugUtils.logInternalDebug("[RegenerationScheduler] Schematic placement completed in " + placeDuration + "ms");

            ioManager.invalidateFromCache(task.id);
            DebugUtils.logInternalDebug("[RegenerationScheduler] Regenerated region: " + task.id);
            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("[RegenerationScheduler] Error regenerating " + task.id + ": " + e.getMessage());
            return false;
        }
    }

    private void checkMemoryAndCleanup() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemoryMB = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 1024 / 1024;

        if (freeMemoryMB < LOW_MEMORY_THRESHOLD_MB) {
            DebugUtils.logInternalDebug("Low memory detected (" + freeMemoryMB + "MB), clearing cache");
            ioManager.clearCache();
            System.gc();
        }
    }

    public CompletableFuture<Boolean> queueSave(String id, org.bukkit.block.Block minBlock, org.bukkit.block.Block maxBlock) {
        if (activeOperations.containsKey(id)) {
            return CompletableFuture.completedFuture(false);
        }

        activeOperations.put(id, true);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RegenerationTask task = new RegenerationTask(
            TaskType.SAVE, id, minBlock, maxBlock, null, future, 3
        );

        taskQueue.offer(task);
        return future;
    }

    public CompletableFuture<Boolean> queuePaste(String id, org.bukkit.Location pasteLocation) {
        if (activeOperations.containsKey(id)) {
            return CompletableFuture.completedFuture(false);
        }

        activeOperations.put(id, true);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RegenerationTask task = new RegenerationTask(
            TaskType.PASTE, id, null, null, pasteLocation, future, 2
        );

        taskQueue.offer(task);
        return future;
    }

    public CompletableFuture<Boolean> queueRegenerate(String id, org.bukkit.block.Block minBlock) {
        if (activeOperations.containsKey(id)) {
            return CompletableFuture.completedFuture(false);
        }

        activeOperations.put(id, true);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RegenerationTask task = new RegenerationTask(
            TaskType.REGENERATE, id, minBlock, null, minBlock.getLocation(), future, 1
        );

        taskQueue.offer(task);
        return future;
    }

    public boolean isOperating(String id) {
        return activeOperations.containsKey(id);
    }

    public void shutdown() {
        isShuttingDown.set(true);

        if (taskProcessor != null) taskProcessor.cancel();
        if (memoryMonitor != null) memoryMonitor.cancel();

        taskQueue.clear();
        activeOperations.clear();

        ioManager.clearCache();
        blockPlacer.shutdown();

        DebugUtils.logInternalDebug("RegenerationScheduler shutdown complete");
        DebugUtils.logInternalDebug("Metrics: " + metrics.getSummary());
    }

    public RegenerationStats getStats() {
        return new RegenerationStats(
            (int) ioManager.getCacheSize(),
            activeOperations.size(),
            activeTasks.get(),
            taskQueue.size(),
            metrics.getSuccessCount(),
            metrics.getFailureCount(),
            metrics.getAverageDuration()
        );
    }

    private enum TaskType {
        SAVE, PASTE, REGENERATE
    }

    private static class RegenerationTask {
        final TaskType type;
        final String id;
        final org.bukkit.block.Block minBlock;
        final org.bukkit.block.Block maxBlock;
        final org.bukkit.Location pasteLocation;
        final CompletableFuture<Boolean> future;
        final int priority;

        RegenerationTask(TaskType type, String id, org.bukkit.block.Block minBlock,
                        org.bukkit.block.Block maxBlock, org.bukkit.Location pasteLocation,
                        CompletableFuture<Boolean> future, int priority) {
            this.type = type;
            this.id = id;
            this.minBlock = minBlock;
            this.maxBlock = maxBlock;
            this.pasteLocation = pasteLocation;
            this.future = future;
            this.priority = priority;
        }
    }

    public static class RegenerationStats {
        public final int cachedSchematics;
        public final int activeOperations;
        public final int activeTasks;
        public final int pendingTasks;
        public final long successCount;
        public final long failureCount;
        public final long averageDuration;

        public RegenerationStats(int cachedSchematics, int activeOperations, int activeTasks,
                                int pendingTasks, long successCount, long failureCount, long averageDuration) {
            this.cachedSchematics = cachedSchematics;
            this.activeOperations = activeOperations;
            this.activeTasks = activeTasks;
            this.pendingTasks = pendingTasks;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.averageDuration = averageDuration;
        }
    }

    private static class RegenerationMetrics {
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final ConcurrentLinkedQueue<Long> recentDurations = new ConcurrentLinkedQueue<>();
        private static final int MAX_DURATION_SAMPLES = 100;

        public void recordSuccess(long duration) {
            successCount.incrementAndGet();
            recentDurations.offer(duration);
            if (recentDurations.size() > MAX_DURATION_SAMPLES) {
                recentDurations.poll();
            }
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public long getAverageDuration() {
            if (recentDurations.isEmpty()) return 0;
            return (long) recentDurations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        }

        public String getSummary() {
            return String.format("Success: %d, Failures: %d, Avg Duration: %dms",
                successCount.get(), failureCount.get(), getAverageDuration());
        }
    }
}
