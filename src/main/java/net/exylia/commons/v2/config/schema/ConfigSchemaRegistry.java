package net.exylia.commons.v2.config.schema;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.visual.config.serializer.ActionBarConfigSerializer;
import net.exylia.commons.v2.visual.config.serializer.BossBarConfigSerializer;
import net.exylia.commons.v2.visual.config.serializer.FireworkConfigSerializer;
import net.exylia.commons.v2.visual.config.serializer.HologramTemplateSerializer;
import net.exylia.commons.v2.visual.config.serializer.TitleConfigSerializer;
import net.exylia.commons.v2.scoreboard.config.serializer.ScoreboardConfigSerializer;
import net.exylia.commons.v2.scoreboard.config.serializer.ScoreboardSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ConfigSchemaRegistry {

    private static final Map<String, List<Class<?>>> registeredSchemas = new HashMap<>();
    private static final Map<Class<?>, ConfigSerializer<?>> serializers = new HashMap<>();
    private static final Set<String> scheduledFinalizations = new HashSet<>();

    static {
        registerSerializer(new BossBarConfigSerializer());
        registerSerializer(new ActionBarConfigSerializer());
        registerSerializer(new TitleConfigSerializer());
        registerSerializer(new HologramTemplateSerializer());
        registerSerializer(new FireworkConfigSerializer());
        registerSerializer(new ScoreboardConfigSerializer());
        registerSerializer(new ScoreboardSerializer());
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

        List<Class<?>> schemas = registeredSchemas.computeIfAbsent(fileName, k -> new ArrayList<>());
        if (!schemas.contains(schemaClass)) {
            schemas.add(schemaClass);
        }

        Config config = Configs.file(fileName);
        config.reload();

        DebugAPI.logLibDebug("[ConfigSchema] ensureDefaults for " + fileName + ".yml");

        processClass(schemaClass, "", config);

        config.save();

        loadClass(schemaClass, "", config);

        scheduleFinalizationIfNeeded(fileName);
    }

    private static void scheduleFinalizationIfNeeded(String fileName) {
        if (scheduledFinalizations.contains(fileName)) return;

        List<Class<?>> schemas = registeredSchemas.get(fileName);
        if (schemas == null) return;

        boolean anyStrict = schemas.stream().anyMatch(c -> c.getAnnotation(ConfigSchema.class).strict());
        if (!anyStrict) return;

        if (!net.exylia.commons.v2.tasks.api.TaskAPI.isInitialized()) return;

        scheduledFinalizations.add(fileName);
        net.exylia.commons.v2.tasks.api.TaskAPI.asyncScheduledLater(
                () -> finalizeStrict(fileName),
                30, TimeUnit.SECONDS
        );
        DebugAPI.logLibDebug("[ConfigSchema] Strict finalization scheduled for " + fileName + ".yml in 30s");
    }

    public static void finalizeStrict(String fileName) {
        List<Class<?>> schemas = registeredSchemas.get(fileName);
        if (schemas == null || schemas.isEmpty()) return;

        boolean anyStrict = schemas.stream().anyMatch(c -> c.getAnnotation(ConfigSchema.class).strict());
        if (!anyStrict) return;

        Config config = Configs.file(fileName);

        Set<String> allPaths = new HashSet<>();
        for (Class<?> cls : schemas) {
            allPaths.addAll(collectSchemaPaths(cls, ""));
        }

        removeOrphanedKeys(config, allPaths);
        config.save();

        config.reload();
        for (Class<?> cls : schemas) {
            loadClass(cls, "", config);
        }

        scheduledFinalizations.remove(fileName);
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
                        DebugAPI.logLibDebug("[ConfigSchema] Loading (serialized) " + path + " from legacy path " + legacyPath);
                    }
                }

                Object value = section != null ? serializer.deserialize(section) : defaultValue;
                DebugAPI.logLibDebug("[ConfigSchema] Loading (serialized) " + path + " | found=" + (section != null));
                if (value != null) {
                    field.set(null, value);
                }
            } else if (List.class.isAssignableFrom(field.getType())) {
                ConfigSerializer itemSerializer = getListItemSerializer(field);
                if (itemSerializer != null) {
                    List<?> rawList = config.raw().getList(path);
                    if (rawList != null && !rawList.isEmpty()) {
                        List<Object> result = new ArrayList<>(rawList.size());
                        for (Object item : rawList) {
                            ConfigurationSection section = toSection(item);
                            if (section != null) {
                                result.add(itemSerializer.deserialize(section));
                            }
                        }
                        if (!result.isEmpty()) {
                            field.set(null, result);
                        }
                    }
                    DebugAPI.logLibDebug("[ConfigSchema] Loading (list-serialized) " + path + " | size=" + (rawList != null ? rawList.size() : 0));
                } else {
                    Object rawValue = config.raw().get(path);
                    if (rawValue instanceof String stringValue) {
                        rawValue = List.of(stringValue);
                    }
                    Object value = rawValue != null ? rawValue : defaultValue;
                    DebugAPI.logLibDebug("[ConfigSchema] Loading " + path + " | raw=" + rawValue + " | default=" + defaultValue + " | final=" + value);
                    if (value != null) {
                        field.set(null, value);
                    }
                }
            } else {
                Object rawValue = config.raw().get(path);
                Object value = rawValue != null ? rawValue : defaultValue;
                DebugAPI.logLibDebug("[ConfigSchema] Loading " + path + " | raw=" + rawValue + " | default=" + defaultValue + " | final=" + value);
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
            } else if (List.class.isAssignableFrom(field.getType())) {
                ConfigSerializer itemSerializer = getListItemSerializer(field);
                if (itemSerializer != null) {
                    if (!config.exists(path)) {
                        config.set(path, serializeList((List<?>) value, itemSerializer));
                    }
                } else if (!config.exists(path)) {
                    config.set(path, value);
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

    private static ConfigSerializer<?> getListItemSerializer(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType pt)) return null;
        Type[] typeArgs = pt.getActualTypeArguments();
        if (typeArgs.length != 1 || !(typeArgs[0] instanceof Class<?> itemClass)) return null;
        return serializers.get(itemClass);
    }

    private static ConfigurationSection toSection(Object item) {
        if (item instanceof ConfigurationSection section) {
            return section;
        }
        if (item instanceof Map<?, ?> map) {
            YamlConfiguration temp = new YamlConfiguration();
            temp.set("item", normalizeMap(map));
            return temp.getConfigurationSection("item");
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Object> serializeList(List<?> list, ConfigSerializer serializer) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(serializer.serialize(item));
        }
        return result;
    }

    public static void reloadSchema(String fileName) {
        List<Class<?>> schemas = registeredSchemas.get(fileName);
        if (schemas == null || schemas.isEmpty()) return;

        for (Class<?> schemaClass : new ArrayList<>(schemas)) {
            ConfigSchema schema = schemaClass.getAnnotation(ConfigSchema.class);
            Config config = Configs.file(schema.file());
            config.reload();
            processClass(schemaClass, "", config);
            config.save();
            loadClass(schemaClass, "", config);
        }

        finalizeStrict(fileName);
    }

    public static void reloadAll() {
        for (String fileName : new ArrayList<>(registeredSchemas.keySet())) {
            reloadSchema(fileName);
        }
    }

    public static Map<String, List<Class<?>>> getRegisteredSchemas() {
        return Collections.unmodifiableMap(registeredSchemas);
    }

    private static Set<String> collectSchemaPaths(Class<?> clazz, String prefix) {
        Set<String> paths = new HashSet<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!field.isAnnotationPresent(ConfigValue.class)) continue;

            ConfigValue cv = field.getAnnotation(ConfigValue.class);
            String path = prefix.isEmpty() ? cv.value() : prefix + "." + cv.value();
            paths.add(path);
        }

        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (!innerClass.isAnnotationPresent(ConfigSection.class)) continue;
            ConfigSection section = innerClass.getAnnotation(ConfigSection.class);
            String sectionPath = prefix.isEmpty() ? section.value() : prefix + "." + section.value();
            paths.addAll(collectSchemaPaths(innerClass, sectionPath));
        }

        return paths;
    }

    private static void removeOrphanedKeys(Config config, Set<String> schemaPaths) {
        Set<String> preserved = config.getPreservedPrefixes();
        removeOrphanedKeysInSection(config.raw(), "", schemaPaths, preserved);
    }

    private static void removeOrphanedKeysInSection(ConfigurationSection section, String currentPrefix, Set<String> schemaPaths, Set<String> preserved) {
        for (String key : new HashSet<>(section.getKeys(false))) {
            String fullPath = currentPrefix.isEmpty() ? key : currentPrefix + "." + key;

            boolean isOwned = false;
            for (String schemaPath : schemaPaths) {
                if (schemaPath.equals(fullPath) || schemaPath.startsWith(fullPath + ".") || fullPath.startsWith(schemaPath + ".")) {
                    isOwned = true;
                    break;
                }
            }

            if (!isOwned) {
                for (String pPath : preserved) {
                    if (fullPath.equals(pPath) || fullPath.startsWith(pPath + ".") || pPath.startsWith(fullPath + ".")) {
                        isOwned = true;
                        break;
                    }
                }
            }

            if (!isOwned) {
                section.set(key, null);
                DebugAPI.logLibDebug("[ConfigSchema] [STRICT] Removed orphaned key: " + fullPath);
            } else if (section.isConfigurationSection(key)) {
                boolean fullyPreserved = false;
                for (String pPath : preserved) {
                    if (fullPath.equals(pPath) || fullPath.startsWith(pPath + ".")) {
                        fullyPreserved = true;
                        break;
                    }
                }
                if (!fullyPreserved) {
                    removeOrphanedKeysInSection(section.getConfigurationSection(key), fullPath, schemaPaths, preserved);
                }
            }
        }
    }
}
