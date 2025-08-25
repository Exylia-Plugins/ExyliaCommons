package net.exylia.commons.config.base;

import net.exylia.commons.config.ConfigBase;
import net.exylia.commons.config.ConfigFile;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.config.ConfigValue;

/**
 * Configuración base para config.yml que contiene configuraciones comunes
 * Esta clase debe ser extendida por MainConfig en cada plugin
 */
@ConfigFile(value = "config", required = true)
public class MainConfigBase extends ConfigBase {

    @ConfigValue(value = "debug", defaultValue = "false")
    private boolean debug;

    @ConfigValue(value = "text.automatic-font", defaultValue = "small")
    private String textAutomaticFont;

    @ConfigValue(value = "text.force-in-upper-case", defaultValue = "true")
    private boolean textForceInUpperCase;

    // ===== CONFIGURACIONES DATE FORMATTER =====

    @ConfigValue(value = "date-formatter.default-pattern", defaultValue = "dd/MM/yyyy HH:mm:ss")
    private String dateFormatterDefaultPattern;

    @ConfigValue(value = "date-formatter.date-pattern", defaultValue = "dd/MM/yyyy")
    private String dateFormatterDatePattern;

    @ConfigValue(value = "date-formatter.time-pattern", defaultValue = "HH:mm:ss")
    private String dateFormatterTimePattern;

    @ConfigValue(value = "date-formatter.use-iso", defaultValue = "false")
    private boolean dateFormatterUseIso;

    // ===== CONFIGURACIONES TIME FORMATTER =====

    @ConfigValue(value = "time-formatter.zero-text", defaultValue = "0s")
    private String timeFormatterZeroText;

    @ConfigValue(value = "time-formatter.show-milliseconds", defaultValue = "true")
    private boolean timeFormatterShowMilliseconds;

    @ConfigValue(value = "time-formatter.precision", defaultValue = "1")
    private int timeFormatterPrecision;

    @ConfigValue(value = "time-formatter.language", defaultValue = "es")
    private String timeFormatterLanguage;

    @ConfigValue(value = "time-formatter.compact-mode", defaultValue = "false")
    private boolean timeFormatterCompactMode;

    // ===== GETTERS ESTÁTICOS =====

    public static boolean debug() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return false;
        return instance.debug;
    }

    public static String textAutomaticFont() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return "small";
        return instance.textAutomaticFont;
    }

    public static boolean textForceInUpperCase() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return false;
        return instance.textForceInUpperCase;
    }

    // ===== DATE FORMATTER GETTERS =====

    public static String dateFormatterDefaultPattern() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return "dd/MM/yyyy HH:mm:ss";
        return instance.dateFormatterDefaultPattern;
    }

    public static String dateFormatterDatePattern() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return "dd/MM/yyyy";
        return instance.dateFormatterDatePattern;
    }

    public static String dateFormatterTimePattern() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return "HH:mm:ss";
        return instance.dateFormatterTimePattern;
    }

    public static boolean dateFormatterUseIso() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return false;
        return instance.dateFormatterUseIso;
    }

    // ===== TIME FORMATTER GETTERS =====

    public static String timeFormatterZeroText() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return "0s";
        return instance.timeFormatterZeroText;
    }

    public static boolean timeFormatterShowMilliseconds() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return true;
        return instance.timeFormatterShowMilliseconds;
    }

    public static int timeFormatterPrecision() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return 1;
        return instance.timeFormatterPrecision;
    }

    public static String timeFormatterLanguage() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return "es";
        return instance.timeFormatterLanguage;
    }

    public static boolean timeFormatterCompactMode() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return false;
        return instance.timeFormatterCompactMode;
    }

    // ===== MÉTODOS INTERNOS =====

    /**
     * Obtiene la instancia activa de MainConfigBase o su extensión
     */
    private static MainConfigBase getActiveInstance() {
        try {
            for (Class<?> clazz : ConfigManager.getSystem().getConfigInstances().keySet()) {
                if (MainConfigBase.class.isAssignableFrom(clazz) && !clazz.equals(MainConfigBase.class)) {
                    return (MainConfigBase) ConfigManager.getSystem().getConfigInstances().get(clazz);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            return ConfigManager.get(MainConfigBase.class);
        } catch (Exception e) {
            return null;
        }
    }
}