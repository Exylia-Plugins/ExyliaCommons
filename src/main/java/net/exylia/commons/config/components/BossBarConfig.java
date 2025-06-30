package net.exylia.commons.config.components;

import net.exylia.commons.config.ConfigValue;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

/**
 * Configuración reutilizable para BossBars
 * Maneja automáticamente la carga de todos los valores necesarios
 */
public class BossBarConfig {
    @ConfigValue("enabled")
    private boolean enabled = true;

    @ConfigValue("title")
    private String title = "";

    @ConfigValue("color")
    private String colorString = "YELLOW";

    @ConfigValue("style")
    private String styleString = "SOLID";

    @ConfigValue("progress")
    private double progress = 1.0;

    // Constructor para inicialización manual con path base
    public BossBarConfig() {}

    public BossBarConfig(String basePath, org.bukkit.configuration.file.FileConfiguration config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.title = config.getString(basePath + ".title", "");
        this.colorString = config.getString(basePath + ".color", "YELLOW");
        this.styleString = config.getString(basePath + ".style", "SOLID");
        this.progress = config.getDouble(basePath + ".progress", 1.0);
    }

    // ===== GETTERS =====

    public boolean isEnabled() {
        return enabled;
    }

    public String getTitle() {
        return title;
    }

    public BarColor getColor() {
        try {
            return BarColor.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.YELLOW;
        }
    }

    public BarStyle getStyle() {
        try {
            return BarStyle.valueOf(styleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    public double getProgress() {
        return Math.max(0.0, Math.min(1.0, progress));
    }

    // ===== SETTERS PARA RUNTIME =====

    public BossBarConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public BossBarConfig setTitle(String title) {
        this.title = title;
        return this;
    }

    public BossBarConfig setColor(BarColor color) {
        this.colorString = color.name();
        return this;
    }

    public BossBarConfig setStyle(BarStyle style) {
        this.styleString = style.name();
        return this;
    }

    public BossBarConfig setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        return this;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    public boolean isValid() {
        return title != null && !title.isEmpty() && progress >= 0.0 && progress <= 1.0;
    }

    public BossBarConfig copy() {
        BossBarConfig copy = new BossBarConfig();
        copy.enabled = this.enabled;
        copy.title = this.title;
        copy.colorString = this.colorString;
        copy.styleString = this.styleString;
        copy.progress = this.progress;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("BossBarConfig{enabled=%s, title='%s', color=%s, style=%s, progress=%.2f}",
                enabled, title, colorString, styleString, progress);
    }
}