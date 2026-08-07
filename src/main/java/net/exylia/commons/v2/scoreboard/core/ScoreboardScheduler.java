package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardScheduler {

    private static final String LOG_PREFIX = "[Scoreboard] [ScoreboardScheduler] ";

    private final Map<Long, Set<ScoreboardInstance>> byInterval = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public void schedule(ScoreboardInstance instance) {
        long interval = Math.max(1L, instance.getScoreboard().getUpdateInterval());
        byInterval.computeIfAbsent(interval, ignored -> ConcurrentHashMap.newKeySet()).add(instance);
        boolean newTask = !tasks.containsKey(interval);
        tasks.computeIfAbsent(interval, this::createTask);
        Bukkit.getLogger().info(LOG_PREFIX + "schedule(" + instance.getPlayer().getName() + ") -> interval=" + interval
                + " ticks, " + (newTask ? "created new timer task" : "reused existing timer task")
                + ", instancesAtInterval=" + byInterval.get(interval).size());
    }

    public void unschedule(ScoreboardInstance instance) {
        long interval = Math.max(1L, instance.getScoreboard().getUpdateInterval());
        Set<ScoreboardInstance> instances = byInterval.get(interval);
        if (instances == null) return;
        instances.remove(instance);
        Bukkit.getLogger().info(LOG_PREFIX + "unschedule(" + instance.getPlayer().getName() + ") -> interval=" + interval
                + " ticks, remainingAtInterval=" + instances.size());
        if (instances.isEmpty()) {
            byInterval.remove(interval, instances);
            ScheduledTask task = tasks.remove(interval);
            if (task != null) {
                task.cancel();
                Bukkit.getLogger().info(LOG_PREFIX + "unschedule() -> no more instances at interval=" + interval + ", timer task cancelled");
            }
        }
    }

    public void shutdown() {
        Bukkit.getLogger().info(LOG_PREFIX + "shutdown() -> cancelling " + tasks.size() + " timer task(s)");
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
        byInterval.clear();
    }

    private ScheduledTask createTask(long interval) {
        return Tasks.timer(() -> {
            Set<ScoreboardInstance> instances = byInterval.get(interval);
            if (instances == null) return;
            long now = System.nanoTime();
            instances.removeIf(instance -> {
                if (!instance.isActive()) return true;
                if (instance.shouldUpdate(now)) instance.update();
                return false;
            });
            if (instances.isEmpty()) {
                byInterval.remove(interval, instances);
                ScheduledTask task = tasks.remove(interval);
                if (task != null) task.cancel();
            }
        }, interval, interval);
    }
}
