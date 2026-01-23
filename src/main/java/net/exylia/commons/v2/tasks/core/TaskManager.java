package net.exylia.commons.v2.tasks.core;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.tasks.builder.TaskBuilder;
import net.exylia.commons.v2.tasks.builder.TaskChainBuilder;
import net.exylia.commons.v2.tasks.config.TaskConfig;
import net.exylia.commons.v2.tasks.model.TaskCategory;
import net.exylia.commons.v2.tasks.model.TaskPriority;
import net.exylia.commons.v2.tasks.model.TaskResult;
import net.exylia.commons.v2.tasks.scheduler.BukkitServerScheduler;
import net.exylia.commons.v2.tasks.scheduler.FoliaServerScheduler;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.tasks.scheduler.ServerScheduler;
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

public class TaskManager {
    private static TaskManager instance;

    @Getter
    private final Plugin plugin;
    @Getter
    private final TaskExecutor executor;
    @Getter
    private final ServerScheduler scheduler;
    @Getter
    private final TaskConfig config;
    @Getter
    private final boolean folia;

    private TaskManager(Plugin plugin, TaskConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.folia = detectFolia();
        this.scheduler = folia ? new FoliaServerScheduler() : new BukkitServerScheduler();
        this.executor = new TaskExecutor(config);

        DebugAPI.logLibInfo(DebugCategory.ASYNC, "TaskManager initialized [" + (folia ? "Folia" : "Bukkit") + " mode]");
    }

    public static synchronized void initialize(Plugin plugin) {
        initialize(plugin, TaskConfig.defaults());
    }

    public static synchronized void initialize(Plugin plugin, TaskConfig config) {
        if (instance == null) {
            instance = new TaskManager(plugin, config);
        }
    }

    public static TaskManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TaskManager not initialized. Call initialize(plugin) first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public <T> TaskBuilder<T> task() {
        return new TaskBuilder<>(this);
    }

    public TaskChainBuilder<Void> chain() {
        return new TaskChainBuilder<>(this);
    }

    public <T> CompletableFuture<TaskResult<T>> async(Supplier<T> supplier) {
        return executor.submit(supplier, TaskCategory.GENERAL, TaskPriority.NORMAL);
    }

    public <T> CompletableFuture<TaskResult<T>> async(TaskCategory category, Supplier<T> supplier) {
        return executor.submit(supplier, category, TaskPriority.NORMAL);
    }

    public CompletableFuture<TaskResult<Void>> async(Runnable runnable) {
        return executor.submit(runnable, TaskCategory.GENERAL, TaskPriority.NORMAL);
    }

    public CompletableFuture<TaskResult<Void>> async(TaskCategory category, Runnable runnable) {
        return executor.submit(runnable, category, TaskPriority.NORMAL);
    }

    public ScheduledTask sync(Runnable task) {
        return scheduler.runSync(plugin, task);
    }

    public ScheduledTask syncLater(Runnable task, long delay, TimeUnit unit) {
        return scheduler.runSyncLater(plugin, task, delay, unit);
    }

    public ScheduledTask syncLater(Runnable task, long ticks) {
        return syncLater(task, ticks * 50, TimeUnit.MILLISECONDS);
    }

    public ScheduledTask syncTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return scheduler.runSyncTimer(plugin, task, delay, period, unit);
    }

    public ScheduledTask syncTimer(Runnable task, long delayTicks, long periodTicks) {
        return syncTimer(task, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }

    public ScheduledTask asyncScheduled(Runnable task) {
        return scheduler.runAsync(plugin, task);
    }

    public ScheduledTask asyncScheduledLater(Runnable task, long delay, TimeUnit unit) {
        return scheduler.runAsyncLater(plugin, task, delay, unit);
    }

    public ScheduledTask asyncScheduledTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return scheduler.runAsyncTimer(plugin, task, delay, period, unit);
    }

    public ScheduledTask at(Location location, Runnable task) {
        return scheduler.runAt(plugin, location, task);
    }

    public ScheduledTask atLater(Location location, Runnable task, long delay, TimeUnit unit) {
        return scheduler.runAtLater(plugin, location, task, delay, unit);
    }

    public ScheduledTask atTimer(Location location, Runnable task, long delay, long period, TimeUnit unit) {
        return scheduler.runAtTimer(plugin, location, task, delay, period, unit);
    }

    public ScheduledTask at(Entity entity, Runnable task) {
        return scheduler.runAt(plugin, entity, task);
    }

    public ScheduledTask atLater(Entity entity, Runnable task, long delay, TimeUnit unit) {
        return scheduler.runAtLater(plugin, entity, task, delay, unit);
    }

    public void runSync(Runnable task) {
        if (isMainThread()) {
            task.run();
        } else {
            sync(task);
        }
    }

    public void runAsync(Runnable task) {
        if (isMainThread()) {
            asyncScheduled(task);
        } else {
            task.run();
        }
    }

    public <T> void asyncThenSync(Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
        async(asyncSupplier).thenAccept(result -> {
            if (result.isSuccess()) {
                runSync(() -> syncConsumer.accept(result.getValue().orElse(null)));
            }
        });
    }

    public <T> void asyncThenSync(TaskCategory category, Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
        async(category, asyncSupplier).thenAccept(result -> {
            if (result.isSuccess()) {
                runSync(() -> syncConsumer.accept(result.getValue().orElse(null)));
            }
        });
    }

    public boolean isMainThread() {
        return scheduler.isMainThread();
    }

    public boolean isRegionThread(Location location) {
        return scheduler.isRegionThread(location);
    }

    public boolean isRegionThread(World world, int chunkX, int chunkZ) {
        return scheduler.isRegionThread(world, chunkX, chunkZ);
    }

    public boolean isRegionThread(Chunk chunk) {
        return scheduler.isRegionThread(chunk);
    }

    public boolean isEntityThread(Entity entity) {
        return scheduler.isEntityThread(entity);
    }

    public TaskStats getStats() {
        return executor.getStats();
    }

    public TaskExecutor.PoolStatus getPoolStatus(TaskCategory category) {
        return executor.getPoolStatus(category);
    }

    public void resetStats() {
        executor.getMetrics().reset();
    }

    public void cancelAll() {
        scheduler.cancelAll(plugin);
    }

    public void shutdown() {
        DebugAPI.logLibInfo(DebugCategory.ASYNC, "Shutting down TaskManager...");
        cancelAll();
        executor.shutdown();
        instance = null;
        DebugAPI.logLibInfo(DebugCategory.ASYNC, "TaskManager shutdown complete");
    }
}
