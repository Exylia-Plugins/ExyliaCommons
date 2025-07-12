package net.exylia.commons.config;

import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.BossBarConfig;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.config.components.TitleConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalError;

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
            // Cargar campos de todas las clases en la jerarquía
            loadFieldsFromClass(this.getClass());

            // Cargar campos de clases padre hasta llegar a ConfigBase
            Class<?> currentClass = this.getClass().getSuperclass();
            while (currentClass != null && ConfigBase.class.isAssignableFrom(currentClass) && !currentClass.equals(ConfigBase.class)) {
                loadFieldsFromClass(currentClass);
                currentClass = currentClass.getSuperclass();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error cargando campos de configuración", e);
        }
    }

    private void loadFieldsFromClass(Class<?> clazz) throws IllegalAccessException {
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            ConfigValue annotation = field.getAnnotation(ConfigValue.class);
            if (annotation != null) {
                field.setAccessible(true);
                Object value = getValueForField(field, annotation);
                field.set(this, value);
            }
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
            boolean defaultVal = !annotation.defaultValue().isEmpty() && Boolean.parseBoolean(annotation.defaultValue());
            return config.getBoolean(path, defaultVal);
        } else if (fieldType == double.class || fieldType == Double.class) {
            return config.getDouble(path, parseDoubleDefault(annotation.defaultValue()));
        } else if (fieldType == long.class || fieldType == Long.class) {
            return config.getLong(path, parseLongDefault(annotation.defaultValue()));
        } else if (fieldType == List.class) {
            return config.getStringList(path);
        } else if (fieldType == BossBarConfig.class) {
            return createBossBarConfig(path);
        } else if (fieldType == TitleConfig.class) {
            return createTitleConfig(path);
        } else if (fieldType == ActionBarConfig.class) {
            return createActionBarConfig(path);
        } else if (fieldType == ScoreboardConfig.class) {
            return createScoreboardConfig(path);
        }else if (fieldType == Map.class) {
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
            logInternalError("Advertencia: No se encontró configuración para " + path + ", usando mapa vacío");
            return new HashMap<>();
        }

        Map<String, Object> resultMap = new HashMap<>();

        // Get the generic type information
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length >= 2) {
                Type valueTypeGeneric = actualTypes[1];

                // Manejar el tipo del valor de manera segura
                Class<?> valueType = null;

                if (valueTypeGeneric instanceof Class<?>) {
                    // Caso simple: el tipo es una clase directa
                    valueType = (Class<?>) valueTypeGeneric;
                } else if (valueTypeGeneric instanceof ParameterizedType paramValueType) {
                    // Caso complejo: el tipo es genérico (ej: List<String>)
                    valueType = (Class<?>) paramValueType.getRawType();
                } else {
                    // Fallback: intentar obtener el tipo raw
                    logInternalError("Tipo de valor no soportado para el campo " + field.getName() + ": " + valueTypeGeneric);
                    return resultMap;
                }

                // Handle different value types
                for (String key : section.getKeys(false)) {
                    Object value = createValueFromSection(section, key, valueType);
                    resultMap.put(key, value);
                }
            }
        } else {
            // Si no es un tipo parametrizado, intentar crear un mapa básico
            for (String key : section.getKeys(false)) {
                resultMap.put(key, section.get(key));
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
        } else if (valueType == Long.class || valueType == long.class) {
            return parentSection.getLong(key);
        } else if (valueType == BossBarConfig.class) {
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new BossBarConfig(key, config);
            }
            return new BossBarConfig();
        } else if (valueType == TitleConfig.class) {
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new TitleConfig(key, config);
            }
            return new TitleConfig();
        } else if (valueType == ActionBarConfig.class) {
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new ActionBarConfig(key, config);
            }
            return new ActionBarConfig();
        }else if (valueType == ScoreboardConfig.class) {
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new ScoreboardConfig(key, config);
            }
            return new ScoreboardConfig();
        }else {
            // For complex objects, try to create from configuration section
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
            logInternalError("Error creando objeto " + objectType.getSimpleName() + " desde configuración: " + e.getMessage());
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
        } else if (expectedType == long.class || expectedType == Long.class) {
            return section.getLong(key);
        } else if (expectedType == BossBarConfig.class) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new BossBarConfig(key, config);
            }
            return new BossBarConfig();
        } else if (expectedType == TitleConfig.class) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new TitleConfig(key, config);
            }
            return new TitleConfig();
        } else if (expectedType == ActionBarConfig.class) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new ActionBarConfig(key, config);
            }
            return new ActionBarConfig();
        }else if (expectedType == ScoreboardConfig.class) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new ScoreboardConfig(key, config);
            }
            return new ScoreboardConfig();
        }else {
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
            logInternalError("Advertencia: No se encontró configuración para BossBar en " + basePath + ", usando valores por defecto");
            return new BossBarConfig();
        }
    }

    private TitleConfig createTitleConfig(String basePath) {
        if (config.getConfigurationSection(basePath) != null) {
            return new TitleConfig(basePath, config);
        } else {
            logInternalError("Advertencia: No se encontró configuración para Title en " + basePath + ", usando valores por defecto");
            return new TitleConfig();
        }
    }

    private ActionBarConfig createActionBarConfig(String basePath) {
        if (config.getConfigurationSection(basePath) != null) {
            return new ActionBarConfig(basePath, config);
        } else {
            logInternalError("Advertencia: No se encontró configuración para ActionBar en " + basePath + ", usando valores por defecto");
            return new ActionBarConfig();
        }
    }

    private ScoreboardConfig createScoreboardConfig(String basePath) {
        if (config.getConfigurationSection(basePath) != null) {
            return new ScoreboardConfig(basePath, config);
        } else {
            logInternalError("Advertencia: No se encontró configuración para Scoreboard en " + basePath + ", usando valores por defecto");
            return new ScoreboardConfig();
        }
    }

    // ==================== MÉTODOS DE ESCRITURA PARA ConfigBase ====================

    /**
     * Establece un valor en la configuración y lo guarda
     */
    protected void setValue(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }

    /**
     * Establece un valor en la configuración sin guardar automáticamente
     */
    protected void setValueNoSave(String path, Object value) {
        config.set(path, value);
    }

    /**
     * Guarda la configuración actual al archivo
     */
    protected void saveConfig() {
        try {
            File configFile = new File(system.getPlugin().getDataFolder(), fileName + ".yml");
            config.save(configFile);

            // Actualizar el timestamp en el sistema
            ConfigurationSystem.ConfigFileData data = system.getFileData(fileName);
            if (data != null) {
                data.lastModified = configFile.lastModified();
            }

            logInternalDebug(true, "Configuración guardada: " + fileName);
        } catch (Exception e) {
            logInternalError("Error guardando configuración " + fileName + ": " + e.getMessage());
            throw new RuntimeException("Error guardando configuración", e);
        }
    }

    /**
     * Establece múltiples valores y guarda una sola vez
     */
    protected void setValues(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        saveConfig();
    }

    /**
     * Recarga un campo específico después de cambiar su valor
     */
    protected void reloadField(String fieldName) {
        try {
            java.lang.reflect.Field field = findFieldByName(fieldName);
            if (field != null) {
                ConfigValue annotation = field.getAnnotation(ConfigValue.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    Object value = getValueForField(field, annotation);
                    field.set(this, value);
                }
            }
        } catch (Exception e) {
            logInternalError("Error recargando campo " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * Busca un campo por nombre en la jerarquía de clases
     */
    private java.lang.reflect.Field findFieldByName(String fieldName) {
        Class<?> currentClass = this.getClass();
        while (currentClass != null && ConfigBase.class.isAssignableFrom(currentClass)) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }


    private int parseIntDefault(String value) {
        try { return value.isEmpty() ? 0 : Integer.parseInt(value); }
        catch (NumberFormatException e) { return 0; }
    }

    private double parseDoubleDefault(String value) {
        try { return value.isEmpty() ? 0.0 : Double.parseDouble(value); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private long parseLongDefault(String value) {
        try { return value.isEmpty() ? 0L : Long.parseLong(value); }
        catch (NumberFormatException e) { return 0L; }
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