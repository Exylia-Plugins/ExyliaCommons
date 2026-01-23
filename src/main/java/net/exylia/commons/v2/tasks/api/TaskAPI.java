package net.exylia.commons.v2.tasks.api;

import net.exylia.commons.v2.tasks.builder.TaskBuilder;
import net.exylia.commons.v2.tasks.builder.TaskChainBuilder;
import net.exylia.commons.v2.tasks.config.TaskConfig;
import net.exylia.commons.v2.tasks.core.TaskExecutor;
import net.exylia.commons.v2.tasks.core.TaskManager;
import net.exylia.commons.v2.tasks.model.TaskCategory;
import net.exylia.commons.v2.tasks.model.TaskResult;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.tasks.stats.TaskStats;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TaskAPI {
    private TaskAPI() {}

    public static void initialize(Plugin plugin) {
        TaskManager.initialize(plugin);
    }

    public static void initialize(Plugin plugin, TaskConfig config) {
        TaskManager.initialize(plugin, config);
    }

    public static void shutdown() {
        if (TaskManager.isInitialized()) {
            TaskManager.getInstance().shutdown();
        }
    }

    public static boolean isInitialized() {
        return TaskManager.isInitialized();
    }

    public static <T> TaskBuilder<T> task() {
        return TaskManager.getInstance().task();
    }

    public static TaskChainBuilder<Void> chain() {
        return TaskManager.getInstance().chain();
    }

    public static <T> CompletableFuture<TaskResult<T>> async(Supplier<T> supplier) {
        return TaskManager.getInstance().async(supplier);
    }

    public static <T> CompletableFuture<TaskResult<T>> async(TaskCategory category, Supplier<T> supplier) {
        return TaskManager.getInstance().async(category, supplier);
    }

    public static CompletableFuture<TaskResult<Void>> async(Runnable runnable) {
        return TaskManager.getInstance().async(runnable);
    }

    public static CompletableFuture<TaskResult<Void>> async(TaskCategory category, Runnable runnable) {
        return TaskManager.getInstance().async(category, runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> database(Supplier<T> supplier) {
        return async(TaskCategory.DATABASE, supplier);
    }

    public static CompletableFuture<TaskResult<Void>> database(Runnable runnable) {
        return async(TaskCategory.DATABASE, runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> io(Supplier<T> supplier) {
        return async(TaskCategory.IO, supplier);
    }

    public static CompletableFuture<TaskResult<Void>> io(Runnable runnable) {
        return async(TaskCategory.IO, runnable);
    }

    public static <T> CompletableFuture<TaskResult<T>> compute(Supplier<T> supplier) {
        return async(TaskCategory.COMPUTE, supplier);
    }

    public static CompletableFuture<TaskResult<Void>> compute(Runnable runnable) {
        return async(TaskCategory.COMPUTE, runnable);
    }

    public static ScheduledTask sync(Runnable task) {
        return TaskManager.getInstance().sync(task);
    }

    public static ScheduledTask syncLater(Runnable task, long delay, TimeUnit unit) {
        return TaskManager.getInstance().syncLater(task, delay, unit);
    }

    public static ScheduledTask syncLater(Runnable task, long ticks) {
        return TaskManager.getInstance().syncLater(task, ticks);
    }

    public static ScheduledTask syncTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return TaskManager.getInstance().syncTimer(task, delay, period, unit);
    }

    public static ScheduledTask syncTimer(Runnable task, long delayTicks, long periodTicks) {
        return TaskManager.getInstance().syncTimer(task, delayTicks, periodTicks);
    }

    public static ScheduledTask asyncScheduled(Runnable task) {
        return TaskManager.getInstance().asyncScheduled(task);
    }

    public static ScheduledTask asyncScheduledLater(Runnable task, long delay, TimeUnit unit) {
        return TaskManager.getInstance().asyncScheduledLater(task, delay, unit);
    }

    public static ScheduledTask asyncScheduledTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return TaskManager.getInstance().asyncScheduledTimer(task, delay, period, unit);
    }

    public static ScheduledTask at(Location location, Runnable task) {
        return TaskManager.getInstance().at(location, task);
    }

    public static ScheduledTask atLater(Location location, Runnable task, long delay, TimeUnit unit) {
        return TaskManager.getInstance().atLater(location, task, delay, unit);
    }

    public static ScheduledTask atTimer(Location location, Runnable task, long delay, long period, TimeUnit unit) {
        return TaskManager.getInstance().atTimer(location, task, delay, period, unit);
    }

    public static ScheduledTask at(Entity entity, Runnable task) {
        return TaskManager.getInstance().at(entity, task);
    }

    public static ScheduledTask atLater(Entity entity, Runnable task, long delay, TimeUnit unit) {
        return TaskManager.getInstance().atLater(entity, task, delay, unit);
    }

    public static void runSync(Runnable task) {
        TaskManager.getInstance().runSync(task);
    }

    public static void runAsync(Runnable task) {
        TaskManager.getInstance().runAsync(task);
    }

    public static <T> void asyncThenSync(Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
        TaskManager.getInstance().asyncThenSync(asyncSupplier, syncConsumer);
    }

    public static <T> void asyncThenSync(TaskCategory category, Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
        TaskManager.getInstance().asyncThenSync(category, asyncSupplier, syncConsumer);
    }

    public static <T> void databaseThenSync(Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
        asyncThenSync(TaskCategory.DATABASE, asyncSupplier, syncConsumer);
    }

    public static boolean isMainThread() {
        return TaskManager.getInstance().isMainThread();
    }

    public static boolean isFolia() {
        return TaskManager.getInstance().isFolia();
    }

    public static boolean isRegionThread(Location location) {
        return TaskManager.getInstance().isRegionThread(location);
    }

    public static boolean isRegionThread(World world, int chunkX, int chunkZ) {
        return TaskManager.getInstance().isRegionThread(world, chunkX, chunkZ);
    }

    public static boolean isRegionThread(Chunk chunk) {
        return TaskManager.getInstance().isRegionThread(chunk);
    }

    public static boolean isEntityThread(Entity entity) {
        return TaskManager.getInstance().isEntityThread(entity);
    }

    public static TaskStats getStats() {
        return TaskManager.getInstance().getStats();
    }

    public static TaskExecutor.PoolStatus getPoolStatus(TaskCategory category) {
        return TaskManager.getInstance().getPoolStatus(category);
    }

    public static void resetStats() {
        TaskManager.getInstance().resetStats();
    }

    public static void cancelAll() {
        TaskManager.getInstance().cancelAll();
    }
}
