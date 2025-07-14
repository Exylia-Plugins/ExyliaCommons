package net.exylia.commons.config;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static ConfigurationSystem internalSystem;
    private static final Map<Class<?>, Object> staticConfigs = new ConcurrentHashMap<>();
    private static Class<? extends ConfigBase>[] configClasses;

    public static void init(ConfigurationSystem existingSystem, Class<? extends ConfigBase>... configClasses) {
        ConfigManager.configClasses = configClasses;
        internalSystem = existingSystem; // Usar el sistema existente en lugar de crear uno nuevo

        // Registrar configs para acceso estático usando el sistema existente
        for (Class<? extends ConfigBase> configClass : configClasses) {
            staticConfigs.put(configClass, internalSystem.getConfig(configClass));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ConfigBase> T get(Class<T> configClass) {
        return (T) staticConfigs.get(configClass);
    }

    // ===== ACCESO DIRECTO A ARCHIVOS =====
    public static FileConfiguration getFile(String fileName) {
        return internalSystem.getFile(fileName);
    }

    public static FileConfiguration getFile(Class<? extends ConfigBase> configClass) {
        ConfigBase config = get(configClass);
        return config != null ? config.getConfig() : null;
    }

    public static ConfigurationSystem getSystem() {
        return internalSystem;
    }

    public static CompletableFuture<Boolean> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Recargar el sistema interno
                boolean systemReloadSuccess = internalSystem.reloadAllAsync().join();
                if (!systemReloadSuccess) {
                    DebugUtils.logInternalError("ERROR: Fallo en reload del sistema interno");
                    return false;
                }

                // 2. CRÍTICO: Recrear todas las instancias de configuración
                staticConfigs.clear();

                // 3. Reinicializar cada clase de configuración usando el sistema existente
                for (Class<? extends ConfigBase> configClass : configClasses) {
                    try {
                        // Obtener la instancia ya recargada del sistema
                        Object reloadedInstance = internalSystem.getConfig(configClass);
                        if (reloadedInstance != null) {
                            staticConfigs.put(configClass, reloadedInstance);
                        } else {
                            DebugUtils.logInternalError("ERROR: No se pudo obtener instancia recargada para: " + configClass.getSimpleName());
                            return false;
                        }
                    } catch (Exception e) {
                        DebugUtils.logInternalError("ERROR: Fallo recargando " + configClass.getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }

                return true;

            } catch (Exception e) {
                DebugUtils.logInternalError("ERROR: Fallo crítico en reload de ConfigManager: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
}