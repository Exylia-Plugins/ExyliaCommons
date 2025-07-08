package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import net.exylia.commons.utils.BossbarUtils.BossBarAnimation;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuración reutilizable para BossBars
 * Maneja automáticamente la carga de todos los valores necesarios
 * Compatible con el sistema de configuración automática de Exylia Commons
 */
public class BossBarConfig {
    @Getter
    @ConfigValue("enabled")
    private boolean enabled = true;

    @Getter
    @ConfigValue("text")
    private String text = "";

    @ConfigValue("color")
    private String colorString = "BLUE";

    @ConfigValue("style")
    private String overlayString = "PROGRESS";

    @ConfigValue("progress")
    private float progress = 1.0f;

    @ConfigValue("type")
    private String typeString = "STATIC";

    @ConfigValue("updateInterval")
    private long updateInterval = 5L;

    // Configuraciones específicas por tipo
    @ConfigValue("duration")
    private double duration = 10.0;

    @ConfigValue("decreasing")
    private boolean decreasing = true;

    @Getter
    @ConfigValue("autoHide")
    private boolean autoHide = true;

    @ConfigValue("countdownTime")
    private double countdownTime = 10.0;

    @Getter
    @ConfigValue("countdownFormat")
    private String countdownFormat = "%time%";

    @ConfigValue("animation.type")
    private String animationString = "NONE";

    @ConfigValue("animation.speed")
    private long animationSpeed = 10;

    @ConfigValue("animation.duration")
    private double animationDuration = 10.0;

    @Getter
    @ConfigValue("loop")
    private boolean loop = false;

    @ConfigValue("refreshInterval")
    private long refreshInterval = 60L;

    // Progress específico
    @ConfigValue("progressCurrent")
    private double progressCurrent = 0.0;

    @ConfigValue("progressMax")
    private double progressMax = 100.0;

    // Constructor para inicialización automática
    public BossBarConfig() {}

    // Constructor para inicialización manual con path base
    public BossBarConfig(String basePath, FileConfiguration config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.text = config.getString(basePath + ".text", "");
        this.colorString = config.getString(basePath + ".color", "BLUE");
        this.overlayString = config.getString(basePath + ".style", "PROGRESS");
        this.progress = (float) config.getDouble(basePath + ".progress", 1.0);
        this.typeString = config.getString(basePath + ".type", "STATIC");
        this.updateInterval = config.getLong(basePath + ".updateInterval", 5L);
        this.duration = config.getDouble(basePath + ".duration", 10.0);
        this.decreasing = config.getBoolean(basePath + ".decreasing", true);
        this.autoHide = config.getBoolean(basePath + ".autoHide", true);
        this.countdownTime = config.getDouble(basePath + ".countdownTime", 10.0);
        this.countdownFormat = config.getString(basePath + ".countdownFormat", "%time%");
        this.animationString = config.getString(basePath + ".animation.type", "NONE");
        this.animationSpeed = config.getLong(basePath + ".animation.speed", 10);
        this.animationDuration = config.getDouble(basePath + ".animation.duration", 10.0);
        this.loop = config.getBoolean(basePath + ".loop", false);
        this.refreshInterval = config.getLong(basePath + ".refreshInterval", 60L);
        this.progressCurrent = config.getDouble(basePath + ".progressCurrent", 0.0);
        this.progressMax = config.getDouble(basePath + ".progressMax", 100.0);
    }

    // ===== GETTERS CON VALIDACIÓN =====

    public BossBar.Color getColor() {
        try {
            return BossBar.Color.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    public BossBar.Overlay getOverlay() {
        try {
            return BossBar.Overlay.valueOf(overlayString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    public float getProgress() {
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    public BossBarType getType() {
        try {
            return BossBarType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBarType.STATIC;
        }
    }

    public long getUpdateInterval() {
        return Math.max(1, updateInterval);
    }

    public double getDuration() {
        return Math.max(0.1, duration);
    }

    public double getCountdownTime() {
        return Math.max(0.1, countdownTime);
    }

    public BossBarAnimation getAnimation() {
        try {
            return BossBarAnimation.valueOf(animationString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBarAnimation.NONE;
        }
    }

    public long getAnimationSpeed() {
        return Math.max(1, animationSpeed);
    }

    public double getAnimationDuration() {
        return Math.max(0.1, animationDuration);
    }

    public long getRefreshInterval() {
        return Math.max(1, refreshInterval);
    }

    public double getProgressCurrent() {
        return Math.max(0, progressCurrent);
    }

    public double getProgressMax() {
        return Math.max(1, progressMax);
    }

    public boolean isDecreasing() {
        return decreasing;
    }

    // ===== SETTERS PARA RUNTIME =====

    public BossBarConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public BossBarConfig setText(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    public BossBarConfig setColor(BossBar.Color color) {
        this.colorString = color != null ? color.name() : "BLUE";
        return this;
    }

    public BossBarConfig setOverlay(BossBar.Overlay overlay) {
        this.overlayString = overlay != null ? overlay.name() : "PROGRESS";
        return this;
    }

    public BossBarConfig setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        return this;
    }

    public BossBarConfig setType(BossBarType type) {
        this.typeString = type != null ? type.name() : "STATIC";
        return this;
    }

    public BossBarConfig setUpdateInterval(long updateInterval) {
        this.updateInterval = Math.max(1, updateInterval);
        return this;
    }

    public BossBarConfig setDuration(double duration) {
        this.duration = Math.max(0.1, duration);
        return this;
    }

    public BossBarConfig setDecreasing(boolean decreasing) {
        this.decreasing = decreasing;
        return this;
    }

    public BossBarConfig setAutoHide(boolean autoHide) {
        this.autoHide = autoHide;
        return this;
    }

    public BossBarConfig setCountdownTime(double countdownTime) {
        this.countdownTime = Math.max(0.1, countdownTime);
        return this;
    }

    public BossBarConfig setCountdownFormat(String countdownFormat) {
        this.countdownFormat = countdownFormat != null ? countdownFormat : "%time%";
        return this;
    }

    public BossBarConfig setAnimation(BossBarAnimation animation) {
        this.animationString = animation != null ? animation.name() : "NONE";
        return this;
    }

    public BossBarConfig setAnimationSpeed(long animationSpeed) {
        this.animationSpeed = Math.max(1, animationSpeed);
        return this;
    }

    public BossBarConfig setAnimationDuration(double animationDuration) {
        this.animationDuration = Math.max(0.1, animationDuration);
        return this;
    }

    public BossBarConfig setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public BossBarConfig setRefreshInterval(long refreshInterval) {
        this.refreshInterval = Math.max(1, refreshInterval);
        return this;
    }

    public BossBarConfig setProgressCurrent(double progressCurrent) {
        this.progressCurrent = Math.max(0, progressCurrent);
        return this;
    }

    public BossBarConfig setProgressMax(double progressMax) {
        this.progressMax = Math.max(1, progressMax);
        return this;
    }

    public BossBarConfig setProgressBar(double current, double max) {
        this.progressCurrent = Math.max(0, current);
        this.progressMax = Math.max(1, max);
        this.progress = (float) Math.min(1.0, this.progressCurrent / this.progressMax);
        return this;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    public boolean isValid() {
        return text != null && progress >= 0.0f && progress <= 1.0f;
    }

    public boolean isStatic() {
        return getType() == BossBarType.STATIC;
    }

    public boolean isTimed() {
        return getType() == BossBarType.TIMED;
    }

    public boolean isPermanent() {
        return getType() == BossBarType.PERMANENT;
    }

    public boolean isAnimated() {
        return getType() == BossBarType.ANIMATED;
    }

    public boolean isCountdown() {
        return getType() == BossBarType.COUNTDOWN;
    }

    public boolean isProgress() {
        return getType() == BossBarType.PROGRESS;
    }

    // ===== CARGA DE CONFIGURACIÓN AVANZADA =====

    /**
     * Crea una configuración de boss bar simple
     */
    public static BossBarConfig simple(String text, BossBar.Color color) {
        return new BossBarConfig()
                .setText(text)
                .setColor(color)
                .setType(BossBarType.STATIC);
    }

    /**
     * Crea una configuración de boss bar permanente
     */
    public static BossBarConfig permanent(String text, BossBar.Color color, long refreshInterval) {
        return new BossBarConfig()
                .setText(text)
                .setColor(color)
                .setType(BossBarType.PERMANENT)
                .setRefreshInterval(refreshInterval);
    }

    /**
     * Crea una configuración de countdown
     */
    public static BossBarConfig countdown(String text, BossBar.Color color, double time) {
        return new BossBarConfig()
                .setText(text)
                .setColor(color)
                .setType(BossBarType.COUNTDOWN)
                .setCountdownTime(time);
    }

    /**
     * Crea una configuración animada
     */
    public static BossBarConfig animated(String text, BossBar.Color color, BossBarAnimation animation, double duration) {
        return new BossBarConfig()
                .setText(text)
                .setColor(color)
                .setType(BossBarType.ANIMATED)
                .setAnimation(animation)
                .setAnimationDuration(duration);
    }

    /**
     * Crea una configuración temporizada
     */
    public static BossBarConfig timed(String text, BossBar.Color color, double duration, boolean decreasing) {
        return new BossBarConfig()
                .setText(text)
                .setColor(color)
                .setType(BossBarType.TIMED)
                .setDuration(duration)
                .setDecreasing(decreasing);
    }

    /**
     * Crea una configuración de progreso
     */
    public static BossBarConfig progress(String text, BossBar.Color color, double current, double max) {
        return new BossBarConfig()
                .setText(text)
                .setColor(color)
                .setType(BossBarType.PROGRESS)
                .setProgressBar(current, max);
    }

    @Override
    public String toString() {
        return String.format("BossBarConfig{enabled=%s, text='%s', color=%s, overlay=%s, type=%s, progress=%.2f}",
                enabled, text, colorString, overlayString, typeString, progress);
    }

    // ===== ENUM PARA TIPOS DE BOSS BAR =====

    /**
     * Enum para los tipos de boss bar
     */
    public enum BossBarType {
        STATIC,     // Boss bar estática
        TIMED,      // Boss bar con tiempo específico
        COUNTDOWN,  // Countdown
        ANIMATED,   // Boss bar animada
        PERMANENT,  // Boss bar permanente
        PROGRESS    // Boss bar de progreso
    }

    // ===== BUILDER PATTERN =====

    /**
     * Builder para crear configuraciones complejas
     */
    public static class Builder {
        private final BossBarConfig config = new BossBarConfig();

        public Builder enabled(boolean enabled) {
            config.setEnabled(enabled);
            return this;
        }

        public Builder text(String text) {
            config.setText(text);
            return this;
        }

        public Builder color(BossBar.Color color) {
            config.setColor(color);
            return this;
        }

        public Builder overlay(BossBar.Overlay overlay) {
            config.setOverlay(overlay);
            return this;
        }

        public Builder progress(float progress) {
            config.setProgress(progress);
            return this;
        }

        public Builder type(BossBarType type) {
            config.setType(type);
            return this;
        }

        public Builder timed(double duration, boolean decreasing, boolean autoHide) {
            config.setType(BossBarType.TIMED)
                    .setDuration(duration)
                    .setDecreasing(decreasing)
                    .setAutoHide(autoHide);
            return this;
        }

        public Builder countdown(double time, String format) {
            config.setType(BossBarType.COUNTDOWN)
                    .setCountdownTime(time)
                    .setCountdownFormat(format);
            return this;
        }

        public Builder animated(BossBarAnimation animation, long speed, double duration, boolean loop) {
            config.setType(BossBarType.ANIMATED)
                    .setAnimation(animation)
                    .setAnimationSpeed(speed)
                    .setAnimationDuration(duration)
                    .setLoop(loop);
            return this;
        }

        public Builder permanent(long refreshInterval) {
            config.setType(BossBarType.PERMANENT)
                    .setRefreshInterval(refreshInterval);
            return this;
        }

        public Builder progressBar(double current, double max) {
            config.setType(BossBarType.PROGRESS)
                    .setProgressBar(current, max);
            return this;
        }

        public BossBarConfig build() {
            return config;
        }
    }

    /**
     * Crea un builder para configuración compleja
     */
    public static Builder builder() {
        return new Builder();
    }
}