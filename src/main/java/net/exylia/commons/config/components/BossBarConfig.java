package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

public class BossBarConfig {
    @Getter
    @ConfigValue("enabled")
    private boolean enabled = true;

    @Getter
    @ConfigValue("text")
    private String text = "";

    @Getter
    @ConfigValue("color")
    private String color = "BLUE";

    @Getter
    @ConfigValue("style")
    private String style = "PROGRESS";

    @Getter
    @ConfigValue("progress")
    private double progress = 1.0;

    // Constructor para inicialización automática
    public BossBarConfig() {}

    // Constructor para inicialización manual con path base
    public BossBarConfig(String basePath, ConfigurationSection config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.text = config.getString(basePath + ".text", "");
        this.color = config.getString(basePath + ".color", "BLUE");
        this.style = config.getString(basePath + ".style", "PROGRESS");
        this.progress = config.getDouble(basePath + ".progress", 1.0);
    }
}