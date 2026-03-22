package net.exylia.commons.v2.tasks.core;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.tasks.config.TaskConfig;
import net.exylia.commons.v2.tasks.model.TaskCategory;
import net.exylia.commons.v2.tasks.model.TaskPriority;
import net.exylia.commons.v2.tasks.model.TaskResult;
import net.exylia.commons.v2.tasks.stats.TaskMetrics;
import net.exylia.commons.v2.tasks.stats.TaskStats;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TaskExecutor {
    private final Map<TaskCategory, ThreadPoolExecutor> executors = new EnumMap<>(TaskCategory.class);
    private final ScheduledExecutorService scheduler;
    private final TaskConfig config;
    @Getter
    private final TaskMetrics metrics;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private ScheduledFuture<?> monitoringTask;

    public TaskExecutor(TaskConfig config) {
        this.config = config;
        this.metrics = new TaskMetrics();

        for (TaskCategory category : TaskCategory.values()) {
            TaskConfig.PoolConfig poolConfig = config.getPoolConfig(category);
            executors.put(category, createExecutor(category, poolConfig));
        }

        this.scheduler = Executors.newScheduledThreadPool(2, new NamedThreadFactory("Exylia-Scheduler"));

        if (config.isEnableMonitoring()) {
            startMonitoring();
        }
    }

    private ThreadPoolExecutor createExecutor(TaskCategory category, TaskConfig.PoolConfig poolConfig) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(poolConfig.getQueueSize());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            poolConfig.getCorePoolSize(),
            poolConfig.getMaxPoolSize(),
            config.getKeepAliveTime(),
            config.getKeepAliveUnit(),
            queue,
            new NamedThreadFactory("Exylia-" + category.getDisplayName()),
            (r, e) -> {
                throw new RejectedExecutionException("Task rejected for category " + category + ": queue full");
            }
        );

        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public <T> CompletableFuture<TaskResult<T>> submit(Supplier<T> supplier, TaskCategory category, TaskPriority priority) {
        if (shuttingDown.get()) {
            return CompletableFuture.completedFuture(
                TaskResult.cancelled(UUID.randomUUID().toString(), Instant.now(), category)
            );
        }

        String taskId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        metrics.recordSubmit(category);

        ThreadPoolExecutor executor = executors.get(category);

        try {
            return CompletableFuture.supplyAsync(() -> {
                Thread.currentThread().setPriority(priority.getThreadPriority());
                metrics.recordStart(category);
                long startNanos = System.nanoTime();

                try {
                    T result = supplier.get();
                    long durationNanos = System.nanoTime() - startNanos;
                    metrics.recordComplete(category, durationNanos);
                    return TaskResult.success(taskId, result, startTime, category);
                } catch (Exception e) {
                    metrics.recordFailure(category);
                    DebugAPI.logLibError(DebugCategory.ASYNC, "Task failed [" + category + "]: " + e.getMessage(), e);
                    return TaskResult.failure(taskId, e, startTime, category);
                }
            }, executor);
        } catch (RejectedExecutionException ex) {
            metrics.recordRejected(category);
            DebugAPI.logLibWarn(DebugCategory.ASYNC, "Task rejected for category " + category + ": queue full");
            return CompletableFuture.completedFuture(TaskResult.failure(taskId, ex, startTime, category));
        }
    }

    public CompletableFuture<TaskResult<Void>> submit(Runnable runnable, TaskCategory category, TaskPriority priority) {
        return submit(() -> {
            runnable.run();
            return null;
        }, category, priority);
    }

    public <T> CompletableFuture<TaskResult<T>> submitWithTimeout(
            Supplier<T> supplier, TaskCategory category, TaskPriority priority, long timeout, TimeUnit unit) {

        return submit(supplier, category, priority)
            .orTimeout(timeout, unit)
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    metrics.recordTimeout(category);
                    return TaskResult.timeout(UUID.randomUUID().toString(), Instant.now(), category);
                }
                metrics.recordFailure(category);
                return TaskResult.failure(UUID.randomUUID().toString(), ex, Instant.now(), category);
            });
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(task, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(wrapScheduled(task), initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return scheduler.scheduleWithFixedDelay(wrapScheduled(task), initialDelay, delay, unit);
    }

    private Runnable wrapScheduled(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.ASYNC, "Scheduled task error: " + e.getMessage());
            }
        };
    }

    private void startMonitoring() {
        monitoringTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                checkPoolHealth();
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.ASYNC, "Monitoring error: " + e.getMessage());
            }
        }, config.getMonitoringInterval(), config.getMonitoringInterval(), config.getMonitoringIntervalUnit());
    }

    private void checkPoolHealth() {
        for (Map.Entry<TaskCategory, ThreadPoolExecutor> entry : executors.entrySet()) {
            TaskCategory category = entry.getKey();
            ThreadPoolExecutor pool = entry.getValue();
            TaskConfig.PoolConfig poolConfig = config.getPoolConfig(category);

            double load = (double) pool.getActiveCount() / poolConfig.getMaxPoolSize();
            double queueLoad = (double) pool.getQueue().size() / poolConfig.getQueueSize();

            if (load > config.getHighLoadThreshold() || queueLoad > config.getHighLoadThreshold()) {
                DebugAPI.logLibWarn(DebugCategory.ASYNC, String.format(
                    "[%s] High load - Active: %d/%d (%.0f%%), Queue: %d/%d (%.0f%%)",
                    category.getDisplayName(),
                    pool.getActiveCount(), poolConfig.getMaxPoolSize(), load * 100,
                    pool.getQueue().size(), poolConfig.getQueueSize(), queueLoad * 100
                ));
            }
        }
    }

    public PoolStatus getPoolStatus(TaskCategory category) {
        ThreadPoolExecutor pool = executors.get(category);
        TaskConfig.PoolConfig poolConfig = config.getPoolConfig(category);
        return new PoolStatus(
            pool.getActiveCount(),
            pool.getPoolSize(),
            poolConfig.getMaxPoolSize(),
            pool.getQueue().size(),
            poolConfig.getQueueSize(),
            pool.getCompletedTaskCount()
        );
    }

    public TaskStats getStats() {
        return metrics.snapshot();
    }

    public void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }

        DebugAPI.logLibInfo(DebugCategory.ASYNC, "Shutting down TaskExecutor...");

        if (config.isLogStatsOnShutdown()) {
            DebugAPI.logLibInfo(DebugCategory.ASYNC, "Final stats:\n" + getStats().toString());
        }

        if (monitoringTask != null) {
            monitoringTask.cancel(false);
        }

        scheduler.shutdown();
        executors.values().forEach(ExecutorService::shutdown);

        long timeout = config.getShutdownTimeout();
        try {
            for (Map.Entry<TaskCategory, ThreadPoolExecutor> entry : executors.entrySet()) {
                if (!entry.getValue().awaitTermination(timeout, TimeUnit.SECONDS)) {
                    DebugAPI.logLibWarn(DebugCategory.ASYNC, entry.getKey() + " pool did not terminate, forcing...");
                    entry.getValue().shutdownNow();
                }
            }

            if (!scheduler.awaitTermination(timeout / 2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executors.values().forEach(ExecutorService::shutdownNow);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        DebugAPI.logLibInfo(DebugCategory.ASYNC, "TaskExecutor shutdown complete");
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    public record PoolStatus(
        int activeCount,
        int poolSize,
        int maxPoolSize,
        int queueSize,
        int maxQueueSize,
        long completedTasks
    ) {
        public double activeRatio() {
            return maxPoolSize > 0 ? (double) activeCount / maxPoolSize : 0;
        }

        public double queueRatio() {
            return maxQueueSize > 0 ? (double) queueSize / maxQueueSize : 0;
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) ->
                DebugAPI.logLibError(DebugCategory.ASYNC, "Uncaught exception in " + thread.getName() + ": " + ex.getMessage())
            );
            return t;
        }
    }
}
