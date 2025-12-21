package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarRenderer implements VisualRenderer<BossBarConfig> {
    private static final BossBarRenderer INSTANCE = new BossBarRenderer();
    private final Map<UUID, Map<String, BossBar>> activeBossBars = new ConcurrentHashMap<>();

    private BossBarRenderer() {
    }

    public static BossBarRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> renderAsync(Player player, BossBarConfig config, PlaceholderContext context) {
        return AsyncExecutor.getInstance()
                .supplyAsync(() -> {
                    Component component = CacheManager.getInstance()
                            .processAndParse(config.getText(), player, context);

                    double progress = config.getProgress();
                    Object contextProgress = context.get("progress");
                    if (contextProgress instanceof Number) {
                        progress = ((Number) contextProgress).doubleValue();
                    }

                    return new BossBarData(component, progress);
                }, false)
                .thenAcceptAsync(data -> {
                    SchedulerManager.getInstance().runSync(() -> {
                        if (!player.isOnline()) return;

                        String visualId = (String) context.get("visual_id");
                        BossBar bossBar = getOrCreateBossBar(player, visualId, config);

                        bossBar.name(data.component);
                        bossBar.progress((float) Math.max(0.0, Math.min(1.0, data.progress)));

                        player.showBossBar(bossBar);
                    });
                }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    public CompletableFuture<Void> renderBatch(
            Collection<Player> players,
            BossBarConfig config,
            PlaceholderContext context
    ) {
        return AsyncExecutor.getInstance()
                .supplyAsync(() -> {
                    Component component = CacheManager.getInstance()
                            .processAndParse(config.getText(), null, context);

                    double progress = config.getProgress();
                    Object contextProgress = context.get("progress");
                    if (contextProgress instanceof Number) {
                        progress = ((Number) contextProgress).doubleValue();
                    }

                    return new BossBarData(component, progress);
                }, false)
                .thenAcceptAsync(data -> {
                    SchedulerManager.getInstance().runSync(() -> {
                        for (Player player : players) {
                            if (!player.isOnline()) continue;

                            String visualId = (String) context.get("visual_id");
                            BossBar bossBar = getOrCreateBossBar(player, visualId, config);

                            bossBar.name(data.component);
                            bossBar.progress((float) Math.max(0.0, Math.min(1.0, data.progress)));

                            player.showBossBar(bossBar);
                        }
                    });
                }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    private BossBar getOrCreateBossBar(Player player, String visualId, BossBarConfig config) {
        Map<String, BossBar> playerBars = activeBossBars.computeIfAbsent(
                player.getUniqueId(),
                k -> new ConcurrentHashMap<>()
        );

        return playerBars.computeIfAbsent(visualId, k ->
                BossBar.bossBar(
                        Component.empty(),
                        (float) config.getProgress(),
                        config.getColor(),
                        config.getStyle()
                )
        );
    }

    @Override
    public void cleanup(Player player, String visualId) {
        SchedulerManager schedulerManager = SchedulerManager.getInstance();
        if (schedulerManager.isMainThread()) {
            removeBossBar(player, visualId);
        } else {
            schedulerManager.runSync(() -> removeBossBar(player, visualId));
        }
    }

    public void removeBossBar(Player player, String visualId) {
        Map<String, BossBar> playerBars = activeBossBars.get(player.getUniqueId());
        if (playerBars != null) {
            BossBar bossBar = playerBars.remove(visualId);
            if (bossBar != null && player.isOnline()) {
                player.hideBossBar(bossBar);
            }

            if (playerBars.isEmpty()) {
                activeBossBars.remove(player.getUniqueId());
            }
        }
    }

    public void removeAllBossBars(Player player) {
        SchedulerManager schedulerManager = SchedulerManager.getInstance();
        if (schedulerManager.isMainThread()) {
            removeAllBossBarsSync(player);
        } else {
            schedulerManager.runSync(() -> removeAllBossBarsSync(player));
        }
    }

    private void removeAllBossBarsSync(Player player) {
        Map<String, BossBar> playerBars = activeBossBars.remove(player.getUniqueId());
        if (playerBars != null && player.isOnline()) {
            for (BossBar bossBar : playerBars.values()) {
                player.hideBossBar(bossBar);
            }
        }
    }

    private record BossBarData(Component component, double progress) {
    }
}
