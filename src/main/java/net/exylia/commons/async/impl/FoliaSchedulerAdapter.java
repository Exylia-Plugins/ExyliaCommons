package net.exylia.commons.async.impl;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final GlobalRegionScheduler globalScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaSchedulerAdapter() {
        this.globalScheduler = Bukkit.getGlobalRegionScheduler();
        this.regionScheduler = Bukkit.getRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }

    @Override
    public ScheduledTask runTask(Plugin plugin, Runnable task) {
        return new FoliaScheduledTask(globalScheduler.run(plugin, scheduledTask -> task.run()), plugin);
    }

    @Override
    public ScheduledTask runTaskLater(Plugin plugin, Runnable task, long delay, TimeUnit timeUnit) {
        long ticks = timeUnit.toMillis(delay) / 50;
        return new FoliaScheduledTask(globalScheduler.runDelayed(plugin, scheduledTask -> task.run(), ticks), plugin);
    }

    @Override
    public ScheduledTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit timeUnit) {
        long delayTicks = timeUnit.toMillis(delay) / 50;
        long periodTicks = timeUnit.toMillis(period) / 50;
        return new FoliaScheduledTask(
            globalScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delayTicks, periodTicks),
            plugin
        );
    }

    @Override
    public ScheduledTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        return new FoliaScheduledTask(asyncScheduler.runNow(plugin, scheduledTask -> task.run()), plugin);
    }

    @Override
    public ScheduledTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay, TimeUnit timeUnit) {
        return new FoliaScheduledTask(
            asyncScheduler.runDelayed(plugin, scheduledTask -> task.run(), delay, timeUnit),
            plugin
        );
    }

    @Override
    public ScheduledTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period, TimeUnit timeUnit) {
        return new FoliaScheduledTask(
            asyncScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period, timeUnit),
            plugin
        );
    }

    @Override
    public ScheduledTask runAtLocation(Plugin plugin, Location location, Runnable task) {
        return new FoliaScheduledTask(
            regionScheduler.run(plugin, location, scheduledTask -> task.run()),
            plugin
        );
    }

    @Override
    public ScheduledTask runAtLocationLater(Plugin plugin, Location location, Runnable task, long delay, TimeUnit timeUnit) {
        long ticks = timeUnit.toMillis(delay) / 50;
        return new FoliaScheduledTask(
            regionScheduler.runDelayed(plugin, location, scheduledTask -> task.run(), ticks),
            plugin
        );
    }

    @Override
    public ScheduledTask runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        return new FoliaScheduledTask(
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null),
            plugin
        );
    }

    @Override
    public ScheduledTask runAtEntityLater(Plugin plugin, Entity entity, Runnable task, long delay, TimeUnit timeUnit) {
        long ticks = timeUnit.toMillis(delay) / 50;
        return new FoliaScheduledTask(
            entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, ticks),
            plugin
        );
    }

    @Override
    public void cancelAllTasks(Plugin plugin) {
        globalScheduler.cancelTasks(plugin);
        asyncScheduler.cancelTasks(plugin);
    }

    @Override
    public boolean isGlobalThread() {
        return false;
    }

    @Override
    public boolean isOwnedByCurrentRegion(Location location) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(Chunk chunk) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(Entity entity) {
        return true;
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    private static class FoliaScheduledTask implements ScheduledTask {
        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;
        private final Plugin plugin;

        public FoliaScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task, Plugin plugin) {
            this.task = task;
            this.plugin = plugin;
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
            return plugin;
        }

        @Override
        public boolean isCurrentlyRunning() {
            return task.getExecutionState() == io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.RUNNING;
        }

        @Override
        public boolean isRepeatingTask() {
            return task.isRepeatingTask();
        }
    }
}
