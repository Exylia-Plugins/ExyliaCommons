package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

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
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.text = config.getString(basePath + ".text", "");
        this.color = config.getString(basePath + ".color", "BLUE");
        this.style = config.getString(basePath + ".style", "PROGRESS");
        this.progress = config.getDouble(basePath + ".progress", 1.0);
        this.permanent = config.getBoolean(basePath + ".permanent", false);
        this.updateInterval = config.getLong(basePath + ".update-interval", 20L);
    }

    public BossBarConfig(String text, String color, String style, double progress, long updateInterval) {
        this.text = text;
        this.color = color;
        this.style = style;
        this.progress = progress;
        this.updateInterval = updateInterval;
    }
}