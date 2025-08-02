package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardConfig {
    @Getter
    @ConfigValue("enabled")
    private boolean enabled = true;

    @Getter
    @ConfigValue("title")
    private String title = "";

    @Getter
    @ConfigValue("lines")
    private List<String> lines = new ArrayList<>();

    @Getter
    @ConfigValue("update-ticks")
    private long updateInterval = 20L;

    // Constructor para inicialización automática
    public ScoreboardConfig() {}

    // Constructor para inicialización manual con path base
    public ScoreboardConfig(String basePath, ConfigurationSection config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.lines = config.getStringList(basePath + ".lines");
        this.title = config.getString(basePath + ".title", "");
        this.updateInterval = config.getLong(basePath + ".update-ticks", 20L);
    }
}