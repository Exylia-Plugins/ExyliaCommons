package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import net.exylia.commons.utils.ActionBarUtils.ActionBarAnimation;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuración reutilizable para ActionBars
 * Maneja automáticamente la carga de todos los valores necesarios
 * Compatible con el sistema de configuración automática de Exylia Commons
 */
public class ActionBarConfig {
    @Getter
    @ConfigValue("enabled")
    private boolean enabled = true;

    @Getter
    @ConfigValue("text")
    private String text = "";

    @ConfigValue("type")
    private String typeString = "SINGLE";

    @ConfigValue("updateInterval")
    private long updateInterval = 20L;

    // Configuraciones específicas por tipo
    @ConfigValue("duration")
    private double duration = 5.0;

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

    @ConfigValue("progressBarLength")
    private int progressBarLength = 20;

    @Getter
    @ConfigValue("progressFilledChar")
    private char progressFilledChar = '█';

    @Getter
    @ConfigValue("progressEmptyChar")
    private char progressEmptyChar = '░';

    // Constructor para inicialización automática
    public ActionBarConfig() {}

    // Constructor para inicialización manual con path base
    public ActionBarConfig(String basePath, FileConfiguration config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.text = config.getString(basePath + ".text", "");
        this.typeString = config.getString(basePath + ".type", "SINGLE");
        this.updateInterval = config.getLong(basePath + ".updateInterval", 20L);
        this.duration = config.getDouble(basePath + ".duration", 5.0);
        this.countdownTime = config.getDouble(basePath + ".countdownTime", 10.0);
        this.countdownFormat = config.getString(basePath + ".countdownFormat", "%time%");
        this.animationString = config.getString(basePath + ".animation.type", "NONE");
        this.animationSpeed = config.getLong(basePath + ".animation.speed", 10);
        this.animationDuration = config.getDouble(basePath + ".animation.duration", 10.0);
        this.loop = config.getBoolean(basePath + ".loop", false);
        this.refreshInterval = config.getLong(basePath + ".refreshInterval", 60L);
        this.progressCurrent = config.getDouble(basePath + ".progressCurrent", 0.0);
        this.progressMax = config.getDouble(basePath + ".progressMax", 100.0);
        this.progressBarLength = config.getInt(basePath + ".progressBarLength", 20);

        // Manejar caracteres de progreso con valores por defecto si no están configurados
        String filledCharStr = config.getString(basePath + ".progressFilledChar", "█");
        String emptyCharStr = config.getString(basePath + ".progressEmptyChar", "░");
        this.progressFilledChar = filledCharStr.isEmpty() ? '█' : filledCharStr.charAt(0);
        this.progressEmptyChar = emptyCharStr.isEmpty() ? '░' : emptyCharStr.charAt(0);
    }

    // ===== GETTERS CON VALIDACIÓN =====

    public ActionBarType getType() {
        try {
            return ActionBarType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActionBarType.SINGLE;
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

    public ActionBarAnimation getAnimation() {
        try {
            return ActionBarAnimation.valueOf(animationString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActionBarAnimation.NONE;
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

    public int getProgressBarLength() {
        return Math.max(1, Math.min(50, progressBarLength));
    }

    // ===== SETTERS PARA RUNTIME =====

    public ActionBarConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ActionBarConfig setText(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    public ActionBarConfig setType(ActionBarType type) {
        this.typeString = type != null ? type.name() : "SINGLE";
        return this;
    }

    public ActionBarConfig setUpdateInterval(long updateInterval) {
        this.updateInterval = Math.max(1, updateInterval);
        return this;
    }

    public ActionBarConfig setDuration(double duration) {
        this.duration = Math.max(0.1, duration);
        return this;
    }

    public ActionBarConfig setCountdownTime(double countdownTime) {
        this.countdownTime = Math.max(0.1, countdownTime);
        return this;
    }

    public ActionBarConfig setCountdownFormat(String countdownFormat) {
        this.countdownFormat = countdownFormat != null ? countdownFormat : "%time%";
        return this;
    }

    public ActionBarConfig setAnimation(ActionBarAnimation animation) {
        this.animationString = animation != null ? animation.name() : "NONE";
        return this;
    }

    public ActionBarConfig setAnimationSpeed(long animationSpeed) {
        this.animationSpeed = Math.max(1, animationSpeed);
        return this;
    }

    public ActionBarConfig setAnimationDuration(double animationDuration) {
        this.animationDuration = Math.max(0.1, animationDuration);
        return this;
    }

    public ActionBarConfig setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public ActionBarConfig setRefreshInterval(long refreshInterval) {
        this.refreshInterval = Math.max(1, refreshInterval);
        return this;
    }

    public ActionBarConfig setProgressCurrent(double progressCurrent) {
        this.progressCurrent = Math.max(0, progressCurrent);
        return this;
    }

    public ActionBarConfig setProgressMax(double progressMax) {
        this.progressMax = Math.max(1, progressMax);
        return this;
    }

    public ActionBarConfig setProgressBarLength(int progressBarLength) {
        this.progressBarLength = Math.max(1, Math.min(50, progressBarLength));
        return this;
    }

    public ActionBarConfig setProgressFilledChar(char progressFilledChar) {
        this.progressFilledChar = progressFilledChar;
        return this;
    }

    public ActionBarConfig setProgressEmptyChar(char progressEmptyChar) {
        this.progressEmptyChar = progressEmptyChar;
        return this;
    }

    public ActionBarConfig setProgressBar(double current, double max, int barLength, char filledChar, char emptyChar) {
        this.progressCurrent = Math.max(0, current);
        this.progressMax = Math.max(1, max);
        this.progressBarLength = Math.max(1, Math.min(50, barLength));
        this.progressFilledChar = filledChar;
        this.progressEmptyChar = emptyChar;
        return this;
    }

    public ActionBarConfig setProgressBar(double current, double max) {
        return setProgressBar(current, max, 20, '█', '░');
    }

    // ===== MÉTODOS DE UTILIDAD =====

    public boolean isValid() {
        return text != null;
    }

    public boolean isSingle() {
        return getType() == ActionBarType.SINGLE;
    }

    public boolean isTimed() {
        return getType() == ActionBarType.TIMED;
    }

    public boolean isPermanent() {
        return getType() == ActionBarType.PERMANENT;
    }

    public boolean isAnimated() {
        return getType() == ActionBarType.ANIMATED;
    }

    public boolean isCountdown() {
        return getType() == ActionBarType.COUNTDOWN;
    }

    public boolean isProgress() {
        return getType() == ActionBarType.PROGRESS;
    }

    // ===== CARGA DE CONFIGURACIÓN AVANZADA =====

    /**
     * Crea una configuración de action bar simple
     */
    public static ActionBarConfig simple(String text) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.SINGLE);
    }

    /**
     * Crea una configuración de action bar permanente
     */
    public static ActionBarConfig permanent(String text, long refreshInterval) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.PERMANENT)
                .setRefreshInterval(refreshInterval);
    }

    /**
     * Crea una configuración de countdown
     */
    public static ActionBarConfig countdown(String text, double time) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.COUNTDOWN)
                .setCountdownTime(time);
    }

    /**
     * Crea una configuración animada
     */
    public static ActionBarConfig animated(String text, ActionBarAnimation animation, double duration) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.ANIMATED)
                .setAnimation(animation)
                .setAnimationDuration(duration);
    }

    /**
     * Crea una configuración temporizada
     */
    public static ActionBarConfig timed(String text, double duration) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.TIMED)
                .setDuration(duration);
    }

    /**
     * Crea una configuración de progreso
     */
    public static ActionBarConfig progress(String text, double current, double max) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.PROGRESS)
                .setProgressBar(current, max);
    }

    /**
     * Crea una configuración de progreso con barra personalizada
     */
    public static ActionBarConfig progress(String text, double current, double max, int barLength, char filledChar, char emptyChar) {
        return new ActionBarConfig()
                .setText(text)
                .setType(ActionBarType.PROGRESS)
                .setProgressBar(current, max, barLength, filledChar, emptyChar);
    }

    @Override
    public String toString() {
        return String.format("ActionBarConfig{enabled=%s, text='%s', type=%s, duration=%.1f}",
                enabled, text, typeString, duration);
    }

    // ===== ENUM PARA TIPOS DE ACTION BAR =====

    /**
     * Enum para los tipos de action bar
     */
    public enum ActionBarType {
        SINGLE,     // Action bar simple (una vez)
        TIMED,      // Action bar durante tiempo específico
        COUNTDOWN,  // Countdown
        ANIMATED,   // Action bar animado
        PERMANENT,  // Action bar permanente
        PROGRESS    // Action bar de progreso
    }

    // ===== BUILDER PATTERN =====

    /**
     * Builder para crear configuraciones complejas
     */
    public static class Builder {
        private final ActionBarConfig config = new ActionBarConfig();

        public Builder enabled(boolean enabled) {
            config.setEnabled(enabled);
            return this;
        }

        public Builder text(String text) {
            config.setText(text);
            return this;
        }

        public Builder type(ActionBarType type) {
            config.setType(type);
            return this;
        }

        public Builder updateInterval(long updateInterval) {
            config.setUpdateInterval(updateInterval);
            return this;
        }

        public Builder timed(double duration) {
            config.setType(ActionBarType.TIMED)
                    .setDuration(duration);
            return this;
        }

        public Builder countdown(double time, String format) {
            config.setType(ActionBarType.COUNTDOWN)
                    .setCountdownTime(time)
                    .setCountdownFormat(format);
            return this;
        }

        public Builder animated(ActionBarAnimation animation, long speed, double duration, boolean loop) {
            config.setType(ActionBarType.ANIMATED)
                    .setAnimation(animation)
                    .setAnimationSpeed(speed)
                    .setAnimationDuration(duration)
                    .setLoop(loop);
            return this;
        }

        public Builder permanent(long refreshInterval) {
            config.setType(ActionBarType.PERMANENT)
                    .setRefreshInterval(refreshInterval);
            return this;
        }

        public Builder progress(double current, double max) {
            config.setType(ActionBarType.PROGRESS)
                    .setProgressBar(current, max);
            return this;
        }

        public Builder progress(double current, double max, int barLength, char filledChar, char emptyChar) {
            config.setType(ActionBarType.PROGRESS)
                    .setProgressBar(current, max, barLength, filledChar, emptyChar);
            return this;
        }

        public ActionBarConfig build() {
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