package net.exylia.commons.v2.scoreboard.core;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.cache.ScoreboardCacheManager;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.listener.PlayerCleanupListener;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardStats;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
public class ScoreboardManager {

    private static volatile ScoreboardManager instance;
    private static final Object LOCK = new Object();

    private final Plugin plugin;
    private final ScoreboardRegistry registry;
    private final ScoreboardFactory factory;
    private final ScoreboardScheduler scheduler;
    private final ScoreboardCacheManager cacheManager;
    private boolean initialized;

    private ScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new ScoreboardRegistry();
        this.cacheManager = new ScoreboardCacheManager();
        this.factory = new ScoreboardFactory(plugin, cacheManager);
        this.scheduler = new ScoreboardScheduler(plugin, registry);
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
        registerListeners();
        initialized = true;
    }

    private void registerListeners() {
        PlayerCleanupListener listener = new PlayerCleanupListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public CompletableFuture<String> showScoreboard(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context
    ) {
        if (player == null || scoreboard == null) {
            return CompletableFuture.completedFuture(null);
        }

        boolean isFirstScoreboard = !registry.has(player.getUniqueId());

        if (isFirstScoreboard) {
            org.bukkit.scoreboard.Scoreboard bukkitScoreboard = player.getScoreboard();
            if (bukkitScoreboard != null &&
                !bukkitScoreboard.equals(plugin.getServer().getScoreboardManager().getMainScoreboard())) {
                registry.saveOriginalScoreboard(player.getUniqueId(), bukkitScoreboard);
            }
        }

        Optional<ScoreboardInstance> currentOpt = registry.peek(player.getUniqueId());
        if (currentOpt.isPresent()) {
            ScoreboardInstance current = currentOpt.get();
            current.hide();
            scheduler.unschedule(current);
        }

        ScoreboardInstance instance = factory.createInstance(player, scoreboard, context);

        registry.push(player.getUniqueId(), instance);
        scheduler.schedule(instance);

        return instance.show()
                .thenApply(v -> instance.getId());
    }

    public boolean hideScoreboard(Player player) {
        if (player == null) {
            return false;
        }

        Optional<ScoreboardInstance> instanceOpt = registry.pop(player.getUniqueId());

        if (instanceOpt.isPresent()) {
            ScoreboardInstance instance = instanceOpt.get();

            instance.hide();
            scheduler.unschedule(instance);
            cacheManager.invalidatePlayer(player.getUniqueId());

            Optional<ScoreboardInstance> previousOpt = registry.peek(player.getUniqueId());
            if (previousOpt.isPresent()) {
                ScoreboardInstance previous = previousOpt.get();
                scheduler.schedule(previous);
                previous.show();
            } else {
                Optional<org.bukkit.scoreboard.Scoreboard> originalOpt = registry.getOriginalScoreboard(player.getUniqueId());
                if (originalOpt.isPresent() && player.isOnline()) {
                    player.setScoreboard(originalOpt.get());
                }
                registry.removeOriginalScoreboard(player.getUniqueId());
            }

            return true;
        }

        return false;
    }

    public Optional<ScoreboardInstance> getScoreboard(Player player) {
        if (player == null) {
            return Optional.empty();
        }

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
        if (player == null) {
            return;
        }

        while (registry.has(player.getUniqueId())) {
            Optional<ScoreboardInstance> instanceOpt = registry.pop(player.getUniqueId());
            if (instanceOpt.isPresent()) {
                ScoreboardInstance instance = instanceOpt.get();
                instance.hide();
                scheduler.unschedule(instance);
            }
        }

        registry.removeOriginalScoreboard(player.getUniqueId());
        cacheManager.invalidatePlayer(player.getUniqueId());
    }

    public void hideAll() {
        registry.clear();
        scheduler.shutdown();
        cacheManager.clearAll();
    }

    public int getActiveCount() {
        return registry.getActiveCount();
    }

    public ScoreboardStats getStats() {
        int activeScoreboards = registry.getActiveCount();
        double cacheHitRate = cacheManager.getAverageHitRate();

        return ScoreboardStats.builder()
                .activeScoreboards(activeScoreboards)
                .cacheHitRate(cacheHitRate)
                .totalUpdates(0)
                .averageRenderTimeMs(0.0)
                .build();
    }

    public void clearCache() {
        cacheManager.clearAll();
    }

    public void shutdown() {
        hideAll();
        initialized = false;
    }
}
