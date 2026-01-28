package net.exylia.commons.v2.discord.api;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.discord.builder.WebhookBuilder;
import net.exylia.commons.v2.discord.core.WebhookManager;
import net.exylia.commons.v2.discord.core.WebhookManager.WebhookResponse;
import net.exylia.commons.v2.discord.model.WebhookTemplate;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class DiscordWebhooks {
    private DiscordWebhooks() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        initialize(plugin, "webhooks");
    }

    public static void initialize(JavaPlugin plugin, String configPath) {
        DebugAPI.logLibInfo(DebugCategory.DISCORD, "Initializing Discord Webhooks System");
        WebhookManager.getInstance().initialize(plugin, configPath);
        DebugAPI.logLibSuccess(DebugCategory.DISCORD, "Discord Webhooks System initialized");
    }

    public static void reload() {
        WebhookManager.getInstance().reload();
    }

    public static CompletableFuture<WebhookResponse> send(String templateId) {
        return WebhookManager.getInstance().send(templateId);
    }

    public static CompletableFuture<WebhookResponse> send(String templateId, Player player) {
        return WebhookManager.getInstance().send(templateId, player);
    }

    public static CompletableFuture<WebhookResponse> send(String templateId, PlaceholderContext context) {
        return WebhookManager.getInstance().send(templateId, context);
    }

    public static CompletableFuture<WebhookResponse> send(String templateId, Player player, PlaceholderContext context) {
        return WebhookManager.getInstance().send(templateId, player, context);
    }

    public static WebhookBuilder builder() {
        return new WebhookBuilder();
    }

    public static WebhookBuilder builder(String url) {
        return new WebhookBuilder().url(url);
    }

    public static WebhookTemplate getTemplate(String id) {
        return WebhookManager.getInstance().getTemplate(id);
    }

    public static boolean hasTemplate(String id) {
        return WebhookManager.getInstance().hasTemplate(id);
    }

    public static void shutdown() {
        DebugAPI.logLibInfo(DebugCategory.DISCORD, "Shutting down Discord Webhooks System");
        WebhookManager.getInstance().shutdown();
        DebugAPI.logLibSuccess(DebugCategory.DISCORD, "Discord Webhooks System shutdown complete");
    }
}
