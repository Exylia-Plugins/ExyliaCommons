package net.exylia.commons.config.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

@Getter @NoArgsConstructor
@AllArgsConstructor
public class ActionBarConfig {
    private boolean enabled = true;
    private String text = "";
    private boolean permanent = false;
    private long updateInterval = 20L;
    public ActionBarConfig(String basePath, ConfigurationSection config) {
        this(Objects.requireNonNull(config.getConfigurationSection(basePath)));
    }
    public ActionBarConfig(ConfigurationSection config) {
        this.enabled = config.getBoolean("enabled", true);
        this.text = config.getString("text", "");
        this.permanent = config.getBoolean("permanent", false);
        this.updateInterval = config.getLong("update-interval", 20L);
    }

    public ActionBarConfig(String text, long updateInterval) {
        this.text = text;
        this.updateInterval = updateInterval;
    }
}
