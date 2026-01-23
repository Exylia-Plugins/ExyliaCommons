package net.exylia.commons.async;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * @deprecated Use {@link net.exylia.commons.v2.tasks.scheduler.ServerScheduler} instead.
 */
@Deprecated(forRemoval = true)
public interface SchedulerAdapter {

    ScheduledTask runTask(Plugin plugin, Runnable task);

    ScheduledTask runTaskLater(Plugin plugin, Runnable task, long delay, TimeUnit timeUnit);

    ScheduledTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period, TimeUnit timeUnit);

    ScheduledTask runTaskAsynchronously(Plugin plugin, Runnable task);

    ScheduledTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay, TimeUnit timeUnit);

    ScheduledTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period, TimeUnit timeUnit);

    ScheduledTask runAtLocation(Plugin plugin, Location location, Runnable task);

    ScheduledTask runAtLocationLater(Plugin plugin, Location location, Runnable task, long delay, TimeUnit timeUnit);

    ScheduledTask runAtEntity(Plugin plugin, Entity entity, Runnable task);

    ScheduledTask runAtEntityLater(Plugin plugin, Entity entity, Runnable task, long delay, TimeUnit timeUnit);

    void cancelAllTasks(Plugin plugin);

    boolean isGlobalThread();

    boolean isOwnedByCurrentRegion(Location location);

    boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ);

    boolean isOwnedByCurrentRegion(Chunk chunk);

    boolean isOwnedByCurrentRegion(Entity entity);

    boolean isFolia();
}
