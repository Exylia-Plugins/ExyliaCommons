package net.exylia.commons.async;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class AsyncAPI {

    private AsyncAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> CompletableFuture<T> compute(Supplier<T> supplier) {
        return AsyncExecutor.getInstance().supplyAsync(supplier, false);
    }

    public static <T> CompletableFuture<T> computeDb(Supplier<T> supplier) {
        return AsyncExecutor.getInstance().supplyAsync(supplier, true);
    }

    public static CompletableFuture<Void> execute(Runnable task) {
        return AsyncExecutor.getInstance().runAsync(task, false);
    }

    public static CompletableFuture<Void> executeDb(Runnable task) {
        return AsyncExecutor.getInstance().runAsync(task, true);
    }

    public static <T> CompletableFuture<T> computeWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        return AsyncExecutor.getInstance().supplyAsyncWithTimeout(supplier, false, timeout, unit);
    }

    public static <T> CompletableFuture<T> computeDbWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        return AsyncExecutor.getInstance().supplyAsyncWithTimeout(supplier, true, timeout, unit);
    }

    public static <T> CompletableFuture<T> computeThenSync(Supplier<T> asyncSupplier, java.util.function.Consumer<T> syncConsumer) {
        return compute(asyncSupplier).thenApply(result -> {
            Schedulers.sync(() -> syncConsumer.accept(result));
            return result;
        });
    }

    public static <T> CompletableFuture<T> computeDbThenSync(Supplier<T> asyncSupplier, java.util.function.Consumer<T> syncConsumer) {
        return computeDb(asyncSupplier).thenApply(result -> {
            Schedulers.sync(() -> syncConsumer.accept(result));
            return result;
        });
    }

    public static CompletableFuture<Void> executeThenSync(Runnable asyncTask, Runnable syncTask) {
        return execute(asyncTask).thenRun(() -> Schedulers.sync(syncTask));
    }

    public static CompletableFuture<Void> executeDbThenSync(Runnable asyncTask, Runnable syncTask) {
        return executeDb(asyncTask).thenRun(() -> Schedulers.sync(syncTask));
    }

    public static <T> CompletableFuture<T> syncThenCompute(Runnable syncTask, Supplier<T> asyncSupplier) {
        Schedulers.sync(syncTask);
        return compute(asyncSupplier);
    }

    public static <T> CompletableFuture<T> syncThenComputeDb(Runnable syncTask, Supplier<T> asyncSupplier) {
        Schedulers.sync(syncTask);
        return computeDb(asyncSupplier);
    }

    public static TaskChain chain() {
        return new TaskChain();
    }

    public static class TaskChain {
        private CompletableFuture<Void> currentFuture = CompletableFuture.completedFuture(null);

        public TaskChain thenSync(Runnable task) {
            currentFuture = currentFuture.thenRun(() -> Schedulers.sync(task));
            return this;
        }

        public TaskChain thenAsync(Runnable task) {
            currentFuture = currentFuture.thenCompose(v -> AsyncAPI.execute(task));
            return this;
        }

        public TaskChain thenAsyncDb(Runnable task) {
            currentFuture = currentFuture.thenCompose(v -> AsyncAPI.executeDb(task));
            return this;
        }

        public TaskChain thenAt(Location location, Runnable task) {
            currentFuture = currentFuture.thenRun(() -> Schedulers.at(location, task));
            return this;
        }

        public TaskChain thenAt(Entity entity, Runnable task) {
            currentFuture = currentFuture.thenRun(() -> Schedulers.at(entity, task));
            return this;
        }

        public TaskChain delay(long delay, TimeUnit unit) {
            currentFuture = currentFuture.thenCompose(v ->
                CompletableFuture.runAsync(() -> {
                    try {
                        unit.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
            );
            return this;
        }

        public TaskChain delayTicks(long ticks) {
            return delay(ticks * 50, TimeUnit.MILLISECONDS);
        }

        public CompletableFuture<Void> execute() {
            return currentFuture;
        }

        public void executeAndForget() {
            currentFuture.exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }
    }
}
