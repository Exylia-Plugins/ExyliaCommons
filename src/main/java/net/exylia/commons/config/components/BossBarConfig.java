package net.exylia.commons.config.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

@Getter @NoArgsConstructor
@AllArgsConstructor
public class BossBarConfig {
    private boolean enabled = true;
    private String text = "";
    private String color = "BLUE";
    private String style = "PROGRESS";
    private double progress = 1.0;
    private boolean permanent = false;
    private long updateInterval = 20L;
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
}