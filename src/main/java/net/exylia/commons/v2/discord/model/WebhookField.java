package net.exylia.commons.v2.discord.model;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
@Builder
public class WebhookField {
    private final String name;
    private final String value;
    @Builder.Default
    private final boolean inline = false;

    public static WebhookField from(ConfigurationSection section) {
        return WebhookField.builder()
                .name(section.getString("name", ""))
                .value(section.getString("value", ""))
                .inline(section.getBoolean("inline", false))
                .build();
    }
}
