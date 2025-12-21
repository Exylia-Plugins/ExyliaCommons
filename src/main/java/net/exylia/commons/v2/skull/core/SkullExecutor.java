package net.exylia.commons.v2.skull.core;

import lombok.Getter;
import net.exylia.commons.v2.skull.config.SkullConfig;

import java.util.concurrent.*;

public class SkullExecutor {

    @Getter
    private final ExecutorService fetchExecutor;

    @Getter
    private final ScheduledExecutorService cleanupExecutor;

    private final SkullConfig config;

    public SkullExecutor(SkullConfig config) {
        this.config = config;

        this.fetchExecutor = new ThreadPoolExecutor(
                config.getThreadPoolSize(),
                config.getThreadPoolSize() * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "SkullFetch-" + counter++);
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SkullCleanup");
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, fetchExecutor);
    }

    public CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, fetchExecutor);
    }

    public void scheduleCleanup(Runnable task) {
        cleanupExecutor.scheduleAtFixedRate(
                task,
                config.getCleanupInterval(),
                config.getCleanupInterval(),
                TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        fetchExecutor.shutdown();
        cleanupExecutor.shutdown();
        try {
            if (!fetchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                fetchExecutor.shutdownNow();
            }
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fetchExecutor.shutdownNow();
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public String getStats() {
        if (fetchExecutor instanceof ThreadPoolExecutor tpe) {
            return String.format(
                    "Executor[Active: %d, Pool: %d, Queue: %d, Completed: %d]",
                    tpe.getActiveCount(),
                    tpe.getPoolSize(),
                    tpe.getQueue().size(),
                    tpe.getCompletedTaskCount()
            );
        }
        return "Executor[Unknown]";
    }
}
