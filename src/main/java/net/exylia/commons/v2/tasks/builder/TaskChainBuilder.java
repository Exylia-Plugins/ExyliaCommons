package net.exylia.commons.v2.tasks.builder;

import net.exylia.commons.v2.tasks.core.TaskManager;
import net.exylia.commons.v2.tasks.model.TaskCategory;
import net.exylia.commons.v2.tasks.model.TaskPriority;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TaskChainBuilder<T> {
    private final TaskManager manager;
    private CompletableFuture<T> future;
    private TaskCategory category = TaskCategory.GENERAL;
    private TaskPriority priority = TaskPriority.NORMAL;

    public TaskChainBuilder(TaskManager manager) {
        this.manager = manager;
        this.future = CompletableFuture.completedFuture(null);
    }

    public TaskChainBuilder(TaskManager manager, CompletableFuture<T> initial) {
        this.manager = manager;
        this.future = initial;
    }

    public TaskChainBuilder<T> category(TaskCategory category) {
        this.category = category;
        return this;
    }

    public TaskChainBuilder<T> priority(TaskPriority priority) {
        this.priority = priority;
        return this;
    }

    public TaskChainBuilder<Void> thenSync(Runnable task) {
        CompletableFuture<Void> next = future.thenCompose(v -> {
            CompletableFuture<Void> syncFuture = new CompletableFuture<>();
            manager.runSync(() -> {
                try {
                    task.run();
                    syncFuture.complete(null);
                } catch (Exception e) {
                    syncFuture.completeExceptionally(e);
                }
            });
            return syncFuture;
        });
        return new TaskChainBuilder<>(manager, next);
    }

    public <R> TaskChainBuilder<R> thenSync(Supplier<R> supplier) {
        CompletableFuture<R> next = future.thenCompose(v -> {
            CompletableFuture<R> syncFuture = new CompletableFuture<>();
            manager.runSync(() -> {
                try {
                    syncFuture.complete(supplier.get());
                } catch (Exception e) {
                    syncFuture.completeExceptionally(e);
                }
            });
            return syncFuture;
        });
        return new TaskChainBuilder<>(manager, next);
    }

    public TaskChainBuilder<Void> thenAsync(Runnable task) {
        CompletableFuture<Void> next = future.thenCompose(v ->
            manager.getExecutor().submit(task, category, priority)
                .thenApply(result -> null)
        );
        return new TaskChainBuilder<>(manager, next);
    }

    public <R> TaskChainBuilder<R> thenAsync(Supplier<R> supplier) {
        CompletableFuture<R> next = future.thenCompose(v ->
            manager.getExecutor().submit(supplier, category, priority)
                .thenApply(result -> result.getValue().orElse(null))
        );
        return new TaskChainBuilder<>(manager, next);
    }

    public TaskChainBuilder<Void> thenAsync(TaskCategory cat, Runnable task) {
        CompletableFuture<Void> next = future.thenCompose(v ->
            manager.getExecutor().submit(task, cat, priority)
                .thenApply(result -> null)
        );
        return new TaskChainBuilder<>(manager, next);
    }

    public <R> TaskChainBuilder<R> thenAsync(TaskCategory cat, Supplier<R> supplier) {
        CompletableFuture<R> next = future.thenCompose(v ->
            manager.getExecutor().submit(supplier, cat, priority)
                .thenApply(result -> result.getValue().orElse(null))
        );
        return new TaskChainBuilder<>(manager, next);
    }

    public TaskChainBuilder<Void> thenAt(Location location, Runnable task) {
        CompletableFuture<Void> next = future.thenCompose(v -> {
            CompletableFuture<Void> locFuture = new CompletableFuture<>();
            manager.getScheduler().runAt(manager.getPlugin(), location, () -> {
                try {
                    task.run();
                    locFuture.complete(null);
                } catch (Exception e) {
                    locFuture.completeExceptionally(e);
                }
            });
            return locFuture;
        });
        return new TaskChainBuilder<>(manager, next);
    }

    public TaskChainBuilder<Void> thenAt(Entity entity, Runnable task) {
        CompletableFuture<Void> next = future.thenCompose(v -> {
            CompletableFuture<Void> entFuture = new CompletableFuture<>();
            manager.getScheduler().runAt(manager.getPlugin(), entity, () -> {
                try {
                    task.run();
                    entFuture.complete(null);
                } catch (Exception e) {
                    entFuture.completeExceptionally(e);
                }
            });
            return entFuture;
        });
        return new TaskChainBuilder<>(manager, next);
    }

    public TaskChainBuilder<T> delay(long delay, TimeUnit unit) {
        CompletableFuture<T> next = future.thenCompose(v -> {
            CompletableFuture<T> delayed = new CompletableFuture<>();
            try {
                manager.getExecutor().schedule(() -> delayed.complete(v), delay, unit);
            } catch (RejectedExecutionException e) {
                delayed.completeExceptionally(e);
            }
            return delayed;
        });
        return new TaskChainBuilder<>(manager, next);
    }

    public TaskChainBuilder<T> delayTicks(long ticks) {
        return delay(ticks * 50, TimeUnit.MILLISECONDS);
    }

    public <R> TaskChainBuilder<R> thenApply(Function<T, R> function) {
        return new TaskChainBuilder<>(manager, future.thenApply(function));
    }

    public TaskChainBuilder<T> thenAccept(Consumer<T> consumer) {
        return new TaskChainBuilder<>(manager, future.thenApply(v -> {
            consumer.accept(v);
            return v;
        }));
    }

    public <R> TaskChainBuilder<R> thenCompose(Function<T, CompletableFuture<R>> function) {
        return new TaskChainBuilder<>(manager, future.thenCompose(function));
    }

    public TaskChainBuilder<T> onError(Consumer<Throwable> handler) {
        return new TaskChainBuilder<>(manager, future.exceptionally(ex -> {
            handler.accept(ex);
            return null;
        }));
    }

    public TaskChainBuilder<T> onErrorSync(Consumer<Throwable> handler) {
        return new TaskChainBuilder<>(manager, future.exceptionally(ex -> {
            manager.runSync(() -> handler.accept(ex));
            return null;
        }));
    }

    public CompletableFuture<T> execute() {
        return future;
    }

    public void fire() {
        future.exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
