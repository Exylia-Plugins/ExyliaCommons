package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.ActionBarBuilder;
import net.exylia.commons.v2.visual.builder.GlobalCountdownActionBarBuilder;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.GlobalVisualRegistry;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.CountdownVisualInstance;
import net.exylia.commons.v2.visual.instance.GlobalCountdownInstance;
import net.exylia.commons.v2.visual.renderer.ActionBarRenderer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ActionBarAPI {
    private ActionBarAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<String> send(Player player, ActionBarConfig config) {
        return send(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> send(Player player, ActionBarConfig config, PlaceholderContext context) {
        return VisualManager.getInstance()
                .sendSimple(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR);
    }

    public static void sendUpdatable(Player player, String key, ActionBarConfig config, PlaceholderContext context) {
        VisualManager.getInstance().sendOrUpdateContinuous(
                player, key, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR
        );
    }

    public static CompletableFuture<String> countdown(Player player, int durationSeconds, ActionBarConfig config, PlaceholderContext context) {
        long durationTicks = durationSeconds * 20L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR, durationTicks);
    }

    public static CompletableFuture<String> countdownMillis(Player player, long durationMillis, ActionBarConfig config, PlaceholderContext context) {
        long durationTicks = durationMillis / 50L;
        return VisualManager.getInstance()
                .sendCountdown(player, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR, durationTicks);
    }

    public static CountdownActionBarBuilder countdownBuilder(Player player, int durationSeconds, ActionBarConfig config) {
        return new CountdownActionBarBuilder(player, durationSeconds, config);
    }

    public static CountdownActionBarBuilder countdownMillisBuilder(Player player, long durationMillis, ActionBarConfig config) {
        return new CountdownActionBarBuilder(player, (int) (durationMillis / 1000), config);
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
        private final ActionBarConfig config;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;
        private String key;

        private CountdownActionBarBuilder(Player player, int durationSeconds, ActionBarConfig config) {
            this.player = player;
            this.durationSeconds = durationSeconds;
            this.config = config;
        }

        public CountdownActionBarBuilder key(String key) {
            this.key = key;
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
            return VisualManager.getInstance().sendCountdown(
                    player, key, config, context, ActionBarRenderer.getInstance(), VisualType.ACTIONBAR,
                    durationSeconds * 20L, onComplete, onCancel
            );
        }
    }

    public static GlobalCountdownActionBarBuilder broadcastCountdown(String id, int durationSeconds, ActionBarConfig config) {
        return new GlobalCountdownActionBarBuilder(id, durationSeconds, config);
    }

    @SuppressWarnings("unchecked")
    public static Optional<GlobalCountdownInstance<ActionBarConfig>> getGlobalCountdown(String id) {
        return GlobalVisualRegistry.getInstance().get(id)
                .map(instance -> (GlobalCountdownInstance<ActionBarConfig>) instance);
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
