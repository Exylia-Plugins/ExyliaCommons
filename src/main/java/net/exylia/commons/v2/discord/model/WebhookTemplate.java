package net.exylia.commons.v2.discord.model;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class WebhookTemplate {
    private final String id;
    private final String url;
    private final String content;
    private final String username;
    private final String avatarUrl;
    private final boolean tts;
    @Builder.Default
    private final List<WebhookEmbed> embeds = new ArrayList<>();

    public static WebhookTemplate from(String id, ConfigurationSection section) {
        WebhookTemplateBuilder builder = WebhookTemplate.builder()
                .id(id)
                .url(section.getString("url"))
                .content(section.getString("content"))
                .username(section.getString("username"))
                .avatarUrl(section.getString("avatar_url"))
                .tts(section.getBoolean("tts", false));

        ConfigurationSection embedsSection = section.getConfigurationSection("embeds");
        if (embedsSection != null) {
            List<WebhookEmbed> embeds = new ArrayList<>();
            for (String key : embedsSection.getKeys(false)) {
                ConfigurationSection embedSection = embedsSection.getConfigurationSection(key);
                if (embedSection != null) {
                    embeds.add(WebhookEmbed.from(embedSection));
                }
            }
            builder.embeds(embeds);
        }

        return builder.build();
    }
}
