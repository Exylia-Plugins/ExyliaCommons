package net.exylia.commons.async;

import org.bukkit.plugin.Plugin;

/**
 * @deprecated Use {@link net.exylia.commons.v2.tasks.scheduler.ScheduledTask} instead.
 */
@Deprecated(forRemoval = true)
public interface ScheduledTask {

    void cancel();

    boolean isCancelled();

    Plugin getOwningPlugin();

    boolean isCurrentlyRunning();

    boolean isRepeatingTask();
}
