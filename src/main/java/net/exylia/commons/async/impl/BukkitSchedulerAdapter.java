package net.exylia.commons.async.impl;

import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * @deprecated Use {@link net.exylia.commons.v2.tasks.scheduler.BukkitServerScheduler} instead.
 */
@Deprecated(forRemoval = true)
public class BukkitSchedulerAdapter implements SchedulerAdapter {

    @Override
    public ScheduledTask runTask(Plugin plugin, Runnable task) {
        return new BukkitScheduledTask(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public ScheduledTask runTaskLater(Plugin plugin, Runnable task, long delay, TimeUnit timeUnit) {
        long ticks = timeUnit.toMillis(delay) / 50;
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskLater(plugin, task, ticks));
    }

    @Override
    public ScheduledTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit timeUnit) {
        long delayTicks = Math.max(1, timeUnit.toMillis(delay) / 50);
        long periodTicks = Math.max(1, timeUnit.toMillis(period) / 50);
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    @Override
    public ScheduledTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public ScheduledTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay, TimeUnit timeUnit) {
        long ticks = timeUnit.toMillis(delay) / 50;
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks));
    }

    @Override
    public ScheduledTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period, TimeUnit timeUnit) {
        long delayTicks = timeUnit.toMillis(delay) / 50;
        long periodTicks = timeUnit.toMillis(period) / 50;
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks));
    }

    @Override
    public ScheduledTask runAtLocation(Plugin plugin, Location location, Runnable task) {
        return runTask(plugin, task);
    }

    @Override
    public ScheduledTask runAtLocationLater(Plugin plugin, Location location, Runnable task, long delay, TimeUnit timeUnit) {
        return runTaskLater(plugin, task, delay, timeUnit);
    }

    @Override
    public ScheduledTask runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        return runTask(plugin, task);
    }

    @Override
    public ScheduledTask runAtEntityLater(Plugin plugin, Entity entity, Runnable task, long delay, TimeUnit timeUnit) {
        return runTaskLater(plugin, task, delay, timeUnit);
    }

    @Override
    public void cancelAllTasks(Plugin plugin) {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public boolean isGlobalThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentRegion(Location location) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentRegion(Chunk chunk) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentRegion(Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    private static class BukkitScheduledTask implements ScheduledTask {
        private final BukkitTask task;

        public BukkitScheduledTask(BukkitTask task) {
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
        public boolean isCurrentlyRunning() {
            return Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId());
        }

        @Override
        public boolean isRepeatingTask() {
            return Bukkit.getScheduler().isQueued(task.getTaskId());
        }
    }
}
