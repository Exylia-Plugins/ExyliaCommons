package net.exylia.commons.v2.discord.core;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.discord.config.DiscordConfig;
import net.exylia.commons.v2.discord.exception.WebhookException;
import net.exylia.commons.v2.discord.model.WebhookEmbed;
import net.exylia.commons.v2.discord.model.WebhookField;
import net.exylia.commons.v2.discord.model.WebhookTemplate;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebhookManager {
    private static volatile WebhookManager instance;

    @Getter
    private JavaPlugin plugin;
    private final Map<String, WebhookTemplate> templates = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private String configPath = "webhooks";

    private WebhookManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DiscordConfig.Http.TIMEOUT))
                .build();
    }

    public static WebhookManager getInstance() {
        if (instance == null) {
            synchronized (WebhookManager.class) {
                if (instance == null) {
                    instance = new WebhookManager();
                }
            }
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin, String configPath) {
        this.plugin = plugin;
        this.configPath = configPath;
        loadTemplates();
    }

    public void initialize(JavaPlugin plugin) {
        initialize(plugin, "webhooks");
    }

    public void loadTemplates() {
        templates.clear();
        Config config = Configs.get(configPath);

        ConfigurationSection templatesSection = config.section("templates");
        if (templatesSection == null) {
            DebugAPI.logLibDebug(DebugCategory.DISCORD, "No templates section found in " + configPath);
            return;
        }

        for (String id : templatesSection.getKeys(false)) {
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(id);
            if (templateSection != null) {
                String url = templateSection.getString("url");
                if (url == null || url.isEmpty()) {
                    DebugAPI.logLibError(DebugCategory.DISCORD, "Template '" + id + "' has no URL configured, skipping");
                    continue;
                }
                WebhookTemplate template = WebhookTemplate.from(id, templateSection);
                templates.put(id, template);
                DebugAPI.logLibDebug(DebugCategory.DISCORD, "Loaded webhook template: " + id);
            }
        }

        DebugAPI.logLibInfo(DebugCategory.DISCORD, "Loaded " + templates.size() + " webhook templates");
    }

    public void reload() {
        Configs.get(configPath).reload();
        loadTemplates();
    }

    public CompletableFuture<WebhookResponse> send(String templateId) {
        return send(templateId, null, null);
    }

    public CompletableFuture<WebhookResponse> send(String templateId, Player player) {
        return send(templateId, player, null);
    }

    public CompletableFuture<WebhookResponse> send(String templateId, PlaceholderContext context) {
        return send(templateId, null, context);
    }

    public CompletableFuture<WebhookResponse> send(String templateId, Player player, PlaceholderContext context) {
        WebhookTemplate template = templates.get(templateId);
        if (template == null) {
            return CompletableFuture.failedFuture(new WebhookException("Template not found: " + templateId));
        }

        WebhookTemplate processed = processPlaceholders(template, player, context);
        return sendRaw(processed);
    }

    public CompletableFuture<WebhookResponse> sendRaw(WebhookTemplate template) {
        String url = template.getUrl();
        if (url == null || url.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new WebhookException("No webhook URL configured for template: " + template.getId())
            );
        }

        String json = WebhookSerializer.serialize(template);

        return Tasks.io(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(DiscordConfig.Http.TIMEOUT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    DebugAPI.logLibDebug(DebugCategory.DISCORD, "Webhook sent successfully: " + template.getId());
                    return new WebhookResponse(statusCode, response.body(), true);
                } else {
                    DebugAPI.logLibError(DebugCategory.DISCORD, "Webhook failed with status " + statusCode + ": " + response.body());
                    return new WebhookResponse(statusCode, response.body(), false);
                }
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.DISCORD, "Failed to send webhook: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).thenApply(result -> {
            if (result.isSuccess()) {
                return result.getValue().orElseThrow();
            }
            throw new WebhookException("Failed to send webhook", result.getError().orElse(null));
        });
    }

    private WebhookTemplate processPlaceholders(WebhookTemplate template, Player player, PlaceholderContext context) {
        return WebhookTemplate.builder()
                .id(template.getId())
                .url(template.getUrl())
                .content(process(template.getContent(), player, context))
                .username(process(template.getUsername(), player, context))
                .avatarUrl(process(template.getAvatarUrl(), player, context))
                .tts(template.isTts())
                .embeds(processEmbeds(template.getEmbeds(), player, context))
                .build();
    }

    private List<WebhookEmbed> processEmbeds(List<WebhookEmbed> embeds, Player player, PlaceholderContext context) {
        List<WebhookEmbed> processed = new ArrayList<>();
        for (WebhookEmbed embed : embeds) {
            processed.add(WebhookEmbed.builder()
                    .title(process(embed.getTitle(), player, context))
                    .description(process(embed.getDescription(), player, context))
                    .url(process(embed.getUrl(), player, context))
                    .color(embed.getColor())
                    .timestamp(process(embed.getTimestamp(), player, context))
                    .footerText(process(embed.getFooterText(), player, context))
                    .footerIconUrl(process(embed.getFooterIconUrl(), player, context))
                    .imageUrl(process(embed.getImageUrl(), player, context))
                    .thumbnailUrl(process(embed.getThumbnailUrl(), player, context))
                    .authorName(process(embed.getAuthorName(), player, context))
                    .authorUrl(process(embed.getAuthorUrl(), player, context))
                    .authorIconUrl(process(embed.getAuthorIconUrl(), player, context))
                    .fields(processFields(embed.getFields(), player, context))
                    .build());
        }
        return processed;
    }

    private List<WebhookField> processFields(List<WebhookField> fields, Player player, PlaceholderContext context) {
        List<WebhookField> processed = new ArrayList<>();
        for (WebhookField field : fields) {
            processed.add(WebhookField.builder()
                    .name(process(field.getName(), player, context))
                    .value(process(field.getValue(), player, context))
                    .inline(field.isInline())
                    .build());
        }
        return processed;
    }

    private String process(String text, Player player, PlaceholderContext context) {
        if (text == null) return null;
        if (player != null && context != null) {
            return Placeholders.process(text, player, context);
        } else if (player != null) {
            return Placeholders.process(text, player);
        } else if (context != null) {
            return Placeholders.process(text, context);
        }
        return Placeholders.process(text);
    }

    public WebhookTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public boolean hasTemplate(String id) {
        return templates.containsKey(id);
    }

    public void shutdown() {
        templates.clear();
        DebugAPI.logLibInfo(DebugCategory.DISCORD, "WebhookManager shutdown");
    }

    public record WebhookResponse(int statusCode, String body, boolean success) {}
}
