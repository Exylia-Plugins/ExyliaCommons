package net.exylia.commons.config.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

@Getter @NoArgsConstructor @AllArgsConstructor
public class TitleConfig {
    @ConfigValue("enabled")
    private boolean enabled = true;
    @ConfigValue("title")
    private String title = "";
    @ConfigValue("subtitle")
    private String subtitle = "";
    @ConfigValue("fadeIn")
    private int fadeIn = 10;
    @ConfigValue("stay")
    private int stay = 70;
    @ConfigValue("fadeOut")
    private int fadeOut = 20;
    @ConfigValue("permanent")
    private boolean permanent = false;
    @ConfigValue("refresh-interval")
    private long refreshInterval = 20L;

    // Constructor para inicialización manual con path base
    public TitleConfig(String basePath, ConfigurationSection config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.title = config.getString(basePath + ".title", "");
        this.subtitle = config.getString(basePath + ".subtitle", "");
        this.fadeIn = config.getInt(basePath + ".fadeIn", 10);
        this.stay = config.getInt(basePath + ".stay", 70);
        this.fadeOut = config.getInt(basePath + ".fadeOut", 20);
    }

    public TitleConfig(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public TitleConfig(String title, String subtitle, boolean permanent, long refreshInterval) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = 0;
        this.stay = 40;
        this.fadeOut = 0;
        this.permanent = permanent;
        this.refreshInterval = refreshInterval;
    }

    public TitleConfig(String title, String subtitle, boolean isCountdown) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = 0;
        this.stay = 40;
        this.fadeOut = 0;
        this.permanent = false;
        this.refreshInterval = isCountdown ? 1L : 20L;
    }
}