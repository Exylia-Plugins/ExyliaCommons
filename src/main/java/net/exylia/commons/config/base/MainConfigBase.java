package net.exylia.commons.config.base;

import net.exylia.commons.config.ConfigBase;
import net.exylia.commons.config.ConfigFile;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.config.ConfigValue;

@ConfigFile(value = "config", required = true)
public class MainConfigBase extends ConfigBase {

    @ConfigValue(value = "debug", defaultValue = "false")
    public static boolean debug() {
        return ConfigManager.getBooleanFromMethodAuto();
    }

    @ConfigValue(value = "text.automatic-font", defaultValue = "small")
    public static String textAutomaticFont() {
        return ConfigManager.getStringFromMethodAuto();
    }

    @ConfigValue(value = "text.force-in-upper-case", defaultValue = "true")
    public static boolean textForceInUpperCase() {
        return ConfigManager.getBooleanFromMethodAuto();
    }

    @ConfigValue(value = "date-formatter.default-pattern", defaultValue = "dd/MM/yyyy HH:mm:ss")
    public static String dateFormatterDefaultPattern() {
        return ConfigManager.getStringFromMethodAuto();
    }

    @ConfigValue(value = "date-formatter.date-pattern", defaultValue = "dd/MM/yyyy")
    public static String dateFormatterDatePattern() {
        return ConfigManager.getStringFromMethodAuto();
    }

    @ConfigValue(value = "date-formatter.time-pattern", defaultValue = "HH:mm:ss")
    public static String dateFormatterTimePattern() {
        return ConfigManager.getStringFromMethodAuto();
    }

    @ConfigValue(value = "date-formatter.use-iso", defaultValue = "false")
    public static boolean dateFormatterUseIso() {
        return ConfigManager.getBooleanFromMethodAuto();
    }

    @ConfigValue(value = "time-formatter.zero-text", defaultValue = "0s")
    public static String timeFormatterZeroText() {
        return ConfigManager.getStringFromMethodAuto();
    }

    @ConfigValue(value = "time-formatter.show-milliseconds", defaultValue = "true")
    public static boolean timeFormatterShowMilliseconds() {
        return ConfigManager.getBooleanFromMethodAuto();
    }

    @ConfigValue(value = "time-formatter.precision", defaultValue = "1")
    public static int timeFormatterPrecision() {
        return ConfigManager.getIntFromMethodAuto();
    }

    @ConfigValue(value = "time-formatter.language", defaultValue = "es")
    public static String timeFormatterLanguage() {
        return ConfigManager.getStringFromMethodAuto();
    }

    @ConfigValue(value = "time-formatter.compact-mode", defaultValue = "false")
    public static boolean timeFormatterCompactMode() {
        return ConfigManager.getBooleanFromMethodAuto();
    }

    @ConfigValue(value = "time-formatter.force-show-zero-decimals", defaultValue = "false")
    public static boolean timeFormatterForceShowZeroDecimals() {
        return ConfigManager.getBooleanFromMethodAuto();
    }
}