package net.exylia.commons.config;

import net.exylia.commons.config.components.BossBarConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        } else if (fieldType == Map.class) {
            return createMapFromConfig(field, path);
        } else {
            throw new IllegalArgumentException("Tipo de campo no soportado: " + fieldType.getSimpleName());
        }
    }

    /**
     * Creates a Map from a configuration section
     */
    private Map<String, Object> createMapFromConfig(java.lang.reflect.Field field, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            logError("Advertencia: No se encontró configuración para " + path + ", usando mapa vacío");
            return new HashMap<>();
        }

        Map<String, Object> resultMap = new HashMap<>();

        // Get the generic type information
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length >= 2) {
                Class<?> valueType = (Class<?>) actualTypes[1];

                // Handle different value types
                for (String key : section.getKeys(false)) {
                    Object value = createValueFromSection(section, key, valueType);
                    resultMap.put(key, value);
                }
            }
        }

        return resultMap;
    }

    /**
     * Creates a value from a configuration section based on the expected type
     */
    private Object createValueFromSection(ConfigurationSection parentSection, String key, Class<?> valueType) {
        if (valueType == String.class) {
            return parentSection.getString(key);
        } else if (valueType == Integer.class || valueType == int.class) {
            return parentSection.getInt(key);
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return parentSection.getBoolean(key);
        } else if (valueType == Double.class || valueType == double.class) {
            return parentSection.getDouble(key);
        } else {
            // For complex objects like RankTier, try to create from configuration section
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return createComplexObjectFromSection(subSection, valueType);
            } else {
                // If it's not a section, return the raw value
                return parentSection.get(key);
            }
        }
    }

    /**
     * Creates complex objects (like RankTier) from configuration sections
     */
    private Object createComplexObjectFromSection(ConfigurationSection section, Class<?> objectType) {
        try {
            // Try to create an instance of the object
            Object instance = objectType.getDeclaredConstructor().newInstance();

            // Use reflection to set fields based on configuration
            for (java.lang.reflect.Field field : objectType.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (section.contains(fieldName)) {
                    Object value = getValueFromSection(section, fieldName, field.getType());
                    field.set(instance, value);
                } else if (section.contains(camelToSnake(fieldName))) {
                    // Try snake_case version
                    Object value = getValueFromSection(section, camelToSnake(fieldName), field.getType());
                    field.set(instance, value);
                }
            }

            return instance;
        } catch (Exception e) {
            logError("Error creando objeto " + objectType.getSimpleName() + " desde configuración: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a value from a configuration section with proper type conversion
     */
    private Object getValueFromSection(ConfigurationSection section, String key, Class<?> expectedType) {
        if (expectedType == String.class) {
            return section.getString(key);
        } else if (expectedType == int.class || expectedType == Integer.class) {
            return section.getInt(key);
        } else if (expectedType == boolean.class || expectedType == Boolean.class) {
            return section.getBoolean(key);
        } else if (expectedType == double.class || expectedType == Double.class) {
            return section.getDouble(key);
        } else {
            // For nested objects, recursively create from subsection
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return createComplexObjectFromSection(subSection, expectedType);
            }
            return section.get(key);
        }
    }

    /**
     * Converts camelCase to snake_case
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
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