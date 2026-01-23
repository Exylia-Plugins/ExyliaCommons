package net.exylia.commons.v2.tasks.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class BukkitServerScheduler implements ServerScheduler {

    @Override
    public ScheduledTask runSync(Plugin plugin, Runnable task) {
        return wrap(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public ScheduledTask runSyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
        return wrap(Bukkit.getScheduler().runTaskLater(plugin, task, toTicks(delay, unit)));
    }

    @Override
    public ScheduledTask runSyncTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return wrap(Bukkit.getScheduler().runTaskTimer(plugin, task,
            Math.max(1, toTicks(delay, unit)), Math.max(1, toTicks(period, unit))));
    }

    @Override
    public ScheduledTask runAsync(Plugin plugin, Runnable task) {
        return wrap(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public ScheduledTask runAsyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
        return wrap(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, toTicks(delay, unit)));
    }

    @Override
    public ScheduledTask runAsyncTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return wrap(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task,
            toTicks(delay, unit), toTicks(period, unit)));
    }

    @Override
    public ScheduledTask runAt(Plugin plugin, Location location, Runnable task) {
        return runSync(plugin, task);
    }

    @Override
    public ScheduledTask runAtLater(Plugin plugin, Location location, Runnable task, long delay, TimeUnit unit) {
        return runSyncLater(plugin, task, delay, unit);
    }

    @Override
    public ScheduledTask runAtTimer(Plugin plugin, Location location, Runnable task, long delay, long period, TimeUnit unit) {
        return runSyncTimer(plugin, task, delay, period, unit);
    }

    @Override
    public ScheduledTask runAt(Plugin plugin, Entity entity, Runnable task) {
        return runSync(plugin, task);
    }

    @Override
    public ScheduledTask runAtLater(Plugin plugin, Entity entity, Runnable task, long delay, TimeUnit unit) {
        return runSyncLater(plugin, task, delay, unit);
    }

    @Override
    public void cancelAll(Plugin plugin) {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isRegionThread(Location location) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isRegionThread(World world, int chunkX, int chunkZ) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isRegionThread(Chunk chunk) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isEntityThread(Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    private long toTicks(long duration, TimeUnit unit) {
        return unit.toMillis(duration) / 50;
    }

    private ScheduledTask wrap(BukkitTask task) {
        return new BukkitScheduledTask(task);
    }

    private static class BukkitScheduledTask implements ScheduledTask {
        private final BukkitTask task;

        BukkitScheduledTask(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public Plugin getOwningPlugin() {
            return task.getOwner();
        }

        @Override
        public boolean isRunning() {
            return Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId());
        }

        @Override
        public boolean isRepeating() {
            return Bukkit.getScheduler().isQueued(task.getTaskId());
        }
    }
}
