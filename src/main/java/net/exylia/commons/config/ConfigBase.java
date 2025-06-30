package net.exylia.commons.config;

import net.exylia.commons.config.components.BossBarConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

import static net.exylia.commons.utils.DebugUtils.logError;

/**
 * Clase base abstracta para todas las configuraciones
 * Proporciona métodos de utilidad y manejo automático de valores
 */
public abstract class ConfigBase {
    protected ConfigurationSystem system;
    protected FileConfiguration config;
    protected String fileName;

    public final void initialize(ConfigurationSystem system, ConfigurationSystem.ConfigFileData data) {
        this.system = system;
        this.config = data.configuration;
        this.fileName = extractFileName();
        loadAnnotatedFields();
        onInitialize();
    }

    private String extractFileName() {
        ConfigFile annotation = this.getClass().getAnnotation(ConfigFile.class);
        return annotation != null ? annotation.value() : "unknown";
    }

    private void loadAnnotatedFields() {
        try {
            for (java.lang.reflect.Field field : this.getClass().getDeclaredFields()) {
                ConfigValue annotation = field.getAnnotation(ConfigValue.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    Object value = getValueForField(field, annotation);
                    field.set(this, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando campos de configuración", e);
        }
    }

    private Object getValueForField(java.lang.reflect.Field field, ConfigValue annotation) {
        String path = annotation.value();
        Class<?> fieldType = field.getType();

        if (fieldType == String.class) {
            return config.getString(path, annotation.defaultValue());
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return config.getInt(path, parseIntDefault(annotation.defaultValue()));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return config.getBoolean(path, Boolean.parseBoolean(annotation.defaultValue()));
        } else if (fieldType == double.class || fieldType == Double.class) {
            return config.getDouble(path, parseDoubleDefault(annotation.defaultValue()));
        } else if (fieldType == List.class) {
            return config.getStringList(path);
        } else if (fieldType == BossBarConfig.class) {
            return createBossBarConfig(path);
        } else {
            throw new IllegalArgumentException("Tipo de campo no soportado: " + fieldType.getSimpleName());
        }
    }

    private BossBarConfig createBossBarConfig(String basePath) {
        if (config.getConfigurationSection(basePath) != null) {
            return new BossBarConfig(basePath, config);
        } else {
            logError("Advertencia: No se encontró configuración para " + basePath + ", usando valores por defecto");
            return new BossBarConfig();
        }
    }

    private int parseIntDefault(String value) {
        try { return value.isEmpty() ? 0 : Integer.parseInt(value); }
        catch (NumberFormatException e) { return 0; }
    }

    private double parseDoubleDefault(String value) {
        try { return value.isEmpty() ? 0.0 : Double.parseDouble(value); }
        catch (NumberFormatException e) { return 0.0; }
    }

    public final void onReload() {
        loadAnnotatedFields();
        onCustomReload();
    }

    // Métodos que pueden ser sobrescritos por las clases hijas
    protected void onInitialize() {}
    protected void onCustomReload() {}
    public boolean validate() { return true; }

    // Métodos de utilidad para acceder al sistema y configuración
    protected ConfigurationSystem getSystem() {
        return system;
    }

    protected FileConfiguration getConfig() {
        return config;
    }

    protected String getFileName() {
        return fileName;
    }
}