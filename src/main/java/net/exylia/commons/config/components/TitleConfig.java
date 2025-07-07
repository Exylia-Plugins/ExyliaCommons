package net.exylia.commons.config.components;

import lombok.Getter;
import net.exylia.commons.config.ConfigValue;
import net.exylia.commons.utils.TitleUtils.TitleAnimation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración reutilizable para Títulos
 * Maneja automáticamente la carga de todos los valores necesarios
 * Compatible con el sistema de configuración automática de Exylia Commons
 */
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

    @ConfigValue("fadeIn")
    private int fadeIn = 10;

    @ConfigValue("stay")
    private int stay = 70;

    @ConfigValue("fadeOut")
    private int fadeOut = 20;

    @ConfigValue("type")
    private String typeString = "SINGLE";

    // Configuraciones específicas por tipo
    @ConfigValue("repetitions")
    private int repetitions = 3;

    @ConfigValue("delayBetween")
    private long delayBetween = 80;

    @ConfigValue("countdownTime")
    private double countdownTime = 10.0;

    @Getter
    @ConfigValue("countdownFormat")
    private String countdownFormat = "%time%";

    @ConfigValue("animation.type")
    private String animationString = "NONE";

    @ConfigValue("animation.speed")
    private long animationSpeed = 3;

    @ConfigValue("duration")
    private double duration = 10.0;

    @Getter
    @ConfigValue("loop")
    private boolean loop = false;

    @ConfigValue("refreshInterval")
    private long refreshInterval = 60L;

    // Lista de pasos para secuencias (se carga manualmente)
    private List<StepConfig> steps = new ArrayList<>();

    // Constructor para inicialización automática
    public TitleConfig() {}

    // Constructor para inicialización manual con path base
    public TitleConfig(String basePath, FileConfiguration config) {
        this.enabled = config.getBoolean(basePath + ".enabled", true);
        this.title = config.getString(basePath + ".title", "");
        this.subtitle = config.getString(basePath + ".subtitle", "");
        this.fadeIn = config.getInt(basePath + ".fadeIn", 10);
        this.stay = config.getInt(basePath + ".stay", 70);
        this.fadeOut = config.getInt(basePath + ".fadeOut", 20);
        this.typeString = config.getString(basePath + ".type", "SINGLE");
        this.repetitions = config.getInt(basePath + ".repetitions", 3);
        this.delayBetween = config.getLong(basePath + ".delayBetween", 80);
        this.countdownTime = config.getDouble(basePath + ".countdownTime", 10.0);
        this.countdownFormat = config.getString(basePath + ".countdownFormat", "%time%");
        this.animationString = config.getString(basePath + ".animation.type", "NONE");
        this.animationSpeed = config.getLong(basePath + ".animation.speed", 3);
        this.duration = config.getDouble(basePath + ".duration", 10.0);
        this.loop = config.getBoolean(basePath + ".loop", false);
        this.refreshInterval = config.getLong(basePath + ".refreshInterval", 60L);

        // Cargar pasos de secuencia si existen
        loadSteps(basePath, config);
    }

    // ===== GETTERS CON VALIDACIÓN =====

    public int getFadeIn() {
        return Math.max(0, fadeIn);
    }

    public int getStay() {
        return Math.max(1, stay);
    }

    public int getFadeOut() {
        return Math.max(0, fadeOut);
    }

    public TitleType getType() {
        try {
            return TitleType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TitleType.SINGLE;
        }
    }

    public int getRepetitions() {
        return Math.max(1, repetitions);
    }

    public long getDelayBetween() {
        return Math.max(1, delayBetween);
    }

    public double getCountdownTime() {
        return Math.max(0.1, countdownTime);
    }

    public TitleAnimation getAnimation() {
        try {
            return TitleAnimation.valueOf(animationString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TitleAnimation.NONE;
        }
    }

    public long getAnimationSpeed() {
        return Math.max(1, animationSpeed);
    }

    public double getDuration() {
        return Math.max(0.1, duration);
    }

    public long getRefreshInterval() {
        return Math.max(1, refreshInterval);
    }

    public List<StepConfig> getSteps() {
        return new ArrayList<>(steps);
    }

    // ===== SETTERS PARA RUNTIME =====

    public TitleConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public TitleConfig setTitle(String title) {
        this.title = title != null ? title : "";
        return this;
    }

    public TitleConfig setSubtitle(String subtitle) {
        this.subtitle = subtitle != null ? subtitle : "";
        return this;
    }

    public TitleConfig setTiming(int fadeIn, int stay, int fadeOut) {
        this.fadeIn = Math.max(0, fadeIn);
        this.stay = Math.max(1, stay);
        this.fadeOut = Math.max(0, fadeOut);
        return this;
    }

    public TitleConfig setFadeIn(int fadeIn) {
        this.fadeIn = Math.max(0, fadeIn);
        return this;
    }

    public TitleConfig setStay(int stay) {
        this.stay = Math.max(1, stay);
        return this;
    }

    public TitleConfig setFadeOut(int fadeOut) {
        this.fadeOut = Math.max(0, fadeOut);
        return this;
    }

    public TitleConfig setType(TitleType type) {
        this.typeString = type != null ? type.name() : "SINGLE";
        return this;
    }

    public TitleConfig setRepetitions(int repetitions) {
        this.repetitions = Math.max(1, repetitions);
        return this;
    }

    public TitleConfig setDelayBetween(long delayBetween) {
        this.delayBetween = Math.max(1, delayBetween);
        return this;
    }

    public TitleConfig setCountdownTime(double countdownTime) {
        this.countdownTime = Math.max(0.1, countdownTime);
        return this;
    }

    public TitleConfig setCountdownFormat(String countdownFormat) {
        this.countdownFormat = countdownFormat != null ? countdownFormat : "%time%";
        return this;
    }

    public TitleConfig setAnimation(TitleAnimation animation) {
        this.animationString = animation != null ? animation.name() : "NONE";
        return this;
    }

    public TitleConfig setAnimationSpeed(long animationSpeed) {
        this.animationSpeed = Math.max(1, animationSpeed);
        return this;
    }

    public TitleConfig setDuration(double duration) {
        this.duration = Math.max(0.1, duration);
        return this;
    }

    public TitleConfig setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public TitleConfig setRefreshInterval(long refreshInterval) {
        this.refreshInterval = Math.max(1, refreshInterval);
        return this;
    }

    public TitleConfig addStep(StepConfig step) {
        if (step != null) {
            this.steps.add(step);
        }
        return this;
    }

    public TitleConfig addStep(String title, String subtitle, int fadeIn, int stay, int fadeOut, long delay) {
        return addStep(new StepConfig(title, subtitle, fadeIn, stay, fadeOut, delay));
    }

    public TitleConfig addStep(String title, String subtitle, long delay) {
        return addStep(title, subtitle, this.fadeIn, this.stay, this.fadeOut, delay);
    }

    public TitleConfig clearSteps() {
        this.steps.clear();
        return this;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    public boolean isValid() {
        return title != null && subtitle != null &&
                fadeIn >= 0 && stay >= 1 && fadeOut >= 0;
    }

    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    public boolean isSequence() {
        return getType() == TitleType.SEQUENCE;
    }

    public boolean isPermanent() {
        return getType() == TitleType.PERMANENT;
    }

    public boolean isAnimated() {
        return getType() == TitleType.ANIMATED;
    }

    public boolean isCountdown() {
        return getType() == TitleType.COUNTDOWN;
    }

    public boolean isRepeated() {
        return getType() == TitleType.REPEATED;
    }

    // ===== CARGA DE CONFIGURACIÓN AVANZADA =====

    /**
     * Carga los pasos de secuencia desde la configuración
     */
    private void loadSteps(String basePath, FileConfiguration config) {
        ConfigurationSection stepsSection = config.getConfigurationSection(basePath + ".steps");
        if (stepsSection == null) return;

        this.steps.clear();
        for (String key : stepsSection.getKeys(false)) {
            ConfigurationSection stepSection = stepsSection.getConfigurationSection(key);
            if (stepSection != null) {
                StepConfig step = new StepConfig(
                        stepSection.getString("title", ""),
                        stepSection.getString("subtitle", ""),
                        stepSection.getInt("fadeIn", this.fadeIn),
                        stepSection.getInt("stay", this.stay),
                        stepSection.getInt("fadeOut", this.fadeOut),
                        stepSection.getLong("delay", 0)
                );
                this.steps.add(step);
            }
        }
    }

    /**
     * Carga configuración desde un ConfigurationSection específico
     */
    public static TitleConfig fromSection(ConfigurationSection section) {
        if (section == null) return new TitleConfig();

        TitleConfig config = new TitleConfig();
        config.enabled = section.getBoolean("enabled", true);
        config.title = section.getString("title", "");
        config.subtitle = section.getString("subtitle", "");
        config.fadeIn = section.getInt("fadeIn", 10);
        config.stay = section.getInt("stay", 70);
        config.fadeOut = section.getInt("fadeOut", 20);
        config.typeString = section.getString("type", "SINGLE");
        config.repetitions = section.getInt("repetitions", 3);
        config.delayBetween = section.getLong("delayBetween", 80);
        config.countdownTime = section.getDouble("countdownTime", 10.0);
        config.countdownFormat = section.getString("countdownFormat", "%time%");

        // Cargar configuración de animación
        ConfigurationSection animSection = section.getConfigurationSection("animation");
        if (animSection != null) {
            config.animationString = animSection.getString("type", "NONE");
            config.animationSpeed = animSection.getLong("speed", 3);
        }

        config.duration = section.getDouble("duration", 10.0);
        config.loop = section.getBoolean("loop", false);
        config.refreshInterval = section.getLong("refreshInterval", 60L);

        // Cargar pasos si es una secuencia
        config.loadStepsFromSection(section);

        return config;
    }

    private void loadStepsFromSection(ConfigurationSection section) {
        ConfigurationSection stepsSection = section.getConfigurationSection("steps");
        if (stepsSection == null) return;

        this.steps.clear();
        for (String key : stepsSection.getKeys(false)) {
            ConfigurationSection stepSection = stepsSection.getConfigurationSection(key);
            if (stepSection != null) {
                StepConfig step = new StepConfig(
                        stepSection.getString("title", ""),
                        stepSection.getString("subtitle", ""),
                        stepSection.getInt("fadeIn", this.fadeIn),
                        stepSection.getInt("stay", this.stay),
                        stepSection.getInt("fadeOut", this.fadeOut),
                        stepSection.getLong("delay", 0)
                );
                this.steps.add(step);
            }
        }
    }

    /**
     * Crea una configuración de título simple
     */
    public static TitleConfig simple(String title, String subtitle) {
        return new TitleConfig()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setType(TitleType.SINGLE);
    }

    /**
     * Crea una configuración de título permanente
     */
    public static TitleConfig permanent(String title, String subtitle, long refreshInterval) {
        return new TitleConfig()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setType(TitleType.PERMANENT)
                .setRefreshInterval(refreshInterval);
    }

    /**
     * Crea una configuración de countdown
     */
    public static TitleConfig countdown(String title, String subtitle, double time) {
        return new TitleConfig()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setType(TitleType.COUNTDOWN)
                .setCountdownTime(time);
    }

    /**
     * Crea una configuración animada
     */
    public static TitleConfig animated(String title, String subtitle, TitleAnimation animation, double duration) {
        return new TitleConfig()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setType(TitleType.ANIMATED)
                .setAnimation(animation)
                .setDuration(duration);
    }

    /**
     * Crea una configuración repetida
     */
    public static TitleConfig repeated(String title, String subtitle, int repetitions, long delay) {
        return new TitleConfig()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setType(TitleType.REPEATED)
                .setRepetitions(repetitions)
                .setDelayBetween(delay);
    }

    @Override
    public String toString() {
        return String.format("TitleConfig{enabled=%s, title='%s', subtitle='%s', type=%s, fadeIn=%d, stay=%d, fadeOut=%d}",
                enabled, title, subtitle, typeString, fadeIn, stay, fadeOut);
    }

    // ===== CLASES AUXILIARES =====

    /**
     * Configuración de un paso en una secuencia
     */
    public static class StepConfig {
        @Getter private final String title;
        @Getter private final String subtitle;
        @Getter private final int fadeIn;
        @Getter private final int stay;
        @Getter private final int fadeOut;
        @Getter private final long delay;

        public StepConfig(String title, String subtitle, int fadeIn, int stay, int fadeOut, long delay) {
            this.title = title != null ? title : "";
            this.subtitle = subtitle != null ? subtitle : "";
            this.fadeIn = Math.max(0, fadeIn);
            this.stay = Math.max(1, stay);
            this.fadeOut = Math.max(0, fadeOut);
            this.delay = Math.max(0, delay);
        }

        public StepConfig(String title, String subtitle, long delay) {
            this(title, subtitle, 10, 70, 20, delay);
        }

        public boolean isValid() {
            return title != null && subtitle != null && fadeIn >= 0 && stay >= 1 && fadeOut >= 0;
        }

        @Override
        public String toString() {
            return String.format("StepConfig{title='%s', subtitle='%s', timing=[%d,%d,%d], delay=%d}",
                    title, subtitle, fadeIn, stay, fadeOut, delay);
        }
    }

    /**
     * Enum para los tipos de título
     */
    public enum TitleType {
        SINGLE,     // Título único
        REPEATED,   // Título repetido
        COUNTDOWN,  // Countdown
        ANIMATED,   // Título animado
        SEQUENCE,   // Secuencia de títulos
        PERMANENT   // Título permanente
    }

    // ===== BUILDER PATTERN OPCIONAL =====

    /**
     * Builder para crear configuraciones complejas
     */
    public static class Builder {
        private final TitleConfig config = new TitleConfig();

        public Builder enabled(boolean enabled) {
            config.setEnabled(enabled);
            return this;
        }

        public Builder title(String title) {
            config.setTitle(title);
            return this;
        }

        public Builder subtitle(String subtitle) {
            config.setSubtitle(subtitle);
            return this;
        }

        public Builder timing(int fadeIn, int stay, int fadeOut) {
            config.setTiming(fadeIn, stay, fadeOut);
            return this;
        }

        public Builder type(TitleType type) {
            config.setType(type);
            return this;
        }

        public Builder repeated(int repetitions, long delay) {
            config.setType(TitleType.REPEATED)
                    .setRepetitions(repetitions)
                    .setDelayBetween(delay);
            return this;
        }

        public Builder countdown(double time, String format) {
            config.setType(TitleType.COUNTDOWN)
                    .setCountdownTime(time)
                    .setCountdownFormat(format);
            return this;
        }

        public Builder animated(TitleAnimation animation, long speed, double duration, boolean loop) {
            config.setType(TitleType.ANIMATED)
                    .setAnimation(animation)
                    .setAnimationSpeed(speed)
                    .setDuration(duration)
                    .setLoop(loop);
            return this;
        }

        public Builder permanent(long refreshInterval) {
            config.setType(TitleType.PERMANENT)
                    .setRefreshInterval(refreshInterval);
            return this;
        }

        public Builder addStep(String title, String subtitle, long delay) {
            config.addStep(title, subtitle, delay);
            return this;
        }

        public Builder addStep(String title, String subtitle, int fadeIn, int stay, int fadeOut, long delay) {
            config.addStep(title, subtitle, fadeIn, stay, fadeOut, delay);
            return this;
        }

        public TitleConfig build() {
            return config;
        }
    }

    /**
     * Crea un builder para configuración compleja
     */
    public static Builder builder() {
        return new Builder();
    }

    // ===== CONFIGURACIONES PREDEFINIDAS =====

    /**
     * Configuraciones comunes predefinidas
     */
    public static class Presets {
        public static TitleConfig welcome() {
            return simple("&a&l¡Bienvenido %player_name%!", "&7Disfruta tu estadía");
        }

        public static TitleConfig achievement() {
            return animated("&6&l¡LOGRO DESBLOQUEADO!", "&e%achievement_name%",
                    TitleAnimation.BOUNCE, 3.0);
        }

        public static TitleConfig warning() {
            return animated("&c&l¡ADVERTENCIA!", "&7%warning_message%",
                    TitleAnimation.BLINK, 2.0);
        }

        public static TitleConfig loading() {
            return permanent("&b&lCargando...", "&7Por favor espera", 20L);
        }

        public static TitleConfig countdown10() {
            return countdown("&c&l%time%", "&7segundos restantes", 10.0);
        }

        public static TitleConfig progressBar() {
            return simple("&b%progress_percent%", "%progress_bar%");
        }
    }
}