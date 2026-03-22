package net.exylia.commons.v2.tasks.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class FoliaServerScheduler implements ServerScheduler {
    private final GlobalRegionScheduler globalScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaServerScheduler() {
        this.globalScheduler = Bukkit.getGlobalRegionScheduler();
        this.regionScheduler = Bukkit.getRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }

    @Override
    public ScheduledTask runSync(Plugin plugin, Runnable task) {
        return wrap(globalScheduler.run(plugin, t -> task.run()), plugin);
    }

    @Override
    public ScheduledTask runSyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
        long ticks = Math.max(1, unit.toMillis(delay) / 50);
        return wrap(globalScheduler.runDelayed(plugin, t -> task.run(), ticks), plugin);
    }

    @Override
    public ScheduledTask runSyncTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        long delayTicks = Math.max(1, unit.toMillis(delay) / 50);
        long periodTicks = Math.max(1, unit.toMillis(period) / 50);
        return wrap(globalScheduler.runAtFixedRate(plugin, t -> task.run(), delayTicks, periodTicks), plugin);
    }

    @Override
    public ScheduledTask runAsync(Plugin plugin, Runnable task) {
        return wrap(asyncScheduler.runNow(plugin, t -> task.run()), plugin);
    }

    @Override
    public ScheduledTask runAsyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
        return wrap(asyncScheduler.runDelayed(plugin, t -> task.run(), delay, unit), plugin);
    }

    @Override
    public ScheduledTask runAsyncTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return wrap(asyncScheduler.runAtFixedRate(plugin, t -> task.run(), delay, period, unit), plugin);
    }

    @Override
    public ScheduledTask runAt(Plugin plugin, Location location, Runnable task) {
        return wrap(regionScheduler.run(plugin, location, t -> task.run()), plugin);
    }

    @Override
    public ScheduledTask runAtLater(Plugin plugin, Location location, Runnable task, long delay, TimeUnit unit) {
        long ticks = Math.max(1, unit.toMillis(delay) / 50);
        return wrap(regionScheduler.runDelayed(plugin, location, t -> task.run(), ticks), plugin);
    }

    @Override
    public ScheduledTask runAtTimer(Plugin plugin, Location location, Runnable task, long delay, long period, TimeUnit unit) {
        long delayTicks = Math.max(1, unit.toMillis(delay) / 50);
        long periodTicks = Math.max(1, unit.toMillis(period) / 50);
        return wrap(regionScheduler.runAtFixedRate(plugin, location, t -> task.run(), delayTicks, periodTicks), plugin);
    }

    @Override
    public ScheduledTask runAt(Plugin plugin, Entity entity, Runnable task) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                entity.getScheduler().run(plugin, t -> task.run(), null);
        return scheduled != null ? wrap(scheduled, plugin) : NoopScheduledTask.INSTANCE;
    }

    @Override
    public ScheduledTask runAtLater(Plugin plugin, Entity entity, Runnable task, long delay, TimeUnit unit) {
        long ticks = Math.max(1, unit.toMillis(delay) / 50);
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                entity.getScheduler().runDelayed(plugin, t -> task.run(), null, ticks);
        return scheduled != null ? wrap(scheduled, plugin) : NoopScheduledTask.INSTANCE;
    }

    @Override
    public ScheduledTask runAtTimer(Plugin plugin, Entity entity, Runnable task, long delay, long period, TimeUnit unit) {
        return runAtTimer(plugin, entity, task, null, delay, period, unit);
    }

    @Override
    public ScheduledTask runAtTimer(Plugin plugin, Entity entity, Runnable task, Runnable onStop, long delay, long period, TimeUnit unit) {
        long delayTicks = Math.max(1, unit.toMillis(delay) / 50);
        long periodTicks = Math.max(1, unit.toMillis(period) / 50);
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), onStop, delayTicks, periodTicks);
        return scheduled != null ? wrap(scheduled, plugin) : NoopScheduledTask.INSTANCE;
    }

    @Override
    public void cancelAll(Plugin plugin) {
        globalScheduler.cancelTasks(plugin);
        asyncScheduler.cancelTasks(plugin);
    }

    @Override
    public boolean isMainThread() {
        return false;
    }

    @Override
    public boolean isRegionThread(Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

    @Override
    public boolean isRegionThread(World world, int chunkX, int chunkZ) {
        return Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    @Override
    public boolean isRegionThread(Chunk chunk) {
        return Bukkit.isOwnedByCurrentRegion(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @Override
    public boolean isEntityThread(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    private ScheduledTask wrap(io.papermc.paper.threadedregions.scheduler.ScheduledTask task, Plugin plugin) {
        return new FoliaScheduledTask(task, plugin);
    }

    private static final class NoopScheduledTask implements ScheduledTask {
        static final NoopScheduledTask INSTANCE = new NoopScheduledTask();

        @Override public void cancel() {}
        @Override public boolean isCancelled() { return true; }
        @Override public Plugin getOwningPlugin() { return null; }
        @Override public boolean isRunning() { return false; }
        @Override public boolean isRepeating() { return false; }
    }

    private static class FoliaScheduledTask implements ScheduledTask {
        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;
        private final Plugin plugin;

        FoliaScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task, Plugin plugin) {
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
        public boolean isRunning() {
            return task.getExecutionState() == io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.RUNNING;
        }

        @Override
        public boolean isRepeating() {
            return task.isRepeatingTask();
        }
    }
}
