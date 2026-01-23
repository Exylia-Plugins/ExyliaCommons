package net.exylia.commons.v2.tasks.scheduler;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public interface ServerScheduler {
    ScheduledTask runSync(Plugin plugin, Runnable task);
    ScheduledTask runSyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit);
    ScheduledTask runSyncTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit);

    ScheduledTask runAsync(Plugin plugin, Runnable task);
    ScheduledTask runAsyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit);
    ScheduledTask runAsyncTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit unit);

    ScheduledTask runAt(Plugin plugin, Location location, Runnable task);
    ScheduledTask runAtLater(Plugin plugin, Location location, Runnable task, long delay, TimeUnit unit);
    ScheduledTask runAtTimer(Plugin plugin, Location location, Runnable task, long delay, long period, TimeUnit unit);

    ScheduledTask runAt(Plugin plugin, Entity entity, Runnable task);
    ScheduledTask runAtLater(Plugin plugin, Entity entity, Runnable task, long delay, TimeUnit unit);

    void cancelAll(Plugin plugin);

    boolean isMainThread();
    boolean isRegionThread(Location location);
    boolean isRegionThread(World world, int chunkX, int chunkZ);
    boolean isRegionThread(Chunk chunk);
    boolean isEntityThread(Entity entity);
    boolean isFolia();
}
