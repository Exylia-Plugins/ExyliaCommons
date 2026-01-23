package net.exylia.commons.v2.tasks.builder;

import net.exylia.commons.v2.tasks.core.TaskManager;
import net.exylia.commons.v2.tasks.model.TaskCategory;
import net.exylia.commons.v2.tasks.model.TaskPriority;
import net.exylia.commons.v2.tasks.model.TaskResult;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaskBuilder<T> {
    private final TaskManager manager;
    private Supplier<T> supplier;
    private Runnable runnable;
    private TaskCategory category = TaskCategory.GENERAL;
    private TaskPriority priority = TaskPriority.NORMAL;
    private Long timeout;
    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private Long delay;
    private Long period;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private Location location;
    private Entity entity;
    private boolean async = true;
    private Consumer<T> onSuccess;
    private Consumer<Throwable> onFailure;
    private Runnable onComplete;

    public TaskBuilder(TaskManager manager) {
        this.manager = manager;
    }

    public TaskBuilder<T> supply(Supplier<T> supplier) {
        this.supplier = supplier;
        return this;
    }

    @SuppressWarnings("unchecked")
    public TaskBuilder<T> run(Runnable runnable) {
        this.runnable = runnable;
        return (TaskBuilder<T>) this;
    }

    public TaskBuilder<T> category(TaskCategory category) {
        this.category = category;
        return this;
    }

    public TaskBuilder<T> priority(TaskPriority priority) {
        this.priority = priority;
        return this;
    }

    public TaskBuilder<T> timeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.timeoutUnit = unit;
        return this;
    }

    public TaskBuilder<T> delay(long delay, TimeUnit unit) {
        this.delay = delay;
        this.timeUnit = unit;
        return this;
    }

    public TaskBuilder<T> delayTicks(long ticks) {
        this.delay = ticks * 50;
        this.timeUnit = TimeUnit.MILLISECONDS;
        return this;
    }

    public TaskBuilder<T> period(long period, TimeUnit unit) {
        this.period = period;
        this.timeUnit = unit;
        return this;
    }

    public TaskBuilder<T> periodTicks(long ticks) {
        this.period = ticks * 50;
        this.timeUnit = TimeUnit.MILLISECONDS;
        return this;
    }

    public TaskBuilder<T> at(Location location) {
        this.location = location;
        this.async = false;
        return this;
    }

    public TaskBuilder<T> at(Entity entity) {
        this.entity = entity;
        this.async = false;
        return this;
    }

    public TaskBuilder<T> sync() {
        this.async = false;
        return this;
    }

    public TaskBuilder<T> async() {
        this.async = true;
        return this;
    }

    public TaskBuilder<T> onSuccess(Consumer<T> handler) {
        this.onSuccess = handler;
        return this;
    }

    public TaskBuilder<T> onFailure(Consumer<Throwable> handler) {
        this.onFailure = handler;
        return this;
    }

    public TaskBuilder<T> onComplete(Runnable handler) {
        this.onComplete = handler;
        return this;
    }

    public CompletableFuture<TaskResult<T>> execute() {
        if (!async || entity != null || location != null) {
            return executeSync();
        }
        return executeAsync();
    }

    private CompletableFuture<TaskResult<T>> executeAsync() {
        Supplier<T> actualSupplier = supplier != null ? supplier : () -> {
            if (runnable != null) runnable.run();
            return null;
        };

        CompletableFuture<TaskResult<T>> future;
        if (timeout != null) {
            future = manager.getExecutor().submitWithTimeout(actualSupplier, category, priority, timeout, timeoutUnit);
        } else {
            future = manager.getExecutor().submit(actualSupplier, category, priority);
        }

        return future.whenComplete((result, ex) -> {
            if (ex == null && result != null) {
                if (result.isSuccess() && onSuccess != null) {
                    manager.runSync(() -> onSuccess.accept(result.getValue().orElse(null)));
                } else if (result.isFailure() && onFailure != null) {
                    manager.runSync(() -> onFailure.accept(result.getError().orElse(null)));
                }
            }
            if (onComplete != null) {
                manager.runSync(onComplete);
            }
        });
    }

    private CompletableFuture<TaskResult<T>> executeSync() {
        CompletableFuture<TaskResult<T>> future = new CompletableFuture<>();

        Runnable syncTask = () -> {
            try {
                T result = supplier != null ? supplier.get() : null;
                if (runnable != null) runnable.run();

                TaskResult<T> taskResult = TaskResult.success(
                    java.util.UUID.randomUUID().toString(),
                    result,
                    java.time.Instant.now(),
                    category
                );
                future.complete(taskResult);

                if (onSuccess != null) onSuccess.accept(result);
            } catch (Exception e) {
                TaskResult<T> taskResult = TaskResult.failure(
                    java.util.UUID.randomUUID().toString(),
                    e,
                    java.time.Instant.now(),
                    category
                );
                future.complete(taskResult);

                if (onFailure != null) onFailure.accept(e);
            } finally {
                if (onComplete != null) onComplete.run();
            }
        };

        if (entity != null) {
            scheduleAtEntity(syncTask);
        } else if (location != null) {
            scheduleAtLocation(syncTask);
        } else {
            scheduleSync(syncTask);
        }

        return future;
    }

    private void scheduleSync(Runnable task) {
        if (period != null) {
            manager.getScheduler().runSyncTimer(manager.getPlugin(), task,
                delay != null ? delay : 0, period, timeUnit);
        } else if (delay != null) {
            manager.getScheduler().runSyncLater(manager.getPlugin(), task, delay, timeUnit);
        } else {
            manager.getScheduler().runSync(manager.getPlugin(), task);
        }
    }

    private void scheduleAtLocation(Runnable task) {
        if (period != null) {
            manager.getScheduler().runAtTimer(manager.getPlugin(), location, task,
                delay != null ? delay : 0, period, timeUnit);
        } else if (delay != null) {
            manager.getScheduler().runAtLater(manager.getPlugin(), location, task, delay, timeUnit);
        } else {
            manager.getScheduler().runAt(manager.getPlugin(), location, task);
        }
    }

    private void scheduleAtEntity(Runnable task) {
        if (delay != null) {
            manager.getScheduler().runAtLater(manager.getPlugin(), entity, task, delay, timeUnit);
        } else {
            manager.getScheduler().runAt(manager.getPlugin(), entity, task);
        }
    }

    public ScheduledTask schedule() {
        if (period != null) {
            if (async) {
                return manager.getScheduler().runAsyncTimer(manager.getPlugin(),
                    runnable != null ? runnable : () -> {}, delay != null ? delay : 0, period, timeUnit);
            } else if (location != null) {
                return manager.getScheduler().runAtTimer(manager.getPlugin(), location,
                    runnable != null ? runnable : () -> {}, delay != null ? delay : 0, period, timeUnit);
            } else {
                return manager.getScheduler().runSyncTimer(manager.getPlugin(),
                    runnable != null ? runnable : () -> {}, delay != null ? delay : 0, period, timeUnit);
            }
        } else if (delay != null) {
            if (async) {
                return manager.getScheduler().runAsyncLater(manager.getPlugin(),
                    runnable != null ? runnable : () -> {}, delay, timeUnit);
            } else if (location != null) {
                return manager.getScheduler().runAtLater(manager.getPlugin(), location,
                    runnable != null ? runnable : () -> {}, delay, timeUnit);
            } else if (entity != null) {
                return manager.getScheduler().runAtLater(manager.getPlugin(), entity,
                    runnable != null ? runnable : () -> {}, delay, timeUnit);
            } else {
                return manager.getScheduler().runSyncLater(manager.getPlugin(),
                    runnable != null ? runnable : () -> {}, delay, timeUnit);
            }
        } else {
            if (async) {
                return manager.getScheduler().runAsync(manager.getPlugin(),
                    runnable != null ? runnable : () -> {});
            } else if (location != null) {
                return manager.getScheduler().runAt(manager.getPlugin(), location,
                    runnable != null ? runnable : () -> {});
            } else if (entity != null) {
                return manager.getScheduler().runAt(manager.getPlugin(), entity,
                    runnable != null ? runnable : () -> {});
            } else {
                return manager.getScheduler().runSync(manager.getPlugin(),
                    runnable != null ? runnable : () -> {});
            }
        }
    }

    public void fire() {
        execute();
    }
}
