package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.ActionBarBuilder;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.CountdownVisualInstance;
import net.exylia.commons.v2.visual.renderer.ActionBarRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class ActionBarAPI {
    private ActionBarAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<String> send(Player player, String text) {
        return send(player, text, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, String text, PlaceholderContext context) {
        ActionBarConfig config = ActionBarBuilder.create()
                .text(text)
                .build();

        return VisualManager.getInstance()
                .sendSimple(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR);
    }

    public static CompletableFuture<String> send(Player player, ActionBarConfig config) {
        return send(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, ActionBarConfig config, PlaceholderContext context) {
        if (config.isPermanent()) {
            return sendPermanent(player, config, context);
        }
        return VisualManager.getInstance()
                .sendSimple(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR);
    }

    public static CompletableFuture<String> sendPermanent(Player player, String text) {
        return sendPermanent(player, text, PlaceholderContext.create());
    }

    public static CompletableFuture<String> sendPermanent(Player player, String text, PlaceholderContext context) {
        ActionBarConfig config = ActionBarBuilder.create()
                .text(text)
                .permanent()
                .build();

        return sendPermanent(player, config, context);
    }

    public static CompletableFuture<String> sendPermanent(Player player, ActionBarConfig config, PlaceholderContext context) {
        return VisualManager.getInstance()
                .sendContinuous(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR);
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds) {
        return countdown(player, durationSeconds, "{time_formatted}");
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
        ActionBarConfig config = ActionBarBuilder.create()
                .text(text)
                .build();

        return countdown(player, durationSeconds, config, context);
    }

    public static CompletableFuture<String> countdown(
            Player player,
            int durationSeconds,
            ActionBarConfig config,
            PlaceholderContext context
    ) {
        long durationTicks = durationSeconds * 20L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR, durationTicks);
    }

    public static CompletableFuture<String> countdownMillis(Player player, long durationMillis) {
        return countdownMillis(player, durationMillis, "{time_formatted}");
    }

    public static CompletableFuture<String> countdownMillis(Player player, long durationMillis, String text) {
        return countdownMillis(player, durationMillis, text, PlaceholderContext.create());
    }

    public static CompletableFuture<String> countdownMillis(
            Player player,
            long durationMillis,
            String text,
            PlaceholderContext context
    ) {
        ActionBarConfig config = ActionBarBuilder.create()
                .text(text)
                .build();

        return countdownMillis(player, durationMillis, config, context);
    }

    public static CompletableFuture<String> countdownMillis(
            Player player,
            long durationMillis,
            ActionBarConfig config,
            PlaceholderContext context
    ) {
        long durationTicks = durationMillis / 50L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR, durationTicks);
    }

    public static CountdownActionBarBuilder countdownBuilder(Player player, int durationSeconds) {
        return new CountdownActionBarBuilder(player, durationSeconds);
    }

    public static CountdownActionBarBuilder countdownMillisBuilder(Player player, long durationMillis) {
        return new CountdownActionBarBuilder(player, (int) (durationMillis / 1000));
    }

    public static ActionBarBuilder builder() {
        return ActionBarBuilder.create();
    }

    public static boolean cancel(Player player, String actionBarId) {
        return VisualManager.getInstance().cancel(player.getUniqueId(), actionBarId);
    }

    public static void cancelAll(Player player) {
        VisualManager.getInstance().cancelAllByType(player, VisualType.ACTIONBAR);
    }

    public static class CountdownActionBarBuilder {
        private final Player player;
        private final int durationSeconds;
        private String text = "{time_formatted}";
        private long updateInterval = 10L;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;

        private CountdownActionBarBuilder(Player player, int durationSeconds) {
            this.player = player;
            this.durationSeconds = durationSeconds;
        }

        public CountdownActionBarBuilder text(String text) {
            this.text = text;
            return this;
        }

        public CountdownActionBarBuilder updateInterval(long updateInterval) {
            this.updateInterval = updateInterval;
            return this;
        }

        public CountdownActionBarBuilder context(PlaceholderContext context) {
            this.context = context;
            return this;
        }

        public CountdownActionBarBuilder onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownActionBarBuilder onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }

        public CompletableFuture<String> start() {
            ActionBarConfig config = ActionBarBuilder.create()
                    .text(text)
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
}
