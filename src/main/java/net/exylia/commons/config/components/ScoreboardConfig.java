package net.exylia.commons.config.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter @NoArgsConstructor
@AllArgsConstructor
public class ScoreboardConfig {
    private boolean enabled = true;
    private String title = "";
    private List<String> lines = new ArrayList<>();
    private long updateInterval = 20L;
    public ScoreboardConfig(String basePath, ConfigurationSection config) {
        this(Objects.requireNonNull(config.getConfigurationSection(basePath)));
    }
    public ScoreboardConfig(ConfigurationSection config) {
        this.enabled = config.getBoolean("enabled", true);
        this.lines = config.getStringList("lines");
        this.title = config.getString("title", "");
        this.updateInterval = config.getLong("update-ticks", 20L);
    }
}
