package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardScheduler {

    private final Plugin plugin;
    private final ScoreboardRegistry registry;
    private final Map<Long, ScheduledTask> schedulersByInterval;
    private final Map<Long, Set<ScoreboardInstance>> instancesByInterval;

    public ScoreboardScheduler(Plugin plugin, ScoreboardRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.schedulersByInterval = new ConcurrentHashMap<>();
        this.instancesByInterval = new ConcurrentHashMap<>();
    }

    public void schedule(ScoreboardInstance instance) {
        if (instance == null) {
            return;
        }

        long interval = instance.getScoreboard().getUpdateInterval();

        instancesByInterval.computeIfAbsent(interval, k -> ConcurrentHashMap.newKeySet())
                .add(instance);

        if (!schedulersByInterval.containsKey(interval)) {
            createSchedulerForInterval(interval);
        }
    }

    public void unschedule(ScoreboardInstance instance) {
        if (instance == null) {
            return;
        }

        long interval = instance.getScoreboard().getUpdateInterval();

        Set<ScoreboardInstance> instances = instancesByInterval.get(interval);
        if (instances != null) {
            instances.remove(instance);

            if (instances.isEmpty()) {
                removeSchedulerIfEmpty(interval);
            }
        }
    }

    public void updateInterval(ScoreboardInstance instance, long newInterval) {
        if (instance == null) {
            return;
        }

        unschedule(instance);
        schedule(instance);
    }

    public void shutdown() {
        schedulersByInterval.values().forEach(ScheduledTask::cancel);
        schedulersByInterval.clear();
        instancesByInterval.clear();
    }

    private void createSchedulerForInterval(long interval) {
        ScheduledTask task = Tasks.timer(() -> {
            Set<ScoreboardInstance> instances = instancesByInterval.get(interval);

            if (instances == null || instances.isEmpty()) {
                return;
            }

            instances.removeIf(instance -> {
                if (instance.getLifecycle().isCancelled() || !instance.getPlayer().isOnline()) {
                    return true;
                }

                if (instance.shouldUpdate()) {
                    instance.update();
                }

                return false;
            });

        }, 0L, interval);

        schedulersByInterval.put(interval, task);
    }

    private void removeSchedulerIfEmpty(long interval) {
        Set<ScoreboardInstance> instances = instancesByInterval.get(interval);

        if (instances == null || instances.isEmpty()) {
            ScheduledTask task = schedulersByInterval.remove(interval);
            if (task != null) {
                task.cancel();
            }

            instancesByInterval.remove(interval);
        }
    }
}
