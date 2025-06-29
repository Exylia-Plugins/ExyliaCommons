package net.exylia.commons.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Optional;

/**
 * Clase base abstracta para todas las configuraciones
 * Proporciona métodos de utilidad y manejo automático de valores
 */
public abstract class ConfigBase {

    protected ConfigurationSystem system;
    protected FileConfiguration config;
    protected String fileName;

    /**
     * Inicializa la configuración con el sistema y datos del archivo
     */
    public final void initialize(ConfigurationSystem system, ConfigurationSystem.ConfigFileData data) {
        this.system = system;
        this.config = data.configuration;
        this.fileName = extractFileName();

        // Cargar automáticamente campos anotados
        loadAnnotatedFields();

        // Llamar al método de inicialización personalizada
        onInitialize();
    }

    /**
     * Extrae el nombre del archivo desde la anotación
     */
    private String extractFileName() {
        ConfigFile annotation = this.getClass().getAnnotation(ConfigFile.class);
        return annotation != null ? annotation.value() : "unknown";
    }

    /**
     * Carga automáticamente campos marcados con @ConfigValue
     */
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
            throw new RuntimeException("Error cargando campos de configuración en " + getClass().getSimpleName(), e);
        }
    }

    /**
     * Obtiene el valor apropiado para un campo basándose en su tipo y anotación
     */
    private Object getValueForField(java.lang.reflect.Field field, ConfigValue annotation) {
        String path = annotation.value();
        Class<?> fieldType = field.getType();

        // Manejar diferentes tipos
        if (fieldType == String.class) {
            return getString(path, annotation.defaultValue());
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return getInt(path, parseIntDefault(annotation.defaultValue()));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return getBoolean(path, Boolean.parseBoolean(annotation.defaultValue()));
        } else if (fieldType == double.class || fieldType == Double.class) {
            return getDouble(path, parseDoubleDefault(annotation.defaultValue()));
        } else if (fieldType == List.class) {
            return getStringList(path);
        } else {
            throw new IllegalArgumentException("Tipo de campo no soportado: " + fieldType.getSimpleName());
        }
    }

    private int parseIntDefault(String value) {
        try {
            return value.isEmpty() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleDefault(String value) {
        try {
            return value.isEmpty() ? 0.0 : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ========== MÉTODOS DE ACCESO A CONFIGURACIÓN ==========

    protected String getString(String path) {
        return config.getString(path);
    }

    protected String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    protected int getInt(String path) {
        return config.getInt(path);
    }

    protected int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    protected boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    protected boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    protected double getDouble(String path) {
        return config.getDouble(path);
    }

    protected double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    protected List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    protected ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    protected Optional<String> getOptionalString(String path) {
        return Optional.ofNullable(config.getString(path));
    }

    protected Optional<ConfigurationSection> getOptionalSection(String path) {
        return Optional.ofNullable(config.getConfigurationSection(path));
    }

    // ========== MÉTODOS PARA SOBRESCRIBIR ==========

    /**
     * Llamado después de que se cargan los campos automáticamente
     * Usar para inicializaciones personalizadas
     */
    protected void onInitialize() {
        // Implementación por defecto vacía
    }

    /**
     * Llamado cuando se recarga la configuración
     * Usar para limpiar y recargar datos personalizados
     */
    protected void onReload() {
        // Recargar campos automáticamente
        loadAnnotatedFields();

        // Llamar a implementación personalizada
        onCustomReload();
    }

    /**
     * Implementación personalizada del reload
     */
    protected void onCustomReload() {
        // Implementación por defecto vacía
    }

    /**
     * Valida la configuración
     * @return true si la configuración es válida
     */
    public boolean validate() {
        return true; // Por defecto todas las configuraciones son válidas
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    /**
     * Obtiene el nombre del archivo de configuración
     */
    public final String getFileName() {
        return fileName;
    }

    /**
     * Obtiene el FileConfiguration subyacente
     */
    public final FileConfiguration getConfig() {
        return config;
    }

    /**
     * Verifica si existe una ruta en la configuración
     */
    protected boolean hasPath(String path) {
        return config.contains(path);
    }

    /**
     * Obtiene todas las claves de una sección
     */
    protected List<String> getKeys(String path) {
        ConfigurationSection section = getSection(path);
        return section != null ? List.copyOf(section.getKeys(false)) : List.of();
    }

    /**
     * Obtiene todas las claves de una sección de forma recursiva
     */
    protected List<String> getKeysDeep(String path) {
        ConfigurationSection section = getSection(path);
        return section != null ? List.copyOf(section.getKeys(true)) : List.of();
    }
}