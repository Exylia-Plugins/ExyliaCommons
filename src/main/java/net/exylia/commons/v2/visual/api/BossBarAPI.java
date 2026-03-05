package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.BossBarBuilder;
import net.exylia.commons.v2.visual.builder.GlobalCountdownBossBarBuilder;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.GlobalVisualRegistry;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.CountdownVisualInstance;
import net.exylia.commons.v2.visual.instance.GlobalCountdownInstance;
import net.exylia.commons.v2.visual.renderer.BossBarRenderer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BossBarAPI {
    private BossBarAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<String> send(Player player, BossBarConfig config) {
        return send(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, BossBarConfig config, PlaceholderContext context) {
        return VisualManager.getInstance()
                .sendSimple(player, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR);
    }

    public static void sendUpdatable(Player player, String key, BossBarConfig config, PlaceholderContext context) {
        VisualManager.getInstance().sendOrUpdateContinuous(
                player, key, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR
        );
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds, BossBarConfig config, PlaceholderContext context) {
        long durationTicks = durationSeconds * 20L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR, durationTicks);
    }

    public static CompletableFuture<String> countdownMillis(Player player, long durationMillis, BossBarConfig config, PlaceholderContext context) {
        long durationTicks = durationMillis / 50L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR, durationTicks);
    }

    public static CountdownBossBarBuilder countdownBuilder(Player player, int durationSeconds, BossBarConfig config) {
        return new CountdownBossBarBuilder(player, durationSeconds, config);
    }

    public static CountdownBossBarBuilder countdownMillisBuilder(Player player, long durationMillis, BossBarConfig config) {
        return new CountdownBossBarBuilder(player, (int) (durationMillis / 1000), config);
    }

    public static BossBarBuilder builder() {
        return BossBarBuilder.create();
    }

    public static boolean cancel(Player player, String bossBarId) {
        boolean cancelled = VisualManager.getInstance().cancel(player.getUniqueId(), bossBarId);
        if (cancelled) {
            BossBarRenderer.getInstance().removeBossBar(player, bossBarId);
        }
        return cancelled;
    }

    public static void cancelAll(Player player) {
        VisualManager.getInstance().cancelAllByType(player, VisualType.BOSSBAR);
        BossBarRenderer.getInstance().removeAllBossBars(player);
    }

    public static class CountdownBossBarBuilder {
        private final Player player;
        private final int durationSeconds;
        private final BossBarConfig config;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;

        private CountdownBossBarBuilder(Player player, int durationSeconds, BossBarConfig config) {
            this.player = player;
            this.durationSeconds = durationSeconds;
            this.config = config;
        }

        public CountdownBossBarBuilder context(PlaceholderContext context) {
            this.context = context;
            return this;
        }

        public CountdownBossBarBuilder onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownBossBarBuilder onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }

        public CompletableFuture<String> start() {
            return countdown(player, durationSeconds, config, context)
                    .thenApply(id -> {
                        VisualRegistry.getInstance()
                                .get(player.getUniqueId(), id)
                                .ifPresent(instance -> {
                                    if (instance instanceof CountdownVisualInstance<?> countdown) {
                                        if (onComplete != null) countdown.setOnComplete(onComplete);
                                        if (onCancel != null) countdown.setOnCancel(onCancel);
                                    }
                                });
                        return id;
                    });
        }
    }

    public static GlobalCountdownBossBarBuilder broadcastCountdown(String id, int durationSeconds, BossBarConfig config) {
        return new GlobalCountdownBossBarBuilder(id, durationSeconds, config);
    }

    @SuppressWarnings("unchecked")
    public static Optional<GlobalCountdownInstance<BossBarConfig>> getGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> (GlobalCountdownInstance<BossBarConfig>) instance);
    }

    public static boolean cancelGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> { instance.cancel(); return true; })
                .orElse(false);
    }

    public static boolean restartGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> { instance.restart(); return true; })
                .orElse(false);
    }

    public static boolean pauseGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> { instance.pause(); return true; })
                .orElse(false);
    }

    public static boolean resumeGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> { instance.resume(); return true; })
                .orElse(false);
    }
}
