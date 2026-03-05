package net.exylia.commons.v2.scoreboard.core;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.listener.ScoreboardListener;
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
    private boolean initialized;

    private ScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new ScoreboardRegistry();
        this.factory = new ScoreboardFactory(plugin);
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
        plugin.getServer().getPluginManager().registerEvents(new ScoreboardListener(this), plugin);
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

        if (!registry.has(player.getUniqueId())) {
            registry.saveOriginalScoreboard(player.getUniqueId(), player.getScoreboard());
        }

        registry.peek(player.getUniqueId()).ifPresent(current -> {
            current.hide();
            scheduler.unschedule(current);
        });

        ScoreboardInstance newInstance = factory.createInstance(player, scoreboard, context);
        registry.push(player.getUniqueId(), newInstance);
        scheduler.schedule(newInstance);

        return newInstance.show().thenApply(v -> newInstance.getId());
    }

    public boolean hideScoreboard(Player player) {
        if (player == null) return false;

        Optional<ScoreboardInstance> instanceOpt = registry.pop(player.getUniqueId());
        if (instanceOpt.isEmpty()) return false;

        ScoreboardInstance instance = instanceOpt.get();
        instance.hide();
        scheduler.unschedule(instance);

        Optional<ScoreboardInstance> previous = registry.peek(player.getUniqueId());
        if (previous.isPresent()) {
            scheduler.schedule(previous.get());
            previous.get().show();
        } else {
            if (player.isOnline()) {
                registry.getOriginalScoreboard(player.getUniqueId())
                        .ifPresent(player::setScoreboard);
            }
            registry.removeOriginalScoreboard(player.getUniqueId());
        }

        return true;
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

        while (registry.has(player.getUniqueId())) {
            registry.pop(player.getUniqueId()).ifPresent(instance -> {
                instance.hide();
                scheduler.unschedule(instance);
            });
        }

        registry.removeOriginalScoreboard(player.getUniqueId());
    }

    public void hideAll() {
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
