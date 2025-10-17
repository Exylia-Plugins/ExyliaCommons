package net.exylia.commons.async;

import org.bukkit.plugin.Plugin;

public interface ScheduledTask {

    void cancel();

    boolean isCancelled();

    Plugin getOwningPlugin();

    boolean isCurrentlyRunning();

    boolean isRepeatingTask();
}
