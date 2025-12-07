package net.exylia.commons.async;

import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class AsyncExecutor {

    private static AsyncExecutor instance;

    @Getter
    private final ExecutorService databaseExecutor;

    @Getter
    private final ExecutorService generalExecutor;

    @Getter
    private final ScheduledExecutorService scheduledExecutor;

    private final AtomicInteger activeDbTasks = new AtomicInteger(0);
    private final AtomicInteger activeGeneralTasks = new AtomicInteger(0);
    private final AtomicInteger activeScheduledTasks = new AtomicInteger(0);

    private static final int DB_POOL_SIZE = 4;
    private static final int GENERAL_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final int SCHEDULED_POOL_SIZE = 2;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int KEEP_ALIVE_TIME = 60;

    private volatile boolean isShuttingDown = false;

    private AsyncExecutor() {
        BlockingQueue<Runnable> dbQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.databaseExecutor = new ThreadPoolExecutor(
            DB_POOL_SIZE,
            DB_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            dbQueue,
            new NamedThreadFactory("ExyliaCommons-DB"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        BlockingQueue<Runnable> generalQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.generalExecutor = new ThreadPoolExecutor(
            GENERAL_POOL_SIZE,
            GENERAL_POOL_SIZE * 2,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            generalQueue,
            new NamedThreadFactory("ExyliaCommons-Async"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.scheduledExecutor = Executors.newScheduledThreadPool(
            SCHEDULED_POOL_SIZE,
            new NamedThreadFactory("ExyliaCommons-Scheduled")
        );

        startMonitoring();
    }

    public static synchronized AsyncExecutor getInstance() {
        if (instance == null) {
            instance = new AsyncExecutor();
        }
        return instance;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, boolean isDatabase) {
        if (isShuttingDown) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncExecutor is shutting down"));
        }

        ExecutorService executor = isDatabase ? databaseExecutor : generalExecutor;
        AtomicInteger counter = isDatabase ? activeDbTasks : activeGeneralTasks;

        return CompletableFuture.supplyAsync(() -> {
            counter.incrementAndGet();
            try {
                return supplier.get();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error in async task: " + e.getMessage());
                throw e;
            } finally {
                counter.decrementAndGet();
            }
        }, executor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable, boolean isDatabase) {
        if (isShuttingDown) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncExecutor is shutting down"));
        }

        ExecutorService executor = isDatabase ? databaseExecutor : generalExecutor;
        AtomicInteger counter = isDatabase ? activeDbTasks : activeGeneralTasks;

        return CompletableFuture.runAsync(() -> {
            counter.incrementAndGet();
            try {
                runnable.run();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error in async task: " + e.getMessage());
                throw e;
            } finally {
                counter.decrementAndGet();
            }
        }, executor);
    }

    public <T> CompletableFuture<T> supplyAsyncWithTimeout(Supplier<T> supplier, boolean isDatabase, long timeout, TimeUnit unit) {
        return supplyAsync(supplier, isDatabase)
            .orTimeout(timeout, unit)
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    DebugUtils.logInternalWarn("Async task timed out after " + timeout + " " + unit);
                }
                return null;
            });
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (isShuttingDown) {
            throw new IllegalStateException("AsyncExecutor is shutting down");
        }

        return scheduledExecutor.schedule(() -> {
            activeScheduledTasks.incrementAndGet();
            try {
                task.run();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error in scheduled task: " + e.getMessage());
            } finally {
                activeScheduledTasks.decrementAndGet();
            }
        }, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (isShuttingDown) {
            throw new IllegalStateException("AsyncExecutor is shutting down");
        }

        return scheduledExecutor.scheduleAtFixedRate(() -> {
            activeScheduledTasks.incrementAndGet();
            try {
                task.run();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error in scheduled task: " + e.getMessage());
            } finally {
                activeScheduledTasks.decrementAndGet();
            }
        }, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (isShuttingDown) {
            throw new IllegalStateException("AsyncExecutor is shutting down");
        }

        return scheduledExecutor.scheduleWithFixedDelay(() -> {
            activeScheduledTasks.incrementAndGet();
            try {
                task.run();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error in scheduled task: " + e.getMessage());
            } finally {
                activeScheduledTasks.decrementAndGet();
            }
        }, initialDelay, delay, unit);
    }

    private void startMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                int dbActive = activeDbTasks.get();
                int generalActive = activeGeneralTasks.get();
                int scheduledActive = activeScheduledTasks.get();

                ThreadPoolExecutor dbPool = (ThreadPoolExecutor) databaseExecutor;
                ThreadPoolExecutor generalPool = (ThreadPoolExecutor) generalExecutor;

                int dbQueueSize = dbPool.getQueue().size();
                int generalQueueSize = generalPool.getQueue().size();

                if (dbActive > DB_POOL_SIZE * 0.8 || generalActive > GENERAL_POOL_SIZE * 0.8) {
                    DebugUtils.logInternalWarn(String.format(
                        "High async load - DB: %d/%d (queue: %d), General: %d/%d (queue: %d), Scheduled: %d",
                        dbActive, DB_POOL_SIZE, dbQueueSize,
                        generalActive, GENERAL_POOL_SIZE, generalQueueSize,
                        scheduledActive
                    ));
                }

                if (dbQueueSize > MAX_QUEUE_SIZE * 0.8 || generalQueueSize > MAX_QUEUE_SIZE * 0.8) {
                    DebugUtils.logInternalError("Critical: Async task queues near capacity!");
                }
            } catch (Exception e) {
                DebugUtils.logInternalError("Error in monitoring task: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public ExecutorStats getStats() {
        ThreadPoolExecutor dbPool = (ThreadPoolExecutor) databaseExecutor;
        ThreadPoolExecutor generalPool = (ThreadPoolExecutor) generalExecutor;

        return new ExecutorStats(
            dbPool.getActiveCount(),
            dbPool.getQueue().size(),
            dbPool.getCompletedTaskCount(),
            generalPool.getActiveCount(),
            generalPool.getQueue().size(),
            generalPool.getCompletedTaskCount(),
            activeScheduledTasks.get()
        );
    }

    public void shutdown() {
        if (isShuttingDown) {
            return;
        }

        isShuttingDown = true;
        DebugUtils.logInternalInfo("Shutting down AsyncExecutor...");

        scheduledExecutor.shutdown();
        databaseExecutor.shutdown();
        generalExecutor.shutdown();

        try {
            if (!databaseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                DebugUtils.logInternalWarn("Database executor did not terminate in time, forcing shutdown");
                databaseExecutor.shutdownNow();
            }
            if (!generalExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                DebugUtils.logInternalWarn("General executor did not terminate in time, forcing shutdown");
                generalExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                DebugUtils.logInternalWarn("Scheduled executor did not terminate in time, forcing shutdown");
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            DebugUtils.logInternalError("Interrupted during shutdown, forcing immediate shutdown");
            databaseExecutor.shutdownNow();
            generalExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        DebugUtils.logInternalInfo("AsyncExecutor shutdown complete");
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setUncaughtExceptionHandler((t, e) ->
                DebugUtils.logInternalError("Uncaught exception in thread " + t.getName() + ": " + e.getMessage())
            );
            return thread;
        }
    }

    public static class ExecutorStats {
        @Getter
        private final int dbActive;
        @Getter
        private final int dbQueued;
        @Getter
        private final long dbCompleted;
        @Getter
        private final int generalActive;
        @Getter
        private final int generalQueued;
        @Getter
        private final long generalCompleted;
        @Getter
        private final int scheduledActive;

        public ExecutorStats(int dbActive, int dbQueued, long dbCompleted,
                           int generalActive, int generalQueued, long generalCompleted,
                           int scheduledActive) {
            this.dbActive = dbActive;
            this.dbQueued = dbQueued;
            this.dbCompleted = dbCompleted;
            this.generalActive = generalActive;
            this.generalQueued = generalQueued;
            this.generalCompleted = generalCompleted;
            this.scheduledActive = scheduledActive;
        }

        @Override
        public String toString() {
            return String.format(
                "AsyncExecutor Stats:\n" +
                "  DB Pool: %d active, %d queued, %d completed\n" +
                "  General Pool: %d active, %d queued, %d completed\n" +
                "  Scheduled: %d active",
                dbActive, dbQueued, dbCompleted,
                generalActive, generalQueued, generalCompleted,
                scheduledActive
            );
        }
    }
}
