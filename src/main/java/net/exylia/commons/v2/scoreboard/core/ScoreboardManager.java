package net.exylia.commons.v2.scoreboard.core;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.listener.ScoreboardListener;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardStats;
import net.exylia.commons.v2.scoreboard.protocol.PacketScoreboardSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public final class ScoreboardManager {

    private static final String LOG_PREFIX = "[Scoreboard] [ScoreboardManager] ";

    private static volatile ScoreboardManager instance;
    private static final Object LOCK = new Object();

    private final Plugin plugin;
    private final ScoreboardRegistry registry;
    private final ScoreboardFactory factory;
    private final ScoreboardScheduler scheduler;
    private volatile boolean initialized;

    private ScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new ScoreboardRegistry();
        this.factory = new ScoreboardFactory();
        this.scheduler = new ScoreboardScheduler();
    }

    public static ScoreboardManager getInstance() {
        ScoreboardManager current = instance;
        if (current == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "getInstance() FAILED -> ScoreboardManager not initialized. Call ScoreboardAPI.initialize(plugin) in onExyliaEnable()/onEnable() first.");
            throw new IllegalStateException("ScoreboardManager not initialized. Call initialize() first.");
        }
        return current;
    }

    public static void initialize(Plugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (instance != null) {
            Bukkit.getLogger().info(LOG_PREFIX + "initialize() called again, already initialized -> ignored");
            return;
        }
        synchronized (LOCK) {
            if (instance == null) {
                Bukkit.getLogger().info(LOG_PREFIX + "initialize(plugin=" + plugin.getName() + ") -> creating ScoreboardManager instance");
                instance = new ScoreboardManager(plugin);
                instance.initializeInternal();
            }
        }
    }

    private void initializeInternal() {
        Bukkit.getPluginManager().registerEvents(new ScoreboardListener(this), plugin);
        initialized = true;
        Bukkit.getLogger().info(LOG_PREFIX + "ScoreboardManager initialized for plugin " + plugin.getName()
                + " | packetEventsAvailable=" + PacketScoreboardSupport.isAvailable());
    }

    public CompletableFuture<String> showScoreboard(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        String playerName = player == null ? "null" : player.getName();
        Bukkit.getLogger().info(LOG_PREFIX + "showScoreboard(" + playerName + ", scoreboardId="
                + (scoreboard == null ? "null" : scoreboard.getId())
                + ", enabled=" + (scoreboard != null && scoreboard.isEnabled())
                + ", lines=" + (scoreboard != null && scoreboard.getLines() != null ? scoreboard.getLines().size() : 0)
                + ") called");

        if (player == null || scoreboard == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "showScoreboard aborted -> player or scoreboard is null (player=" + playerName + ", scoreboard=" + scoreboard + ")");
            return CompletableFuture.completedFuture(null);
        }
        if (!scoreboard.isEnabled()) {
            Bukkit.getLogger().info(LOG_PREFIX + "showScoreboard(" + playerName + ") -> scoreboard.isEnabled()=false, hiding instead");
            hideScoreboard(player);
            return CompletableFuture.completedFuture(null);
        }
        if (!PacketScoreboardSupport.isAvailable()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "showScoreboard(" + playerName + ") FAILED -> PacketEvents is not available. "
                    + "Make sure the 'packetevents' plugin is installed, enabled, and listed in your plugin's depend: section.");
            return CompletableFuture.failedFuture(new IllegalStateException("PacketEvents is required for scoreboards"));
        }

        UUID playerId = player.getUniqueId();
        registry.remove(playerId).ifPresent(previous -> {
            Bukkit.getLogger().info(LOG_PREFIX + "showScoreboard(" + playerName + ") -> hiding previous active scoreboard (id=" + previous.getScoreboard().getId() + ") before showing new one");
            previous.hide();
            scheduler.unschedule(previous);
        });

        ScoreboardInstance created = factory.create(player, scoreboard, context);
        registry.replace(playerId, created);
        scheduler.schedule(created);
        Bukkit.getLogger().info(LOG_PREFIX + "showScoreboard(" + playerName + ") -> instance created and scheduled (interval=" + scoreboard.getUpdateInterval() + " ticks), calling show()...");
        return created.show().whenComplete((id, throwable) -> {
            if (throwable != null) {
                Bukkit.getLogger().warning(LOG_PREFIX + "showScoreboard(" + playerName + ") -> show() FAILED: "
                        + throwable.getClass().getSimpleName() + " - " + throwable.getMessage()
                        + ". Rolling back registry/scheduler.");
                registry.remove(playerId);
                scheduler.unschedule(created);
            } else {
                Bukkit.getLogger().info(LOG_PREFIX + "showScoreboard(" + playerName + ") -> show() SUCCEEDED, id=" + id);
            }
        });
    }

    public boolean hideScoreboard(Player player) {
        String playerName = player == null ? "null" : player.getName();
        if (player == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "hideScoreboard aborted -> player is null");
            return false;
        }
        ScoreboardInstance current = registry.remove(player.getUniqueId()).orElse(null);
        if (current == null) {
            Bukkit.getLogger().info(LOG_PREFIX + "hideScoreboard(" + playerName + ") -> no active scoreboard found, nothing to hide");
            return false;
        }
        scheduler.unschedule(current);
        current.hide();
        Bukkit.getLogger().info(LOG_PREFIX + "hideScoreboard(" + playerName + ") -> hidden scoreboard id=" + current.getScoreboard().getId());
        return true;
    }

    public Optional<ScoreboardInstance> getScoreboard(Player player) {
        return player == null ? Optional.empty() : registry.get(player.getUniqueId());
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

    public void clearPlayer(Player player) {
        hideScoreboard(player);
    }

    public void hideAll() {
        registry.all().forEach(ScoreboardInstance::hide);
        registry.clear();
        scheduler.shutdown();
    }

    public int getActiveCount() {
        return registry.size();
    }

    public ScoreboardStats getStats() {
        long updates = 0L;
        double renderNanos = 0.0;
        for (ScoreboardInstance instance : registry.all()) {
            updates += instance.getTotalUpdates().get();
            renderNanos += instance.getAverageRenderTimeMs();
        }
        int active = registry.size();
        return ScoreboardStats.builder()
                .activeScoreboards(active)
                .totalUpdates(updates)
                .averageRenderTimeMs(active == 0 ? 0.0 : renderNanos / active)
                .cacheHitRate(0.0)
                .build();
    }

    public void clearCache() {
        net.exylia.commons.v2.visual.api.ColorAPI.clearCache();
        net.exylia.commons.v2.placeholders.api.Placeholders.clearCache();
    }

    public void reload() {
        clearCache();
        for (ScoreboardInstance instance : registry.all()) {
            if (instance.isActive()) instance.forceUpdate();
        }
    }

    public void shutdown() {
        hideAll();
        initialized = false;
        synchronized (LOCK) {
            if (instance == this) instance = null;
        }
    }
}
