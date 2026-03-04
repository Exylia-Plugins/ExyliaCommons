package net.exylia.commons.v2.tasks.api;

import net.exylia.commons.v2.tasks.builder.TaskBuilder;
import net.exylia.commons.v2.tasks.builder.TaskChainBuilder;
import net.exylia.commons.v2.tasks.core.TaskManager;
import net.exylia.commons.v2.tasks.model.TaskCategory;
import net.exylia.commons.v2.tasks.model.TaskResult;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Tasks {
    private Tasks() {}

    public static <T> TaskBuilder<T> build() {
        return TaskManager.getInstance().task();
    }

    public static TaskChainBuilder<Void> chain() {
        return TaskManager.getInstance().chain();
    }

    public static <T> CompletableFuture<TaskResult<T>> run(Supplier<T> supplier) {
        return TaskAPI.async(supplier);
    }

    public static CompletableFuture<TaskResult<Void>> run(Runnable runnable) {
        return TaskAPI.async(runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> run(TaskCategory category, Supplier<T> supplier) {
        return TaskAPI.async(category, supplier);
    }

    public static CompletableFuture<TaskResult<Void>> run(TaskCategory category, Runnable runnable) {
        return TaskAPI.async(category, runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> db(Supplier<T> supplier) {
        return TaskAPI.database(supplier);
    }

    public static CompletableFuture<TaskResult<Void>> db(Runnable runnable) {
        return TaskAPI.database(runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> io(Supplier<T> supplier) {
        return TaskAPI.io(supplier);
    }

    public static CompletableFuture<TaskResult<Void>> io(Runnable runnable) {
        return TaskAPI.io(runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> compute(Supplier<T> supplier) {
        return TaskAPI.compute(supplier);
    }

    public static CompletableFuture<TaskResult<Void>> compute(Runnable runnable) {
        return TaskAPI.compute(runnable);
    }

    public static ScheduledTask sync(Runnable task) {
        return TaskAPI.sync(task);
    }

    public static ScheduledTask later(Runnable task, long ticks) {
        return TaskAPI.syncLater(task, ticks);
    }

    public static ScheduledTask later(Runnable task, long delay, TimeUnit unit) {
        return TaskAPI.syncLater(task, delay, unit);
    }

    public static ScheduledTask timer(Runnable task, long delayTicks, long periodTicks) {
        return TaskAPI.syncTimer(task, delayTicks, periodTicks);
    }

    public static ScheduledTask timer(Runnable task, long delay, long period, TimeUnit unit) {
        return TaskAPI.syncTimer(task, delay, period, unit);
    }

    public static ScheduledTask asyncTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return TaskAPI.asyncScheduledTimer(task, delay, period, unit);
    }

    public static ScheduledTask at(Location location, Runnable task) {
        return TaskAPI.at(location, task);
    }

    public static ScheduledTask at(Entity entity, Runnable task) {
        return TaskAPI.at(entity, task);
    }

    public static ScheduledTask atLater(Location location, Runnable task, long ticks) {
        return TaskAPI.atLater(location, task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public static ScheduledTask atLater(Entity entity, Runnable task, long ticks) {
        return TaskAPI.atLater(entity, task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public static boolean isMain() {
        return TaskAPI.isMainThread();
    }

    public static boolean isFolia() {
        return TaskAPI.isFolia();
    }

    public static void runOnEntity(Entity entity, Runnable task) {
        if (TaskAPI.isEntityThread(entity)) {
            task.run();
        } else {
            TaskAPI.at(entity, task);
        }
    }

    public static void runOnLocation(Location location, Runnable task) {
        if (TaskAPI.isRegionThread(location)) {
            task.run();
        } else {
            TaskAPI.at(location, task);
        }
    }

    public static ScheduledTask asyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return TaskAPI.asyncScheduledTimer(task, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }

    public static <T> CompletableFuture<T> dbValue(Supplier<T> supplier) {
        return db(supplier).thenApply(r -> r.getValue().orElse(null));
    }

    public static CompletableFuture<Void> dbRun(Runnable runnable) {
        return db(runnable).thenApply(r -> null);
    }

    public static CompletableFuture<Void> ioRun(Runnable runnable) {
        return io(runnable).thenApply(r -> null);
    }

    public static CompletableFuture<Void> computeRun(Runnable runnable) {
        return compute(runnable).thenApply(r -> null);
    }

    public static <T> CompletableFuture<T> ioValue(Supplier<T> supplier) {
        return io(supplier).thenApply(r -> r.getValue().orElse(null));
    }

    public static <T> CompletableFuture<T> computeValue(Supplier<T> supplier) {
        return compute(supplier).thenApply(r -> r.getValue().orElse(null));
    }

    public static <T> CompletableFuture<T> runValue(Supplier<T> supplier) {
        return run(supplier).thenApply(r -> r.getValue().orElse(null));
    }

    public static <T> CompletableFuture<T> runValue(TaskCategory category, Supplier<T> supplier) {
        return run(category, supplier).thenApply(r -> r.getValue().orElse(null));
    }
}
