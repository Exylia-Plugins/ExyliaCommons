package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.TitleBuilder;
import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.CountdownVisualInstance;
import net.exylia.commons.v2.visual.renderer.TitleRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class TitleAPI {
    private TitleAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<String> send(Player player, String title, String subtitle) {
        return send(player, title, subtitle, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, String title, String subtitle, PlaceholderContext context) {
        TitleConfig config = TitleBuilder.create()
                .title(title)
                .subtitle(subtitle)
                .build();

        return send(player, config, context);
    }

    public static CompletableFuture<String> send(Player player, TitleConfig config) {
        return send(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, TitleConfig config, PlaceholderContext context) {
        if (config.isPermanent()) {
            return sendPermanent(player, config, context);
        }
        return VisualManager.getInstance()
                .sendSimple(player, config, context, TitleRenderer.getInstance(), VisualType.TITLE);
    }

    public static CompletableFuture<String> sendPermanent(Player player, String title, String subtitle) {
        return sendPermanent(player, title, subtitle, PlaceholderContext.create());
    }

    public static CompletableFuture<String> sendPermanent(
            Player player,
            String title,
            String subtitle,
            PlaceholderContext context
    ) {
        TitleConfig config = TitleBuilder.create()
                .title(title)
                .subtitle(subtitle)
                .permanent()
                .build();

        return sendPermanent(player, config, context);
    }

    public static CompletableFuture<String> sendPermanent(Player player, TitleConfig config, PlaceholderContext context) {
        return VisualManager.getInstance()
                .sendContinuous(player, config, context, TitleRenderer.getInstance(), VisualType.TITLE);
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds) {
        return countdown(player, durationSeconds, "Countdown", "{time}");
    }

    public static CompletableFuture<String> countdown(
            Player player,
            int durationSeconds,
            String title,
            String subtitle
    ) {
        return countdown(player, durationSeconds, title, subtitle, PlaceholderContext.create());
    }

    public static CompletableFuture<String> countdown(
            Player player,
            int durationSeconds,
            String title,
            String subtitle,
            PlaceholderContext context
    ) {
        TitleConfig config = TitleBuilder.create()
                .title(title)
                .subtitle(subtitle)
                .build();

        return countdown(player, durationSeconds, config, context);
    }

    public static CompletableFuture<String> countdown(
            Player player,
            int durationSeconds,
            TitleConfig config,
            PlaceholderContext context
    ) {
        long durationTicks = durationSeconds * 20L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, TitleRenderer.getInstance(), VisualType.TITLE, durationTicks);
    }

    public static CountdownTitleBuilder countdownBuilder(Player player, int durationSeconds) {
        return new CountdownTitleBuilder(player, durationSeconds);
    }

    public static TitleBuilder builder() {
        return TitleBuilder.create();
    }

    public static boolean cancel(Player player, String titleId) {
        return VisualManager.getInstance().cancel(player.getUniqueId(), titleId);
    }

    public static void cancelAll(Player player) {
        VisualManager.getInstance().cancelAllByType(player, VisualType.TITLE);
    }

    public static class CountdownTitleBuilder {
        private final Player player;
        private final int durationSeconds;
        private String title = "Countdown";
        private String subtitle = "{time}";
        private int fadeIn = 10;
        private int stay = 70;
        private int fadeOut = 20;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;

        private CountdownTitleBuilder(Player player, int durationSeconds) {
            this.player = player;
            this.durationSeconds = durationSeconds;
        }

        public CountdownTitleBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CountdownTitleBuilder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public CountdownTitleBuilder times(int fadeIn, int stay, int fadeOut) {
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
            return this;
        }

        public CountdownTitleBuilder context(PlaceholderContext context) {
            this.context = context;
            return this;
        }

        public CountdownTitleBuilder onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownTitleBuilder onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }

        public CompletableFuture<String> start() {
            TitleConfig config = TitleBuilder.create()
                    .title(title)
                    .subtitle(subtitle)
                    .times(fadeIn, stay, fadeOut)
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
