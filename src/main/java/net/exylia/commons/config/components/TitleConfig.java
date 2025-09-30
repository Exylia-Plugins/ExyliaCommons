package net.exylia.commons.config.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

@Getter @NoArgsConstructor @AllArgsConstructor
public class TitleConfig {
    private boolean enabled = true;
    private String title = "";
    private String subtitle = "";
    private int fadeIn = 10;
    private int stay = 70;
    private int fadeOut = 20;
    private boolean permanent = false;
    private long updateInterval = 20L;
    public TitleConfig(String basePath, ConfigurationSection config) {
        this(Objects.requireNonNull(config.getConfigurationSection(basePath)));
    }
    public TitleConfig(ConfigurationSection config) {
        this.enabled = config.getBoolean("enabled", true);
        this.title = config.getString("title", "");
        this.subtitle = config.getString("subtitle", "");
        this.fadeIn = config.getInt("fadeIn", 10);
        this.stay = config.getInt("stay", 70);
        this.fadeOut = config.getInt("fadeOut", 20);
        this.permanent = config.getBoolean("permanent", false);
        this.updateInterval = config.getLong("update-interval", 20L);
    }
    public TitleConfig(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }
    public TitleConfig(String title, String subtitle, boolean permanent, long updateInterval) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = 0;
        this.stay = 40;
        this.fadeOut = 0;
        this.permanent = permanent;
        this.updateInterval = updateInterval;
    }
    public TitleConfig(String title, String subtitle, boolean isCountdown) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = 0;
        this.stay = 40;
        this.fadeOut = 0;
        this.permanent = false;
        this.updateInterval = isCountdown ? 1L : 20L;
    }
}