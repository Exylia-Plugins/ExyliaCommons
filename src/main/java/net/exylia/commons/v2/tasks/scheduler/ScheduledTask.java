package net.exylia.commons.v2.tasks.scheduler;

import org.bukkit.plugin.Plugin;

public interface ScheduledTask {
    void cancel();
    boolean isCancelled();
    Plugin getOwningPlugin();
    boolean isRunning();
    boolean isRepeating();
}
