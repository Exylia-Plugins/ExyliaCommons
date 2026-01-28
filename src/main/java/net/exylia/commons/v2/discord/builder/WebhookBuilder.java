package net.exylia.commons.v2.discord.builder;

import net.exylia.commons.v2.discord.core.WebhookManager;
import net.exylia.commons.v2.discord.core.WebhookManager.WebhookResponse;
import net.exylia.commons.v2.discord.model.WebhookEmbed;
import net.exylia.commons.v2.discord.model.WebhookField;
import net.exylia.commons.v2.discord.model.WebhookTemplate;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WebhookBuilder {
    private String url;
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private final List<EmbedBuilder> embeds = new ArrayList<>();

    public WebhookBuilder url(String url) {
        this.url = url;
        return this;
    }

    public WebhookBuilder content(String content) {
        this.content = content;
        return this;
    }

    public WebhookBuilder username(String username) {
        this.username = username;
        return this;
    }

    public WebhookBuilder avatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public WebhookBuilder tts(boolean tts) {
        this.tts = tts;
        return this;
    }

    public EmbedBuilder addEmbed() {
        EmbedBuilder embed = new EmbedBuilder(this);
        embeds.add(embed);
        return embed;
    }

    public WebhookTemplate build() {
        List<WebhookEmbed> builtEmbeds = new ArrayList<>();
        for (EmbedBuilder embed : embeds) {
            builtEmbeds.add(embed.build());
        }

        return WebhookTemplate.builder()
                .id("dynamic")
                .url(url)
                .content(content)
                .username(username)
                .avatarUrl(avatarUrl)
                .tts(tts)
                .embeds(builtEmbeds)
                .build();
    }

    public CompletableFuture<WebhookResponse> send() {
        return WebhookManager.getInstance().sendRaw(build());
    }

    public CompletableFuture<WebhookResponse> send(Player player) {
        return send(player, null);
    }

    public CompletableFuture<WebhookResponse> send(PlaceholderContext context) {
        return send(null, context);
    }

    public CompletableFuture<WebhookResponse> send(Player player, PlaceholderContext context) {
        WebhookTemplate template = build();
        return WebhookManager.getInstance().send(template.getId(), player, context);
    }

    public static class EmbedBuilder {
        private final WebhookBuilder parent;
        private String title;
        private String description;
        private String url;
        private Integer color;
        private String timestamp;
        private String footerText;
        private String footerIconUrl;
        private String imageUrl;
        private String thumbnailUrl;
        private String authorName;
        private String authorUrl;
        private String authorIconUrl;
        private final List<WebhookField> fields = new ArrayList<>();

        EmbedBuilder(WebhookBuilder parent) {
            this.parent = parent;
        }

        public EmbedBuilder title(String title) {
            this.title = title;
            return this;
        }

        public EmbedBuilder description(String description) {
            this.description = description;
            return this;
        }

        public EmbedBuilder url(String url) {
            this.url = url;
            return this;
        }

        public EmbedBuilder color(int color) {
            this.color = color;
            return this;
        }

        public EmbedBuilder color(String hex) {
            if (hex != null && hex.startsWith("#")) {
                this.color = Integer.parseInt(hex.substring(1), 16);
            }
            return this;
        }

        public EmbedBuilder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public EmbedBuilder timestampNow() {
            this.timestamp = java.time.Instant.now().toString();
            return this;
        }

        public EmbedBuilder footer(String text) {
            this.footerText = text;
            return this;
        }

        public EmbedBuilder footer(String text, String iconUrl) {
            this.footerText = text;
            this.footerIconUrl = iconUrl;
            return this;
        }

        public EmbedBuilder image(String url) {
            this.imageUrl = url;
            return this;
        }

        public EmbedBuilder thumbnail(String url) {
            this.thumbnailUrl = url;
            return this;
        }

        public EmbedBuilder author(String name) {
            this.authorName = name;
            return this;
        }

        public EmbedBuilder author(String name, String url) {
            this.authorName = name;
            this.authorUrl = url;
            return this;
        }

        public EmbedBuilder author(String name, String url, String iconUrl) {
            this.authorName = name;
            this.authorUrl = url;
            this.authorIconUrl = iconUrl;
            return this;
        }

        public EmbedBuilder field(String name, String value) {
            fields.add(WebhookField.builder().name(name).value(value).inline(false).build());
            return this;
        }

        public EmbedBuilder field(String name, String value, boolean inline) {
            fields.add(WebhookField.builder().name(name).value(value).inline(inline).build());
            return this;
        }

        public WebhookBuilder done() {
            return parent;
        }

        WebhookEmbed build() {
            return WebhookEmbed.builder()
                    .title(title)
                    .description(description)
                    .url(url)
                    .color(color)
                    .timestamp(timestamp)
                    .footerText(footerText)
                    .footerIconUrl(footerIconUrl)
                    .imageUrl(imageUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .authorName(authorName)
                    .authorUrl(authorUrl)
                    .authorIconUrl(authorIconUrl)
                    .fields(fields)
                    .build();
        }
    }
}
