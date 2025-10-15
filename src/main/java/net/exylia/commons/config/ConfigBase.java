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

@Deprecated
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
             
            loadFieldsFromClass(this.getClass());

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

                if (isConfigComponent(field.getType())) {
                    logInternalDebug("Campo de componente recargado: " + field.getName() + " = " + value);
                }
            }
        }
    }

    private boolean isConfigComponent(Class<?> type) {
        return type == BossBarConfig.class ||
                type == TitleConfig.class ||
                type == ActionBarConfig.class ||
                type == ScoreboardConfig.class;
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

    private Map<String, Object> createMapFromConfig(java.lang.reflect.Field field, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            logInternalError("Advertencia: No se encontró configuración para " + path + ", usando mapa vacío");
            return new HashMap<>();
        }

        Map<String, Object> resultMap = new HashMap<>();

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] actualTypes = paramType.getActualTypeArguments();

            if (actualTypes.length >= 2) {
                Type valueTypeGeneric = actualTypes[1];

                Class<?> valueType = null;

                if (valueTypeGeneric instanceof Class<?>) {
                     
                    valueType = (Class<?>) valueTypeGeneric;
                } else if (valueTypeGeneric instanceof ParameterizedType paramValueType) {
                     
                    valueType = (Class<?>) paramValueType.getRawType();
                } else {
                     
                    logInternalError("Tipo de valor no soportado para el campo " + field.getName() + ": " + valueTypeGeneric);
                    return resultMap;
                }

                for (String key : section.getKeys(false)) {
                    Object value = createValueFromSection(section, key, valueType);
                    resultMap.put(key, value);
                }
            }
        } else {
             
            for (String key : section.getKeys(false)) {
                resultMap.put(key, section.get(key));
            }
        }

        return resultMap;
    }

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
            String fullPath = parentSection.getCurrentPath() + "." + key;
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new BossBarConfig(fullPath, config);
            }
            return new BossBarConfig();
        } else if (valueType == TitleConfig.class) {
            String fullPath = parentSection.getCurrentPath() + "." + key;
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new TitleConfig(fullPath, config);
            }
            return new TitleConfig();
        } else if (valueType == ActionBarConfig.class) {
            String fullPath = parentSection.getCurrentPath() + "." + key;
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new ActionBarConfig(fullPath, config);
            }
            return new ActionBarConfig();
        }else if (valueType == ScoreboardConfig.class) {
            String fullPath = parentSection.getCurrentPath() + "." + key;
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return new ScoreboardConfig(fullPath, config);
            }
            return new ScoreboardConfig();
        }else {
            ConfigurationSection subSection = parentSection.getConfigurationSection(key);
            if (subSection != null) {
                return createComplexObjectFromSection(subSection, valueType);
            } else {
                 
                return parentSection.get(key);
            }
        }
    }

    private Object createComplexObjectFromSection(ConfigurationSection section, Class<?> objectType) {
        try {
             
            Object instance = objectType.getDeclaredConstructor().newInstance();

            for (java.lang.reflect.Field field : objectType.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (section.contains(fieldName)) {
                    Object value = getValueFromSection(section, fieldName, field.getType());
                    field.set(instance, value);
                } else if (section.contains(camelToSnake(fieldName))) {
                     
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
            String fullPath = section.getCurrentPath() + "." + key;
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new BossBarConfig(fullPath, config);
            }
            return new BossBarConfig();
        } else if (expectedType == TitleConfig.class) {
            String fullPath = section.getCurrentPath() + "." + key;
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new TitleConfig(fullPath, config);
            }
            return new TitleConfig();
        } else if (expectedType == ActionBarConfig.class) {
            String fullPath = section.getCurrentPath() + "." + key;
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new ActionBarConfig(fullPath, config);
            }
            return new ActionBarConfig();
        }else if (expectedType == ScoreboardConfig.class) {
            String fullPath = section.getCurrentPath() + "." + key;
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return new ScoreboardConfig(fullPath, config);
            }
            return new ScoreboardConfig();
        }else {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
                return createComplexObjectFromSection(subSection, expectedType);
            }
            return section.get(key);
        }
    }

    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private BossBarConfig createBossBarConfig(String basePath) {
        ConfigurationSection section = config.getConfigurationSection(basePath);
        if (section != null) {
            logInternalDebug("Creando BossBarConfig desde sección: " + basePath);
            return new BossBarConfig(basePath, config);
        } else {
            logInternalDebug("Creando BossBarConfig por defecto para: " + basePath);
            return new BossBarConfig();
        }
    }

    private TitleConfig createTitleConfig(String basePath) {
        ConfigurationSection section = config.getConfigurationSection(basePath);
        if (section != null) {
            logInternalDebug("Creando TitleConfig desde sección: " + basePath);
            return new TitleConfig(basePath, config);
        } else {
            logInternalDebug("Creando TitleConfig por defecto para: " + basePath);
            return new TitleConfig();
        }
    }

    private ActionBarConfig createActionBarConfig(String basePath) {
        ConfigurationSection section = config.getConfigurationSection(basePath);
        if (section != null) {
            logInternalDebug("Creando ActionBarConfig desde sección: " + basePath);
            return new ActionBarConfig(basePath, config);
        } else {
            logInternalDebug("Creando ActionBarConfig por defecto para: " + basePath);
            return new ActionBarConfig();
        }
    }

    private ScoreboardConfig createScoreboardConfig(String basePath) {
        ConfigurationSection section = config.getConfigurationSection(basePath);
        if (section != null) {
            logInternalDebug("Creando ScoreboardConfig desde sección: " + basePath);
            return new ScoreboardConfig(basePath, config);
        } else {
            logInternalDebug("Creando ScoreboardConfig por defecto para: " + basePath);
            return new ScoreboardConfig();
        }
    }

    protected void setValue(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }

    protected void setValueNoSave(String path, Object value) {
        config.set(path, value);
    }

    protected void saveConfig() {
        try {
            File configFile = new File(system.getPlugin().getDataFolder(), fileName + ".yml");
            config.save(configFile);

            ConfigurationSystem.ConfigFileData data = system.getFileData(fileName);
            if (data != null) {
                data.lastModified = configFile.lastModified();
            }

            logInternalDebug("Configuración guardada: " + fileName);
        } catch (Exception e) {
            logInternalError("Error guardando configuración " + fileName + ": " + e.getMessage());
            throw new RuntimeException("Error guardando configuración", e);
        }
    }

    protected void setValues(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        saveConfig();
    }

    protected void reloadField(String fieldName) {
        try {
            java.lang.reflect.Field field = findFieldByName(fieldName);
            if (field != null) {
                ConfigValue annotation = field.getAnnotation(ConfigValue.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    Object value = getValueForField(field, annotation);
                    field.set(this, value);
                    logInternalDebug("Campo recargado: " + fieldName + " = " + value);
                }
            }
        } catch (Exception e) {
            logInternalError("Error recargando campo " + fieldName + ": " + e.getMessage());
        }
    }

    protected void reloadAllFields() {
        try {
            logInternalDebug("Recargando todos los campos de configuración para: " + fileName);
            loadAnnotatedFields();
            logInternalDebug("Todos los campos recargados correctamente para: " + fileName);
        } catch (Exception e) {
            logInternalError("Error recargando todos los campos para " + fileName + ": " + e.getMessage());
        }
    }

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
         
        logInternalDebug("Ejecutando onReload para: " + fileName);
        loadAnnotatedFields();  
        onCustomReload();
        logInternalDebug("onReload completado para: " + fileName);
    }

    protected void onInitialize() {}
    protected void onCustomReload() {}
    public boolean validate() { return true; }

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
