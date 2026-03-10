package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.GlobalCountdownTitleBuilder;
import net.exylia.commons.v2.visual.builder.TitleBuilder;
import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.GlobalVisualRegistry;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.CountdownVisualInstance;
import net.exylia.commons.v2.visual.instance.GlobalCountdownInstance;
import net.exylia.commons.v2.visual.renderer.TitleRenderer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class TitleAPI {
    private TitleAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<String> send(Player player, TitleConfig config) {
        return send(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, TitleConfig config, PlaceholderContext context) {
        return VisualManager.getInstance()
                .sendSimple(player, config, context, TitleRenderer.getInstance(), VisualType.TITLE);
    }

    public static void sendUpdatable(Player player, String key, TitleConfig config, PlaceholderContext context) {
        VisualManager.getInstance().sendOrUpdateContinuous(
                player, key, config, context, TitleRenderer.getInstance(), VisualType.TITLE
        );
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds, TitleConfig config, PlaceholderContext context) {
        long durationTicks = durationSeconds * 20L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, TitleRenderer.getInstance(), VisualType.TITLE, durationTicks);
    }

    public static CompletableFuture<String> countdownMillis(Player player, long durationMillis, TitleConfig config, PlaceholderContext context) {
        long durationTicks = durationMillis / 50L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, TitleRenderer.getInstance(), VisualType.TITLE, durationTicks);
    }

    public static CountdownTitleBuilder countdownBuilder(Player player, int durationSeconds, TitleConfig config) {
        return new CountdownTitleBuilder(player, durationSeconds, config);
    }

    public static CountdownTitleBuilder countdownMillisBuilder(Player player, long durationMillis, TitleConfig config) {
        return new CountdownTitleBuilder(player, (int) (durationMillis / 1000), config);
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
        private final TitleConfig config;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;
        private Consumer<Long> onTick;
        private String key;

        private CountdownTitleBuilder(Player player, int durationSeconds, TitleConfig config) {
            this.player = player;
            this.durationSeconds = durationSeconds;
            this.config = config;
        }

        public CountdownTitleBuilder key(String key) {
            this.key = key;
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

        public CountdownTitleBuilder onTick(Consumer<Long> callback) {
            this.onTick = callback;
            return this;
        }

        public CompletableFuture<String> start() {
            return VisualManager.getInstance().sendCountdown(
                    player, key, config, context, TitleRenderer.getInstance(), VisualType.TITLE,
                    durationSeconds * 20L, onComplete, onCancel, onTick
            );
        }
    }

    public static GlobalCountdownTitleBuilder broadcastCountdown(String id, int durationSeconds, TitleConfig config) {
        return new GlobalCountdownTitleBuilder(id, durationSeconds, config);
    }

    @SuppressWarnings("unchecked")
    public static Optional<GlobalCountdownInstance<TitleConfig>> getGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> (GlobalCountdownInstance<TitleConfig>) instance);
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
