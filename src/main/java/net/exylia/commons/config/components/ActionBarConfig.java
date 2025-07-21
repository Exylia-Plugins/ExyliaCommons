package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class ActionBarConfig {
    @ConfigValue("enabled")
    private boolean enabled = true;

    @ConfigValue("text")
    private String text = "";

    @ConfigValue("permanent")
    private boolean permanent = false;

    @ConfigValue("update-interval")
    private long updateInterval = 20L;

    // Constructor para inicialización automática
    public ActionBarConfig() {}

    // Constructor para inicialización manual con path base
    public ActionBarConfig(String basePath, ConfigurationSection config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.text = config.getString(basePath + ".text", "");
        this.permanent = config.getBoolean(basePath + ".permanent", false);
        this.updateInterval = config.getLong(basePath + ".update-interval", 20L);
    }

    public ActionBarConfig(String text, long updateInterval) {
        this.text = text;
        this.updateInterval = updateInterval;
    }
}