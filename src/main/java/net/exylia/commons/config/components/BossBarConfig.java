package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

@Getter
public class BossBarConfig {
    @ConfigValue("enabled")
    private boolean enabled = true;

    @ConfigValue("text")
    private String text = "";

    @ConfigValue("color")
    private String color = "BLUE";

    @ConfigValue("style")
    private String style = "PROGRESS";

    @ConfigValue("progress")
    private double progress = 1.0;

    @ConfigValue("permanent")
    private boolean permanent = false;

    @ConfigValue("update-interval")
    private long updateInterval = 20L;

    // Constructor para inicialización automática
    public BossBarConfig() {}

    // Constructor para inicialización manual con path base
    public BossBarConfig(String basePath, ConfigurationSection config) {
        this(Objects.requireNonNull(config.getConfigurationSection(basePath)));
    }

    public BossBarConfig(ConfigurationSection config) {
        this.enabled = config.getBoolean("enabled", true);
        this.text = config.getString("text", "");
        this.color = config.getString("color", "BLUE");
        this.style = config.getString("style", "PROGRESS");
        this.progress = config.getDouble("progress", 1.0);
        this.permanent = config.getBoolean("permanent", false);
        this.updateInterval = config.getLong("update-interval", 20L);
    }


    public BossBarConfig(String text, String color, String style, double progress, long updateInterval) {
        this.text = text;
        this.color = color;
        this.style = style;
        this.progress = progress;
        this.updateInterval = updateInterval;
    }
}