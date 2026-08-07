package net.exylia.commons.v2.scoreboard.core;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.integration.TabIntegration;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.listener.ScoreboardListener;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardStats;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class ScoreboardManager {

    private static final long REINIT_DELAY_TICKS = 20L;

    private static volatile ScoreboardManager instance;
    private static final Object LOCK = new Object();

    private final Plugin plugin;
    private final ScoreboardRegistry registry;
    private final ScoreboardFactory factory;
    private final ScoreboardScheduler scheduler;
    private final Map<UUID, ScheduledTask> pendingReinitTasks;
    private boolean initialized;

    private ScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new ScoreboardRegistry();
        this.factory = new ScoreboardFactory(plugin);
        this.scheduler = new ScoreboardScheduler(plugin, registry);
        this.pendingReinitTasks = new HashMap<>();
        this.initialized = false;
    }

    public static ScoreboardManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ScoreboardManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static void initialize(Plugin plugin) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ScoreboardManager(plugin);
                    instance.init();
                }
            }
        }
    }

    private void init() {
        plugin.getServer().getPluginManager().registerEvents(new ScoreboardListener(this), plugin);
        TabIntegration.register(this);
        initialized = true;
    }

    public CompletableFuture<String> showScoreboard(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context
    ) {
        if (player == null || scoreboard == null) {
            return CompletableFuture.completedFuture(null);
        }

        UUID uuid = player.getUniqueId();
        registry.remove(uuid).ifPresent(current -> {
            current.hide();
            scheduler.unschedule(current);
        });

        ScoreboardInstance newInstance = factory.createInstance(player, scoreboard, context);
        registry.set(uuid, newInstance);
        scheduler.schedule(newInstance);

        return newInstance.show().thenApply(v -> newInstance.getId());
    }

    public boolean hideScoreboard(Player player) {
        if (player == null) return false;

        UUID uuid = player.getUniqueId();
        cancelPendingReinit(uuid);

        Optional<ScoreboardInstance> instanceOpt = registry.remove(uuid);
        if (instanceOpt.isEmpty()) return false;

        ScoreboardInstance instance = instanceOpt.get();
        instance.hide();
        scheduler.unschedule(instance);
        TabIntegration.resetScoreboard(player);

        return true;
    }

    public void scheduleReinit(Player player, ScoreboardInstance instance) {
        UUID uuid = player.getUniqueId();
        cancelPendingReinit(uuid);
        ScheduledTask task = Tasks.later(() -> {
            pendingReinitTasks.remove(uuid);
            if (player.isOnline()) instance.reinitialize();
        }, REINIT_DELAY_TICKS);
        pendingReinitTasks.put(uuid, task);
    }

    private void cancelPendingReinit(UUID uuid) {
        ScheduledTask task = pendingReinitTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public Optional<ScoreboardInstance> getScoreboard(Player player) {
        if (player == null) return Optional.empty();
        return registry.get(player.getUniqueId());
    }

    public boolean hasScoreboard(Player player) {
        return player != null && registry.has(player.getUniqueId());
    }

    public void updateContext(Player player, PlaceholderContext context) {
        getScoreboard(player).ifPresent(instance -> instance.updateContext(context));
    }

    public void forceUpdate(Player player) {
        getScoreboard(player).ifPresent(ScoreboardInstance::forceUpdate);
    }

    public void clearPlayerScoreboards(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        cancelPendingReinit(uuid);

        registry.remove(uuid).ifPresent(instance -> {
            instance.hide();
            scheduler.unschedule(instance);
        });

        if (player.isOnline()) {
            TabIntegration.resetScoreboard(player);
        }
    }

    public void hideAll() {
        pendingReinitTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        pendingReinitTasks.clear();
        registry.clear();
        scheduler.shutdown();
    }

    public int getActiveCount() {
        return registry.getActiveCount();
    }

    public ScoreboardStats getStats() {
        return ScoreboardStats.builder()
                .activeScoreboards(registry.getActiveCount())
                .cacheHitRate(0.0)
                .totalUpdates(0)
                .averageRenderTimeMs(0.0)
                .build();
    }

    public void clearCache() {
    }

    public void shutdown() {
        hideAll();
        initialized = false;
    }
}
