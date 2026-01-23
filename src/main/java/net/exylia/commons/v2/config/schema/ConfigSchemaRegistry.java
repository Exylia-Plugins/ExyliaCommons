package net.exylia.commons.v2.config.schema;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.Configs;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigSchemaRegistry {

    private static final Map<String, Class<?>> registeredSchemas = new HashMap<>();

    public static void ensureDefaults(Class<?> schemaClass) {
        if (!schemaClass.isAnnotationPresent(ConfigSchema.class)) {
            throw new IllegalArgumentException("Class " + schemaClass.getName() + " is not annotated with @ConfigSchema");
        }

        ConfigSchema schema = schemaClass.getAnnotation(ConfigSchema.class);
        String fileName = schema.file();

        registeredSchemas.put(fileName, schemaClass);

        Config config = Configs.file(fileName);

        DebugUtils.logInternalDebug("[ConfigSchema] ensureDefaults for " + fileName + ".yml - File exists: " + config.exists("database"));

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

    private static void loadField(Field field, String prefix, Config config) {
        ConfigValue configValue = field.getAnnotation(ConfigValue.class);
        String path = prefix.isEmpty() ? configValue.value() : prefix + "." + configValue.value();

        try {
            field.setAccessible(true);
            Object defaultValue = field.get(null);
            Object rawValue = config.raw().get(path);
            Object value = rawValue != null ? rawValue : defaultValue;

            DebugUtils.logInternalDebug("[ConfigSchema] Loading " + path + " | raw=" + rawValue + " | default=" + defaultValue + " | final=" + value);

            if (value != null) {
                field.set(null, value);
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

    private static void processField(Field field, String prefix, Config config) {
        ConfigValue configValue = field.getAnnotation(ConfigValue.class);
        String path = prefix.isEmpty() ? configValue.value() : prefix + "." + configValue.value();

        try {
            field.setAccessible(true);
            Object value = field.get(null);

            if (!config.exists(path)) {
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
        registeredSchemas.forEach((fileName, schemaClass) -> {
            Config config = Configs.file(fileName);
            config.reload();
            loadClass(schemaClass, "", config);
        });
    }

    public static Map<String, Class<?>> getRegisteredSchemas() {
        return Collections.unmodifiableMap(registeredSchemas);
    }
}
