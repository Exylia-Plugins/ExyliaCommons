package net.exylia.commons.async;

import lombok.Getter;
import net.exylia.commons.async.impl.BukkitSchedulerAdapter;
import net.exylia.commons.async.impl.FoliaSchedulerAdapter;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public class SchedulerManager {
    private static SchedulerManager instance;
    private final SchedulerAdapter adapter;
    private final AsyncExecutor asyncExecutor;
    private final Plugin plugin;

    private SchedulerManager(Plugin plugin) {
        this.plugin = plugin;
        this.adapter = detectSchedulerType();
        this.asyncExecutor = AsyncExecutor.getInstance();

        DebugUtils.logInfo("SchedulerManager initialized with " +
            (adapter.isFolia() ? "Folia" : "Bukkit") + " adapter");
    }

    public static synchronized void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new SchedulerManager(plugin);
        }
    }

    public static SchedulerManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SchedulerManager not initialized! Call initialize(plugin) first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private SchedulerAdapter detectSchedulerType() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return new FoliaSchedulerAdapter();
        } catch (ClassNotFoundException e) {
            return new BukkitSchedulerAdapter();
        }
    }

    public TaskBuilder task(Runnable runnable) {
        return new TaskBuilder(this, runnable);
    }

    public ScheduledTask runTask(Runnable task) {
        return adapter.runTask(plugin, task);
    }

    public ScheduledTask runTaskLater(Runnable task, long delay, TimeUnit timeUnit) {
        return adapter.runTaskLater(plugin, task, delay, timeUnit);
    }

    public ScheduledTask runTaskTimer(Runnable task, long delay, long period, TimeUnit timeUnit) {
        return adapter.runTaskTimer(plugin, task, delay, period, timeUnit);
    }

    public ScheduledTask runTaskAsync(Runnable task) {
        return adapter.runTaskAsynchronously(plugin, task);
    }

    public ScheduledTask runTaskLaterAsync(Runnable task, long delay, TimeUnit timeUnit) {
        return adapter.runTaskLaterAsynchronously(plugin, task, delay, timeUnit);
    }

    public ScheduledTask runTaskTimerAsync(Runnable task, long delay, long period, TimeUnit timeUnit) {
        return adapter.runTaskTimerAsynchronously(plugin, task, delay, period, timeUnit);
    }

    public ScheduledTask runAtLocation(Location location, Runnable task) {
        return adapter.runAtLocation(plugin, location, task);
    }

    public ScheduledTask runAtLocationLater(Location location, Runnable task, long delay, TimeUnit timeUnit) {
        return adapter.runAtLocationLater(plugin, location, task, delay, timeUnit);
    }

    public ScheduledTask runAtEntity(Entity entity, Runnable task) {
        return adapter.runAtEntity(plugin, entity, task);
    }

    public ScheduledTask runAtEntityLater(Entity entity, Runnable task, long delay, TimeUnit timeUnit) {
        return adapter.runAtEntityLater(plugin, entity, task, delay, timeUnit);
    }

    public void runSync(Runnable task) {
        if (isMainThread()) {
            task.run();
        } else {
            runTask(task);
        }
    }

    public void runAsync(Runnable task) {
        if (isMainThread()) {
            runTaskAsync(task);
        } else {
            task.run();
        }
    }

    public <T> void runSyncThenAsync(Consumer<T> syncTask, T data, Consumer<T> asyncTask) {
        runSync(() -> {
            syncTask.accept(data);
            runTaskAsync(() -> asyncTask.accept(data));
        });
    }

    public <T> void runAsyncThenSync(Consumer<T> asyncTask, T data, Consumer<T> syncTask) {
        runTaskAsync(() -> {
            asyncTask.accept(data);
            runSync(() -> syncTask.accept(data));
        });
    }

    public boolean isMainThread() {
        return adapter.isGlobalThread();
    }

    public boolean isFolia() {
        return adapter.isFolia();
    }

    public boolean isOwnedByCurrentRegion(Location location) {
        return adapter.isOwnedByCurrentRegion(location);
    }

    public boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return adapter.isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    public boolean isOwnedByCurrentRegion(Chunk chunk) {
        return adapter.isOwnedByCurrentRegion(chunk);
    }

    public boolean isOwnedByCurrentRegion(Entity entity) {
        return adapter.isOwnedByCurrentRegion(entity);
    }

    public void cancelAllTasks() {
        adapter.cancelAllTasks(plugin);
    }

    public void shutdown() {
        try {
            cancelAllTasks();
            asyncExecutor.shutdown();
            DebugUtils.logInfo("SchedulerManager shutdown complete");
        } catch (Exception e) {
            DebugUtils.logError("Error during SchedulerManager shutdown: " + e.getMessage());
        }
    }

    public static class TaskBuilder {
        private final SchedulerManager manager;
        private final Runnable runnable;
        private boolean async = false;
        private Long delay = null;
        private Long period = null;
        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        private Location location = null;
        private Entity entity = null;

        private TaskBuilder(SchedulerManager manager, Runnable runnable) {
            this.manager = manager;
            this.runnable = runnable;
        }

        public TaskBuilder async() {
            this.async = true;
            return this;
        }

        public TaskBuilder delay(long delay) {
            this.delay = delay;
            return this;
        }

        public TaskBuilder delay(long delay, TimeUnit timeUnit) {
            this.delay = delay;
            this.timeUnit = timeUnit;
            return this;
        }

        public TaskBuilder period(long period) {
            this.period = period;
            return this;
        }

        public TaskBuilder period(long period, TimeUnit timeUnit) {
            this.period = period;
            this.timeUnit = timeUnit;
            return this;
        }

        public TaskBuilder delayTicks(long ticks) {
            this.delay = ticks * 50;
            this.timeUnit = TimeUnit.MILLISECONDS;
            return this;
        }

        public TaskBuilder periodTicks(long ticks) {
            this.period = ticks * 50;
            this.timeUnit = TimeUnit.MILLISECONDS;
            return this;
        }

        public TaskBuilder at(Location location) {
            this.location = location;
            return this;
        }

        public TaskBuilder at(Entity entity) {
            this.entity = entity;
            return this;
        }

        public ScheduledTask schedule() {
            if (entity != null) {
                if (delay != null) {
                    return manager.runAtEntityLater(entity, runnable, delay, timeUnit);
                }
                return manager.runAtEntity(entity, runnable);
            }

            if (location != null) {
                if (delay != null) {
                    return manager.runAtLocationLater(location, runnable, delay, timeUnit);
                }
                return manager.runAtLocation(location, runnable);
            }

            if (async) {
                if (period != null) {
                    long delayValue = delay != null ? delay : 0;
                    return manager.runTaskTimerAsync(runnable, delayValue, period, timeUnit);
                }
                if (delay != null) {
                    return manager.runTaskLaterAsync(runnable, delay, timeUnit);
                }
                return manager.runTaskAsync(runnable);
            } else {
                if (period != null) {
                    long delayValue = delay != null ? delay : 0;
                    return manager.runTaskTimer(runnable, delayValue, period, timeUnit);
                }
                if (delay != null) {
                    return manager.runTaskLater(runnable, delay, timeUnit);
                }
                return manager.runTask(runnable);
            }
        }

        public void run() {
            schedule();
        }
    }
}
