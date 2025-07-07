package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import net.kyori.adventure.bossbar.BossBar;

/**
 * Configuración reutilizable para BossBars
 * Maneja automáticamente la carga de todos los valores necesarios
 */
public class BossBarConfig {
    @Getter
    @ConfigValue("enabled")
    private boolean enabled = true;

    @Getter
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

    public BossBar.Color getColor() {
        try {
            return BossBar.Color.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.YELLOW;
        }
    }

    public BossBar.Overlay getStyle() {
        try {
            return BossBar.Overlay.valueOf(styleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
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

    public BossBarConfig setColor(BossBar.Color color) {
        this.colorString = color.name();
        return this;
    }

    public BossBarConfig setStyle(BossBar.Overlay style) {
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

    @Override
    public String toString() {
        return String.format("BossBarConfig{enabled=%s, title='%s', color=%s, style=%s, progress=%.2f}",
                enabled, title, colorString, styleString, progress);
    }
}