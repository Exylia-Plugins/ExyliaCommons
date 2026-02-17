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
import net.exylia.commons.v2.visual.instance.VisualInstance;
import net.exylia.commons.v2.visual.renderer.BossBarRenderer;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BossBarAPI {
    private BossBarAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<String> send(Player player, String text) {
        return send(player, text, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, String text, PlaceholderContext context) {
        BossBarConfig config = BossBarBuilder.create()
                .text(text)
                .build();

        return send(player, config, context);
    }

    public static CompletableFuture<String> send(Player player, BossBarConfig config) {
        return send(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, BossBarConfig config, PlaceholderContext context) {
        if (config.isPermanent()) {
            return sendPermanent(player, config, context);
        }
        return VisualManager.getInstance()
                .sendSimple(player, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR);
    }

    public static CompletableFuture<String> sendPermanent(Player player, String text) {
        return sendPermanent(player, text, PlaceholderContext.create());
    }

    public static CompletableFuture<String> sendPermanent(Player player, String text, PlaceholderContext context) {
        BossBarConfig config = BossBarBuilder.create()
                .text(text)
                .permanent()
                .build();

        return sendPermanent(player, config, context);
    }

    public static CompletableFuture<String> sendPermanent(Player player, BossBarConfig config, PlaceholderContext context) {
        return VisualManager.getInstance()
                .sendContinuous(player, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR);
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds) {
        return countdown(player, durationSeconds, "Tiempo: {time_formatted}");
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds, String text) {
        return countdown(player, durationSeconds, text, PlaceholderContext.create());
    }

    public static CompletableFuture<String> countdown(
            Player player,
            int durationSeconds,
            String text,
            PlaceholderContext context
    ) {
        BossBarConfig config = BossBarBuilder.create()
                .text(text)
                .color(BossBar.Color.BLUE)
                .build();

        return countdown(player, durationSeconds, config, context);
    }

    public static CompletableFuture<String> countdown(
            Player player,
            int durationSeconds,
            BossBarConfig config,
            PlaceholderContext context
    ) {
        long durationTicks = durationSeconds * 20L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR, durationTicks);
    }

    public static CountdownBossBarBuilder countdownBuilder(Player player, int durationSeconds) {
        return new CountdownBossBarBuilder(player, durationSeconds);
    }

    public static BossBarBuilder builder() {
        return BossBarBuilder.create();
    }

    public static void sendUpdatable(Player player, String key, String text, PlaceholderContext context) {
        BossBarConfig config = BossBarBuilder.create()
                .text(text)
                .permanent()
                .build();

        VisualManager.getInstance().sendOrUpdateContinuous(
                player, key, config, context, BossBarRenderer.getInstance(), VisualType.BOSSBAR
        );
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
        private String text = "Tiempo: {time_formatted}";
        private BossBar.Color color = BossBar.Color.BLUE;
        private BossBar.Overlay style = BossBar.Overlay.PROGRESS;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;

        private CountdownBossBarBuilder(Player player, int durationSeconds) {
            this.player = player;
            this.durationSeconds = durationSeconds;
        }

        public CountdownBossBarBuilder text(String text) {
            this.text = text;
            return this;
        }

        public CountdownBossBarBuilder color(BossBar.Color color) {
            this.color = color;
            return this;
        }

        public CountdownBossBarBuilder style(BossBar.Overlay style) {
            this.style = style;
            return this;
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
            BossBarConfig config = BossBarBuilder.create()
                    .text(text)
                    .color(color)
                    .style(style)
                    .build();

            return countdown(player, durationSeconds, config, context)
                    .thenApply(id -> {
                        VisualRegistry.getInstance()
                                .get(player.getUniqueId(), id)
                                .ifPresent(instance -> {
                                    if (instance instanceof CountdownVisualInstance) {
                                        CountdownVisualInstance<?> countdown = (CountdownVisualInstance<?>) instance;
                                        if (onComplete != null) {
                                            countdown.setOnComplete(onComplete);
                                        }
                                        if (onCancel != null) {
                                            countdown.setOnCancel(onCancel);
                                        }
                                    }
                                });
                        return id;
                    });
        }
    }

    public static GlobalCountdownBossBarBuilder broadcastCountdown(String id, int durationSeconds) {
        return new GlobalCountdownBossBarBuilder(id, durationSeconds);
    }

    @SuppressWarnings("unchecked")
    public static Optional<GlobalCountdownInstance<BossBarConfig>> getGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> (GlobalCountdownInstance<BossBarConfig>) instance);
    }

    public static boolean cancelGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> {
                    instance.cancel();
                    return true;
                })
                .orElse(false);
    }

    public static boolean restartGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> {
                    instance.restart();
                    return true;
                })
                .orElse(false);
    }

    public static boolean pauseGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> {
                    instance.pause();
                    return true;
                })
                .orElse(false);
    }

    public static boolean resumeGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> {
                    instance.resume();
                    return true;
                })
                .orElse(false);
    }
}
