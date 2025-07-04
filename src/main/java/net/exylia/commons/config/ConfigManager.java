package net.exylia.commons.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static ConfigurationSystem internalSystem;
    private static final Map<Class<?>, Object> staticConfigs = new ConcurrentHashMap<>();
    private static Class<? extends ConfigBase>[] configClasses; // NUEVO: Guardar referencia

    public static void init(JavaPlugin plugin, Class<? extends ConfigBase>... configClasses) {
        ConfigManager.configClasses = configClasses; // NUEVO: Guardar para reloads
        internalSystem = new ConfigurationSystem(plugin);
        internalSystem.initialize(configClasses);

        // Registrar configs para acceso estático
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
        return config != null ? config.getConfig() : null; // CAMBIADO: usar getConfig()
    }

    public static ConfigurationSystem getSystem() {
        return internalSystem;
    }

    public static CompletableFuture<Boolean> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("DEBUG: Iniciando reload de ConfigManager...");

                // 1. Recargar el sistema interno
                boolean systemReloadSuccess = internalSystem.reloadAllAsync().join();
                if (!systemReloadSuccess) {
                    System.err.println("ERROR: Fallo en reload del sistema interno");
                    return false;
                }

                // 2. CRÍTICO: Recrear todas las instancias de configuración
                staticConfigs.clear();

                // 3. Reinicializar cada clase de configuración
                for (Class<? extends ConfigBase> configClass : configClasses) {
                    try {
                        System.out.println("DEBUG: Recargando clase: " + configClass.getSimpleName());

                        // Crear nueva instancia
                        ConfigBase newInstance = configClass.getDeclaredConstructor().newInstance();

                        // Obtener datos del archivo
                        ConfigFile annotation = configClass.getAnnotation(ConfigFile.class);
                        if (annotation != null) {
                            String fileName = annotation.value();
                            ConfigurationSystem.ConfigFileData fileData = internalSystem.getFileData(fileName);

                            if (fileData != null) {
                                // Reinicializar la instancia con datos actualizados
                                newInstance.initialize(internalSystem, fileData);
                                staticConfigs.put(configClass, newInstance);
                                System.out.println("DEBUG: Clase recargada exitosamente: " + configClass.getSimpleName());
                            } else {
                                System.err.println("ERROR: No se encontraron datos para: " + fileName);
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR: Fallo recargando " + configClass.getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }

                System.out.println("DEBUG: Reload de ConfigManager completado exitosamente");
                return true;

            } catch (Exception e) {
                System.err.println("ERROR: Fallo crítico en reload de ConfigManager: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
}