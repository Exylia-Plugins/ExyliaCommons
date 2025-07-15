package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;

public class TitleConfig {
    @Getter
    @ConfigValue("enabled")
    private boolean enabled = true;

    @Getter
    @ConfigValue("title")
    private String title = "";

    @Getter
    @ConfigValue("subtitle")
    private String subtitle = "";

    @Getter
    @ConfigValue("fadeIn")
    private int fadeIn = 10;

    @Getter
    @ConfigValue("stay")
    private int stay = 70;

    @Getter
    @ConfigValue("fadeOut")
    private int fadeOut = 20;

    @Getter
    @ConfigValue("permanent")
    private boolean permanent = false;

    @Getter
    @ConfigValue("refresh-interval")
    private long refreshInterval = 60L;

    // Constructor para inicialización automática
    public TitleConfig() {}

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

    public TitleConfig(String title, String subtitle, int fadeIn, int stay, int fadeOut, boolean permanent, long refreshInterval) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.permanent = permanent;
        this.refreshInterval = refreshInterval;
    }
}