package net.exylia.commons.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static ConfigurationSystem internalSystem;
    private static final Map<Class<?>, Object> staticConfigs = new ConcurrentHashMap<>();

    public static void init(JavaPlugin plugin, Class<? extends ConfigBase>... configClasses) {
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
        return config != null ? config.config : null;
    }

    public static ConfigurationSystem getSystem() {
        return internalSystem;
    }

    public static CompletableFuture<Boolean> reloadAllAsync() {
        return internalSystem.reloadAllAsync().thenApply(success -> {
            if (success) {
                // Actualizar referencias estáticas
                for (Map.Entry<Class<?>, Object> entry : staticConfigs.entrySet()) {
                    staticConfigs.put(entry.getKey(), internalSystem.getConfig((Class<? extends ConfigBase>) entry.getKey()));
                }
            }
            return success;
        });
    }
}