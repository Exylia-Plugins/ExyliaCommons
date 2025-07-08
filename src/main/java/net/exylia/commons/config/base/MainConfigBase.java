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

    @ConfigValue(value = "time.format", defaultValue = "HUMAN_READABLE")
    private String timeFormat;

    @ConfigValue(value = "time.show_zero_values", defaultValue = "false")
    private boolean showZeroValues;

    // ===== GETTERS ESTÁTICOS =====

    public static boolean debug() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return false;
        return instance.debug;
    }

    public static String timeFormat() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return null;
        return instance.timeFormat;
    }

    public static boolean timeShowZeroValues() {
        MainConfigBase instance = getActiveInstance();
        if (instance == null) return false;
        return instance.showZeroValues;
    }

    // ===== MÉTODOS INTERNOS =====

    /**
     * Obtiene la instancia activa de MainConfigBase o su extensión
     */
    private static MainConfigBase getActiveInstance() {
        // Primero intentar obtener la extensión del plugin actual
        try {
            // Si hay una extensión registrada, usarla
            for (Class<?> clazz : ConfigManager.getSystem().getConfigInstances().keySet()) {
                if (MainConfigBase.class.isAssignableFrom(clazz) && !clazz.equals(MainConfigBase.class)) {
                    return (MainConfigBase) ConfigManager.getSystem().getConfigInstances().get(clazz);
                }
            }
        } catch (Exception e) {
            // Si falla, usar la instancia base
        }

        // Fallback a la instancia base
        try {
            return ConfigManager.get(MainConfigBase.class);
        } catch (Exception e) {
            return null;
        }
    }
}