package net.exylia.commons.v2.config.schema;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.scoreboard.config.serializer.ScoreboardSerializer;
import net.exylia.commons.v2.visual.config.serializer.ActionBarConfigSerializer;
import net.exylia.commons.v2.visual.config.serializer.BossBarConfigSerializer;
import net.exylia.commons.v2.visual.config.serializer.HologramTemplateSerializer;
import net.exylia.commons.v2.visual.config.serializer.ScoreboardConfigSerializer;
import net.exylia.commons.v2.visual.config.serializer.TitleConfigSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigSchemaRegistry {

    private static final Map<String, Class<?>> registeredSchemas = new HashMap<>();
    private static final Map<Class<?>, ConfigSerializer<?>> serializers = new HashMap<>();

    static {
        registerSerializer(new BossBarConfigSerializer());
        registerSerializer(new ActionBarConfigSerializer());
        registerSerializer(new TitleConfigSerializer());
        registerSerializer(new ScoreboardConfigSerializer());
        registerSerializer(new ScoreboardSerializer());
        registerSerializer(new HologramTemplateSerializer());
    }

    public static <T> void registerSerializer(ConfigSerializer<T> serializer) {
        serializers.put(serializer.getType(), serializer);
    }

    @SuppressWarnings("unchecked")
    private static <T> ConfigSerializer<T> getSerializer(Class<T> type) {
        return (ConfigSerializer<T>) serializers.get(type);
    }

    public static void ensureDefaults(Class<?> schemaClass) {
        if (!schemaClass.isAnnotationPresent(ConfigSchema.class)) {
            throw new IllegalArgumentException("Class " + schemaClass.getName() + " is not annotated with @ConfigSchema");
        }

        ConfigSchema schema = schemaClass.getAnnotation(ConfigSchema.class);
        String fileName = schema.file();

        registeredSchemas.put(fileName, schemaClass);

        Config config = Configs.file(fileName);
        config.reload();

        DebugUtils.logInternalDebug("[ConfigSchema] ensureDefaults for " + fileName + ".yml");

        processClass(schemaClass, "", config);

        config.save();

        loadClass(schemaClass, "", config);
    }

    public static void load(Class<?> schemaClass) {
        if (!schemaClass.isAnnotationPresent(ConfigSchema.class)) {
            throw new IllegalArgumentException("Class " + schemaClass.getName() + " is not annotated with @ConfigSchema");
        }

        ConfigSchema schema = schemaClass.getAnnotation(ConfigSchema.class);
        String fileName = schema.file();

        Config config = Configs.file(fileName);
        config.reload();

        loadClass(schemaClass, "", config);
    }

    private static void loadClass(Class<?> clazz, String prefix, Config config) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            if (field.isAnnotationPresent(ConfigValue.class)) {
                loadField(field, prefix, config);
            }
        }

        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(ConfigSection.class)) {
                ConfigSection section = innerClass.getAnnotation(ConfigSection.class);
                String sectionPath = prefix.isEmpty() ? section.value() : prefix + "." + section.value();
                loadClass(innerClass, sectionPath, config);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void loadField(Field field, String prefix, Config config) {
        ConfigValue configValue = field.getAnnotation(ConfigValue.class);
        String path = prefix.isEmpty() ? configValue.value() : prefix + "." + configValue.value();

        try {
            field.setAccessible(true);
            Object defaultValue = field.get(null);

            ConfigSerializer serializer = getSerializer(field.getType());

            if (serializer != null) {
                ConfigurationSection section = resolveSerializedSection(config, path);

                if (section == null && !prefix.isEmpty()) {
                    String legacyPath = configValue.value();
                    section = resolveSerializedSection(config, legacyPath);
                    if (section != null) {
                        DebugUtils.logInternalDebug("[ConfigSchema] Loading (serialized) " + path + " from legacy path " + legacyPath);
                    }
                }

                Object value = section != null ? serializer.deserialize(section) : defaultValue;
                DebugUtils.logInternalDebug("[ConfigSchema] Loading (serialized) " + path + " | found=" + (section != null));
                if (value != null) {
                    field.set(null, value);
                }
            } else {
                Object rawValue = config.raw().get(path);
                Object value = rawValue != null ? rawValue : defaultValue;
                DebugUtils.logInternalDebug("[ConfigSchema] Loading " + path + " | raw=" + rawValue + " | default=" + defaultValue + " | final=" + value);
                if (value != null) {
                    field.set(null, value);
                }
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to load field: " + field.getName(), e);
        }
    }

    private static void processClass(Class<?> clazz, String prefix, Config config) {
        processClassComments(clazz, prefix, config);

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            if (field.isAnnotationPresent(ConfigValue.class)) {
                processField(field, prefix, config);
            }
        }

        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(ConfigSection.class)) {
                ConfigSection section = innerClass.getAnnotation(ConfigSection.class);
                String sectionPath = prefix.isEmpty() ? section.value() : prefix + "." + section.value();
                processClass(innerClass, sectionPath, config);
            }
        }
    }

    private static void processClassComments(Class<?> clazz, String prefix, Config config) {
        if (clazz.isAnnotationPresent(Comment.class)) {
            Comment comment = clazz.getAnnotation(Comment.class);
            setComments(config, prefix, comment.value());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void processField(Field field, String prefix, Config config) {
        ConfigValue configValue = field.getAnnotation(ConfigValue.class);
        String path = prefix.isEmpty() ? configValue.value() : prefix + "." + configValue.value();

        try {
            field.setAccessible(true);
            Object value = field.get(null);

            ConfigSerializer serializer = getSerializer(field.getType());
            if (serializer != null) {
                Object serialized = serializer.serialize(value);
                if (!config.exists(path)) {
                    config.set(path, serialized);
                } else {
                    mergeSerializedDefaults(config, path, serialized);
                }
            } else if (!config.exists(path)) {
                config.set(path, value);
            }

            if (field.isAnnotationPresent(Comment.class)) {
                Comment comment = field.getAnnotation(Comment.class);
                setComments(config, path, comment.value());
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access field: " + field.getName(), e);
        }
    }

    private static ConfigurationSection resolveSerializedSection(Config config, String path) {
        ConfigurationSection section = config.raw().getConfigurationSection(path);
        if (section != null) {
            return section;
        }

        Object raw = config.raw().get(path);
        if (raw instanceof ConfigurationSection rawSection) {
            return rawSection;
        }

        if (raw instanceof Map<?, ?> rawMap) {
            YamlConfiguration temp = new YamlConfiguration();
            temp.set("tmp", normalizeMap(rawMap));
            return temp.getConfigurationSection("tmp");
        }

        return null;
    }

    private static void mergeSerializedDefaults(Config config, String path, Object serializedDefaults) {
        if (!(serializedDefaults instanceof Map<?, ?> defaultsMapRaw)) {
            return;
        }

        Map<String, Object> defaults = normalizeMap(defaultsMapRaw);
        mergeMissingKeys(config, path, defaults);
    }

    private static void mergeMissingKeys(Config config, String basePath, Map<String, Object> defaults) {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();
            String fullPath = basePath + "." + key;

            if (!config.exists(fullPath)) {
                config.set(fullPath, defaultValue);
            }

            if (defaultValue instanceof Map<?, ?> defaultMapRaw) {
                Map<String, Object> defaultMap = normalizeMap(defaultMapRaw);
                mergeMissingKeys(config, fullPath, defaultMap);
            }
        }
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
        }
        return result;
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof ConfigurationSection section) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                converted.put(key, normalizeValue(section.get(key)));
            }
            return converted;
        }

        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }

        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>(list.size());
            for (Object item : list) {
                converted.add(normalizeValue(item));
            }
            return converted;
        }

        return value;
    }

    private static void setComments(Config config, String path, String[] comments) {
        YamlConfiguration yaml = (YamlConfiguration) config.raw();
        if (yaml != null && comments != null && comments.length > 0) {
            yaml.setComments(path, Arrays.asList(comments));
        }
    }

    public static void reloadSchema(String fileName) {
        Class<?> schemaClass = registeredSchemas.get(fileName);
        if (schemaClass != null) {
            ensureDefaults(schemaClass);
        }
    }

    public static void reloadAll() {
        List<Class<?>> schemas = new ArrayList<>(registeredSchemas.values());
        for (Class<?> schemaClass : schemas) {
            ensureDefaults(schemaClass);
        }
    }

    public static Map<String, Class<?>> getRegisteredSchemas() {
        return Collections.unmodifiableMap(registeredSchemas);
    }
}
