package net.exylia.commons.v2.discord.model;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class WebhookEmbed {
    private final String title;
    private final String description;
    private final String url;
    private final Integer color;
    private final String timestamp;
    private final String footerText;
    private final String footerIconUrl;
    private final String imageUrl;
    private final String thumbnailUrl;
    private final String authorName;
    private final String authorUrl;
    private final String authorIconUrl;
    @Builder.Default
    private final List<WebhookField> fields = new ArrayList<>();

    public static WebhookEmbed from(ConfigurationSection section) {
        WebhookEmbedBuilder builder = WebhookEmbed.builder()
                .title(section.getString("title"))
                .description(section.getString("description"))
                .url(section.getString("url"))
                .timestamp(section.getString("timestamp"));

        String colorStr = section.getString("color");
        if (colorStr != null) {
            builder.color(parseColor(colorStr));
        }

        ConfigurationSection footer = section.getConfigurationSection("footer");
        if (footer != null) {
            builder.footerText(footer.getString("text"));
            builder.footerIconUrl(footer.getString("icon_url"));
        }

        ConfigurationSection image = section.getConfigurationSection("image");
        if (image != null) {
            builder.imageUrl(image.getString("url"));
        }

        ConfigurationSection thumbnail = section.getConfigurationSection("thumbnail");
        if (thumbnail != null) {
            builder.thumbnailUrl(thumbnail.getString("url"));
        }

        ConfigurationSection author = section.getConfigurationSection("author");
        if (author != null) {
            builder.authorName(author.getString("name"));
            builder.authorUrl(author.getString("url"));
            builder.authorIconUrl(author.getString("icon_url"));
        }

        ConfigurationSection fieldsSection = section.getConfigurationSection("fields");
        if (fieldsSection != null) {
            List<WebhookField> fields = new ArrayList<>();
            for (String key : fieldsSection.getKeys(false)) {
                ConfigurationSection fieldSection = fieldsSection.getConfigurationSection(key);
                if (fieldSection != null) {
                    fields.add(WebhookField.from(fieldSection));
                }
            }
            builder.fields(fields);
        }

        return builder.build();
    }

    private static Integer parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) return null;
        try {
            if (colorStr.startsWith("#")) {
                return Integer.parseInt(colorStr.substring(1), 16);
            } else if (colorStr.startsWith("0x")) {
                return Integer.parseInt(colorStr.substring(2), 16);
            }
            return Integer.parseInt(colorStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
