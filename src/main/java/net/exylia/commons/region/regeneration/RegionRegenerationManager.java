package net.exylia.commons.region.regeneration;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class RegionRegenerationManager {
    private static RegionRegenerationManager instance;

    private final JavaPlugin plugin;
    private final File schemsFolder;

    private final LinkedHashMap<String, Clipboard> clipboardCache;
    private final ConcurrentMap<String, Boolean> regeneratingRegions;
    private final PriorityBlockingQueue<RegenerationTask> taskQueue;
    private final ExecutorService asyncExecutor;

    private BukkitRunnable taskProcessor;
    private BukkitRunnable memoryMonitor;
    private final AtomicInteger activeTasks;
    private final AtomicBoolean isShuttingDown;

    private static final int MAX_CONCURRENT_OPERATIONS = 4;
    private static final int TASK_PROCESS_INTERVAL_TICKS = 1;
    private static final int MEMORY_CHECK_INTERVAL_TICKS = 200;
    private static final long LOW_MEMORY_THRESHOLD_MB = 512;
    private static final int MAX_CACHE_SIZE = 50;

    private final RegenerationMetrics metrics;

    private RegionRegenerationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schemsFolder = new File(plugin.getDataFolder(), "schematics");
        this.clipboardCache = new LinkedHashMap<String, Clipboard>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Clipboard> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        this.regeneratingRegions = new ConcurrentHashMap<>();
        this.taskQueue = new PriorityBlockingQueue<>(100, Comparator.comparingInt(t -> t.priority));
        this.activeTasks = new AtomicInteger(0);
        this.isShuttingDown = new AtomicBoolean(false);
        this.metrics = new RegenerationMetrics();

        this.asyncExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "ExyliaRegionRegen-Async");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        );

        if (!schemsFolder.exists()) {
            schemsFolder.mkdirs();
            logInternalDebug("Schematics directory created: " + schemsFolder.getPath());
        }

        startTaskProcessor();
        startMemoryMonitor();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionRegenerationManager(plugin);
        }
    }

    public static RegionRegenerationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionRegenerationManager not initialized");
        }
        return instance;
    }

    private void startTaskProcessor() {
        taskProcessor = new BukkitRunnable() {
            @Override
            public void run() {
                if (isShuttingDown.get()) {
                    return;
                }
                processNextTask();
            }
        };
        taskProcessor.runTaskTimer(plugin, TASK_PROCESS_INTERVAL_TICKS, TASK_PROCESS_INTERVAL_TICKS);
    }

    private void startMemoryMonitor() {
        memoryMonitor = new BukkitRunnable() {
            @Override
            public void run() {
                checkMemoryAndCleanup();
            }
        };
        memoryMonitor.runTaskTimerAsynchronously(plugin, MEMORY_CHECK_INTERVAL_TICKS, MEMORY_CHECK_INTERVAL_TICKS);
    }

    private void checkMemoryAndCleanup() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemoryMB = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 1024 / 1024;

        if (freeMemoryMB < LOW_MEMORY_THRESHOLD_MB) {
            logInternalDebug("Low memory detected (" + freeMemoryMB + "MB free), performing cleanup");
            performAggressiveCleanup();
        }
    }

    private void performAggressiveCleanup() {
        synchronized (clipboardCache) {
            int cleared = clipboardCache.size();
            clipboardCache.clear();
            if (cleared > 0) {
                logInternalDebug("Aggressively cleared " + cleared + " clipboards from cache");
                System.gc();
            }
        }
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

        asyncExecutor.submit(() -> {
            try {
                executeTask(task);
            } catch (Exception e) {
                DebugUtils.logInternalError("Error executing regeneration task: " + e.getMessage());
                task.future.completeExceptionally(e);
            } finally {
                activeTasks.decrementAndGet();
            }
        });
    }

    private void executeTask(RegenerationTask task) {
        try {
            switch (task.type) {
                case REGENERATE:
                    boolean success = executeRegeneration(task);
                    task.future.complete(success);
                    break;
                case PASTE:
                    boolean pasteSuccess = executePaste(task);
                    task.future.complete(pasteSuccess);
                    break;
                case SAVE:
                    boolean saveSuccess = executeSave(task);
                    task.future.complete(saveSuccess);
                    break;
            }
        } catch (Exception e) {
            task.future.completeExceptionally(e);
        }
    }

    private boolean executeRegeneration(RegenerationTask task) {
        String regionId = task.region.getId();
        long startTime = System.currentTimeMillis();

        try {
            logInternalDebug("Starting regeneration for region: " + regionId);

            cleanRegionEntities(task.region).join();
            PlayerBlockTracker.getInstance().clearRegionBlocks(regionId);

            Clipboard clipboard = loadOrGetClipboard(task.region);
            if (clipboard == null) {
                DebugUtils.logInternalError("Failed to load clipboard for region: " + regionId);
                metrics.recordFailure();
                return false;
            }

            Location targetLocation = task.region.getMinimumPoint();
            boolean success = executePaste(clipboard, targetLocation);

            long duration = System.currentTimeMillis() - startTime;
            if (success) {
                logInternalDebug("Region regenerated successfully: " + regionId + " in " + duration + "ms");
                metrics.recordSuccess(duration);
            } else {
                DebugUtils.logInternalError("Regeneration failed for region: " + regionId);
                metrics.recordFailure();
            }

            return success;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error during regeneration of " + regionId + ": " + e.getMessage());
            metrics.recordFailure();
            return false;
        } finally {
            regeneratingRegions.remove(regionId);
        }
    }

    private boolean executePaste(RegenerationTask task) {
        long startTime = System.currentTimeMillis();
        try {
            Clipboard clipboard = loadOrGetClipboard(task.region);
            if (clipboard == null) {
                metrics.recordFailure();
                return false;
            }

            boolean success = executePaste(clipboard, task.targetLocation);
            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                metrics.recordSuccess(duration);
            } else {
                metrics.recordFailure();
            }

            return success;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error during paste operation: " + e.getMessage());
            metrics.recordFailure();
            return false;
        }
    }

    private boolean executeSave(RegenerationTask task) {
        try {
            logInternalDebug("Saving schematic for region: " + task.region.getId());

            Location min = task.region.getMinimumPoint();
            Location max = task.region.getMaximumPoint();

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(min.getWorld());
            BlockVector3 minVec = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
            BlockVector3 maxVec = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());

            CuboidRegion worldEditRegion = new CuboidRegion(weWorld, minVec, maxVec);

            Clipboard clipboard = saveRegionToClipboard(weWorld, worldEditRegion, minVec);
            if (clipboard == null) {
                return false;
            }

            clipboard.setOrigin(minVec);

            File schematicFile = getSchematicFile(task.region);
            try (FileOutputStream fos = new FileOutputStream(schematicFile);
                 ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(fos)) {

                writer.write(clipboard);
                cacheClipboard(task.region.getId(), clipboard);

                logInternalDebug("Schematic saved: " + schematicFile.getName());
                return true;
            }

        } catch (Exception e) {
            DebugUtils.logInternalError("Error saving schematic: " + e.getMessage());
            return false;
        }
    }

    private Clipboard saveRegionToClipboard(com.sk89q.worldedit.world.World weWorld, CuboidRegion region, BlockVector3 minVec) {
        EditSession editSession = null;
        try {
            editSession = WorldEdit.getInstance().newEditSession(weWorld);
            editSession.setFastMode(true);

            Clipboard clipboard = new BlockArrayClipboard(region);
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, minVec);
            copy.setCopyingEntities(false);
            Operations.complete(copy);

            return clipboard;
        } catch (Exception e) {
            DebugUtils.logInternalError("Error saving region to clipboard: " + e.getMessage());
            return null;
        } finally {
            if (editSession != null) {
                editSession.close();
            }
        }
    }

    private boolean executePaste(Clipboard clipboard, Location targetLocation) {
        World world = targetLocation.getWorld();
        if (world == null) {
            return false;
        }

        BlockVector3 clipboardOrigin = clipboard.getOrigin();
        BlockVector3 clipboardMin = clipboard.getMinimumPoint();
        BlockVector3 offset = clipboardMin.subtract(clipboardOrigin);

        BlockVector3 targetPosition = BlockVector3.at(
            targetLocation.getBlockX(),
            targetLocation.getBlockY(),
            targetLocation.getBlockZ()
        );
        BlockVector3 pastePosition = targetPosition.add(offset);

        EditSession editSession = null;
        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

            editSession = WorldEdit.getInstance().newEditSession(weWorld);
            editSession.setFastMode(true);
            editSession.setReorderMode(EditSession.ReorderMode.FAST);

            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation pasteOperation = holder
                .createPaste(editSession)
                .to(pastePosition)
                .copyEntities(false)
                .ignoreAirBlocks(false)
                .build();

            Operations.complete(pasteOperation);
            editSession.flushQueue();

            return true;

        } catch (WorldEditException e) {
            DebugUtils.logInternalError("WorldEdit error during paste: " + e.getMessage());
            return false;
        } catch (Exception e) {
            DebugUtils.logInternalError("Unexpected error during paste: " + e.getMessage());
            return false;
        } finally {
            if (editSession != null) {
                try {
                    editSession.close();
                } catch (Exception e) {
                    DebugUtils.logInternalError("Error closing EditSession: " + e.getMessage());
                }
            }
        }
    }

    private Clipboard loadOrGetClipboard(Region region) {
        String regionId = region.getId();

        synchronized (clipboardCache) {
            Clipboard cached = clipboardCache.get(regionId);
            if (cached != null) {
                logInternalDebug("Using cached clipboard for region: " + regionId);
                metrics.recordCacheHit();
                return cached;
            }
        }

        metrics.recordCacheMiss();

        File schematicFile = getSchematicFile(region);
        if (!schematicFile.exists()) {
            DebugUtils.logInternalError("Schematic file not found: " + schematicFile.getName());
            return null;
        }

        Clipboard clipboard = loadSchematicFromFile(schematicFile);
        if (clipboard != null) {
            cacheClipboard(regionId, clipboard);
        }

        return clipboard;
    }

    private Clipboard loadSchematicFromFile(File schematicFile) {
        try (FileInputStream fis = new FileInputStream(schematicFile);
             ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(fis)) {

            Clipboard clipboard = reader.read();
            logInternalDebug("Loaded schematic from file: " + schematicFile.getName());
            return clipboard;

        } catch (IOException e) {
            DebugUtils.logInternalError("Error loading schematic " + schematicFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void cacheClipboard(String regionId, Clipboard clipboard) {
        synchronized (clipboardCache) {
            clipboardCache.put(regionId, clipboard);
            logInternalDebug("Clipboard cached for region: " + regionId + " (cache size: " + clipboardCache.size() + ")");
        }
    }

    public CompletableFuture<Void> preloadClipboards(List<Region> regions) {
        return CompletableFuture.runAsync(() -> {
            logInternalDebug("Preloading " + regions.size() + " clipboards...");
            int loaded = 0;
            for (Region region : regions) {
                if (loadOrGetClipboard(region) != null) {
                    loaded++;
                }
            }
            logInternalDebug("Preloaded " + loaded + "/" + regions.size() + " clipboards");
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        String regionId = region.getId();

        if (regeneratingRegions.containsKey(regionId)) {
            logInternalDebug("Region already regenerating: " + regionId);
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(region);
        if (!schematicFile.exists()) {
            DebugUtils.logInternalError("No schematic found for region: " + regionId);
            return CompletableFuture.completedFuture(false);
        }

        regeneratingRegions.put(regionId, true);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RegenerationTask task = new RegenerationTask(
            TaskType.REGENERATE,
            region,
            null,
            future,
            1
        );

        taskQueue.offer(task);
        logInternalDebug("Regeneration task queued for region: " + regionId);

        return future;
    }

    public CompletableFuture<List<Boolean>> regenerateMultipleRegions(List<Region> regions) {
        logInternalDebug("Batch regenerating " + regions.size() + " regions");

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (Region region : regions) {
            futures.add(regenerateRegion(region));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    public CompletableFuture<Boolean> pasteRegionSchematicAt(Region sourceRegion, Location targetLocation) {
        if (!sourceRegion.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(sourceRegion);
        if (!schematicFile.exists()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RegenerationTask task = new RegenerationTask(
            TaskType.PASTE,
            sourceRegion,
            targetLocation,
            future,
            2
        );

        taskQueue.offer(task);
        logInternalDebug("Paste task queued for region: " + sourceRegion.getId());

        return future;
    }

    public CompletableFuture<Boolean> saveRegionSchematic(Region region) {
        if (!region.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RegenerationTask task = new RegenerationTask(
            TaskType.SAVE,
            region,
            null,
            future,
            3
        );

        taskQueue.offer(task);
        logInternalDebug("Save task queued for region: " + region.getId());

        return future;
    }

    public CompletableFuture<Integer> cleanRegionEntities(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    int removed = 0;
                    Location min = region.getMinimumPoint();
                    Location max = region.getMaximumPoint();

                    double minX = Math.min(min.getX(), max.getX());
                    double maxX = Math.max(min.getX(), max.getX());
                    double minY = Math.min(min.getY(), max.getY());
                    double maxY = Math.max(min.getY(), max.getY());
                    double minZ = Math.min(min.getZ(), max.getZ());
                    double maxZ = Math.max(min.getZ(), max.getZ());

                    for (Entity entity : min.getWorld().getEntities()) {
                        if (entity instanceof Player) continue;

                        Location loc = entity.getLocation();
                        if (loc.getX() >= minX && loc.getX() <= maxX &&
                            loc.getY() >= minY && loc.getY() <= maxY &&
                            loc.getZ() >= minZ && loc.getZ() <= maxZ) {

                            if (shouldRemoveEntity(entity)) {
                                entity.remove();
                                removed++;
                            }
                        }
                    }

                    logInternalDebug("Cleaned " + removed + " entities in region " + region.getId());
                    return removed;

                }).get();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error cleaning entities: " + e.getMessage());
                return 0;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> copyRegionStructure(Region sourceRegion, Location targetCenter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location sourceMin = sourceRegion.getMinimumPoint();
                Location sourceMax = sourceRegion.getMaximumPoint();

                Selection sourceSelection = sourceRegion.getSelection();
                Location sourceSelectionCenter = sourceSelection.getCenter();

                int offsetX = targetCenter.getBlockX() - sourceSelectionCenter.getBlockX();
                int offsetY = targetCenter.getBlockY() - sourceSelectionCenter.getBlockY();
                int offsetZ = targetCenter.getBlockZ() - sourceSelectionCenter.getBlockZ();

                Location targetMin = sourceMin.clone().add(offsetX, offsetY, offsetZ);
                targetMin.setWorld(targetCenter.getWorld());

                return copyAreaDirectly(sourceMin, sourceMax, targetMin);

            } catch (Exception e) {
                DebugUtils.logInternalError("Error copying region structure: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    private boolean copyAreaDirectly(Location sourceMin, Location sourceMax, Location targetMin) {
        try {
            com.sk89q.worldedit.world.World sourceWorld = BukkitAdapter.adapt(sourceMin.getWorld());
            com.sk89q.worldedit.world.World targetWorld = BukkitAdapter.adapt(targetMin.getWorld());

            BlockVector3 sourceMinVec = BlockVector3.at(sourceMin.getBlockX(), sourceMin.getBlockY(), sourceMin.getBlockZ());
            BlockVector3 sourceMaxVec = BlockVector3.at(sourceMax.getBlockX(), sourceMax.getBlockY(), sourceMax.getBlockZ());
            BlockVector3 targetMinVec = BlockVector3.at(targetMin.getBlockX(), targetMin.getBlockY(), targetMin.getBlockZ());

            CuboidRegion sourceRegion = new CuboidRegion(sourceWorld, sourceMinVec, sourceMaxVec);

            Clipboard clipboard = saveRegionToClipboard(sourceWorld, sourceRegion, sourceMinVec);
            if (clipboard == null) {
                return false;
            }

            return executePaste(clipboard, targetMin);

        } catch (Exception e) {
            DebugUtils.logInternalError("Error in direct copy: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> deleteRegionSchematic(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            File schematicFile = getSchematicFile(region);
            String regionId = region.getId();

            synchronized (clipboardCache) {
                clipboardCache.remove(regionId);
            }

            if (schematicFile.exists()) {
                boolean deleted = schematicFile.delete();
                if (deleted) {
                    logInternalDebug("Schematic deleted for region: " + regionId);
                }
                return deleted;
            }

            return true;
        }, asyncExecutor);
    }

    public CompletableFuture<Integer> clearRegionPlayerBlocks(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            String regionId = region.getId();
            PlayerBlockTracker tracker = PlayerBlockTracker.getInstance();

            int blocksBefore = tracker.getPlayerBlocks(regionId).size();
            tracker.clearRegionBlocks(regionId);

            logInternalDebug("Cleared " + blocksBefore + " player blocks in region " + regionId);
            return blocksBefore;
        }, asyncExecutor);
    }

    public boolean hasSchematic(Region region) {
        return getSchematicFile(region).exists();
    }

    public boolean isRegenerating(Region region) {
        return regeneratingRegions.containsKey(region.getId());
    }

    public RegenerationStats getStats() {
        int cachedCount;
        synchronized (clipboardCache) {
            cachedCount = clipboardCache.size();
        }

        return new RegenerationStats(
            cachedCount,
            regeneratingRegions.size(),
            schemsFolder.listFiles() != null ? schemsFolder.listFiles().length : 0,
            activeTasks.get(),
            taskQueue.size(),
            metrics.getSuccessCount(),
            metrics.getFailureCount(),
            metrics.getAverageDuration(),
            metrics.getCacheHitRate()
        );
    }

    public void clearCache() {
        synchronized (clipboardCache) {
            int size = clipboardCache.size();
            clipboardCache.clear();
            logInternalDebug("Clipboard cache cleared: " + size + " entries");
        }
        System.gc();
    }

    private boolean shouldRemoveEntity(Entity entity) {
        return entity.getType() != EntityType.PLAYER;
    }

    private File getSchematicFile(Region region) {
        return new File(schemsFolder, region.getId() + ".schem");
    }

    public void shutdown() {
        isShuttingDown.set(true);

        if (taskProcessor != null) {
            taskProcessor.cancel();
        }
        if (memoryMonitor != null) {
            memoryMonitor.cancel();
        }

        if (!regeneratingRegions.isEmpty()) {
            DebugUtils.logInternalInfo("Waiting for " + regeneratingRegions.size() + " active regenerations...");
            int attempts = 0;
            while (!regeneratingRegions.isEmpty() && attempts < 20) {
                try {
                    Thread.sleep(500);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        asyncExecutor.shutdown();

        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clearCache();
        regeneratingRegions.clear();
        taskQueue.clear();

        logInternalDebug("RegionRegenerationManager shutdown complete");
        logInternalDebug("Final metrics: " + metrics.getSummary());
    }

    private enum TaskType {
        REGENERATE, PASTE, SAVE
    }

    private static class RegenerationTask {
        final TaskType type;
        final Region region;
        final Location targetLocation;
        final CompletableFuture<Boolean> future;
        final int priority;

        RegenerationTask(TaskType type, Region region, Location targetLocation,
                        CompletableFuture<Boolean> future, int priority) {
            this.type = type;
            this.region = region;
            this.targetLocation = targetLocation;
            this.future = future;
            this.priority = priority;
        }
    }

    @Getter
    public static class RegenerationStats {
        private final int cachedClipboards;
        private final int activeRegenerations;
        private final int totalSchematics;
        private final int activeTasks;
        private final int pendingTasks;
        private final long successCount;
        private final long failureCount;
        private final long averageDuration;
        private final double cacheHitRate;

        public RegenerationStats(int cachedClipboards, int activeRegenerations, int totalSchematics,
                                int activeTasks, int pendingTasks, long successCount, long failureCount,
                                long averageDuration, double cacheHitRate) {
            this.cachedClipboards = cachedClipboards;
            this.activeRegenerations = activeRegenerations;
            this.totalSchematics = totalSchematics;
            this.activeTasks = activeTasks;
            this.pendingTasks = pendingTasks;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.averageDuration = averageDuration;
            this.cacheHitRate = cacheHitRate;
        }
    }

    private static class RegenerationMetrics {
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger cacheHits = new AtomicInteger(0);
        private final AtomicInteger cacheMisses = new AtomicInteger(0);
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

        public void recordCacheHit() {
            cacheHits.incrementAndGet();
        }

        public void recordCacheMiss() {
            cacheMisses.incrementAndGet();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public long getAverageDuration() {
            if (recentDurations.isEmpty()) {
                return 0;
            }
            return (long) recentDurations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        }

        public double getCacheHitRate() {
            int totalRequests = cacheHits.get() + cacheMisses.get();
            if (totalRequests == 0) {
                return 0.0;
            }
            return (double) cacheHits.get() / totalRequests * 100.0;
        }

        public String getSummary() {
            return String.format("Success: %d, Failures: %d, Avg Duration: %dms, Cache Hit Rate: %.1f%%",
                successCount.get(), failureCount.get(), getAverageDuration(), getCacheHitRate());
        }
    }
}
