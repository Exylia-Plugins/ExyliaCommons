package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.ActionBarBuilder;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualType;
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

    public static ActionBarBuilder builder() {
        return ActionBarBuilder.create();
    }

    public static boolean cancel(Player player, String actionBarId) {
        return VisualManager.getInstance().cancel(player.getUniqueId(), actionBarId);
    }

    public static void cancelAll(Player player) {
        VisualManager.getInstance().cancelAllByType(player, VisualType.ACTIONBAR);
    }
}
