package net.exylia.commons.config;

import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.BossBarConfig;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class ConfigManager {
    private static ConfigurationSystem internalSystem;
    private static final Map<Class<?>, Object> staticConfigs = new ConcurrentHashMap<>();
    private static Class<? extends ConfigBase>[] configClasses;

    public static void init(ConfigurationSystem existingSystem, Class<? extends ConfigBase>... configClasses) {
        ConfigManager.configClasses = configClasses;
        internalSystem = existingSystem;  

        for (Class<? extends ConfigBase> configClass : configClasses) {
            staticConfigs.put(configClass, internalSystem.getConfig(configClass));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ConfigBase> T get(Class<T> configClass) {
        return (T) staticConfigs.get(configClass);
    }

    public static <T> T getValue(Class<? extends ConfigBase> configClass, String path, T defaultValue) {
        ConfigBase config = get(configClass);
        if (config == null || config.getConfig() == null) return defaultValue;

        Object value = config.getConfig().get(path);
        if (value == null) return defaultValue;

        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public static String getString(Class<? extends ConfigBase> configClass, String path, String defaultValue) {
        ConfigBase config = get(configClass);
        if (config == null || config.getConfig() == null) return defaultValue;
        return config.getConfig().getString(path, defaultValue);
    }

    public static boolean getBoolean(Class<? extends ConfigBase> configClass, String path, boolean defaultValue) {
        ConfigBase config = get(configClass);
        if (config == null || config.getConfig() == null) return defaultValue;
        return config.getConfig().getBoolean(path, defaultValue);
    }

    public static int getInt(Class<? extends ConfigBase> configClass, String path, int defaultValue) {
        ConfigBase config = get(configClass);
        if (config == null || config.getConfig() == null) return defaultValue;
        return config.getConfig().getInt(path, defaultValue);
    }

    public static double getDouble(Class<? extends ConfigBase> configClass, String path, double defaultValue) {
        ConfigBase config = get(configClass);
        if (config == null || config.getConfig() == null) return defaultValue;
        return config.getConfig().getDouble(path, defaultValue);
    }

    public static long getLong(Class<? extends ConfigBase> configClass, String path, long defaultValue) {
        ConfigBase config = get(configClass);
        if (config == null || config.getConfig() == null) return defaultValue;
        return config.getConfig().getLong(path, defaultValue);
    }

    public static String getStringFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            String defaultValue = annotation.defaultValue();
            return getString(configClass, path, defaultValue);
        }

        return "";
    }

    public static boolean getBooleanFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            boolean defaultValue = !annotation.defaultValue().isEmpty() && Boolean.parseBoolean(annotation.defaultValue());
            return getBoolean(configClass, path, defaultValue);
        }

        return false;
    }

    public static int getIntFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            int defaultValue = parseIntDefault(annotation.defaultValue());
            return getInt(configClass, path, defaultValue);
        }

        return 0;
    }

    public static double getDoubleFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            double defaultValue = parseDoubleDefault(annotation.defaultValue());
            return getDouble(configClass, path, defaultValue);
        }

        return 0.0;
    }

    public static long getLongFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            long defaultValue = parseLongDefault(annotation.defaultValue());
            return getLong(configClass, path, defaultValue);
        }

        return 0L;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFromMethod(Class<T> componentClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                ConfigBase config = get(configClass);
                if (config == null || config.getConfig() == null) return null;

                ConfigurationSection section = config.getConfig().getConfigurationSection(path);
                if (section == null) return null;

                try {
                    Constructor<T> constructor = componentClass.getConstructor(ConfigurationSection.class);
                    return constructor.newInstance(section);
                } catch (Exception e) {
                    try {
                        Constructor<T> constructor = componentClass.getConstructor(String.class, ConfigurationSection.class);
                        return constructor.newInstance(path, config.getConfig());
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getListFromMethod(Class<T> componentClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                ConfigBase config = get(configClass);
                if (config == null || config.getConfig() == null) return new ArrayList<>();

                ConfigurationSection section = config.getConfig().getConfigurationSection(path);
                if (section == null) return new ArrayList<>();

                List<T> result = new ArrayList<>();
                for (String key : section.getKeys(false)) {
                    ConfigurationSection itemSection = section.getConfigurationSection(key);
                    if (itemSection != null) {
                        try {
                            Constructor<T> constructor = componentClass.getConstructor(ConfigurationSection.class);
                            T instance = constructor.newInstance(itemSection);
                            result.add(instance);
                        } catch (Exception e) {
                            try {
                                Constructor<T> constructor = componentClass.getConstructor(String.class, ConfigurationSection.class);
                                T instance = constructor.newInstance(path + "." + key, config.getConfig());
                                result.add(instance);
                            } catch (Exception ex) {
                                continue;
                            }
                        }
                    }
                }
                return result;
            }
        }

        return new ArrayList<>();
    }

    public static ActionBarConfig getActionBarFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ActionBarConfig();

            return new ActionBarConfig(path, config.getConfig());
        }

        return new ActionBarConfig();
    }

    public static BossBarConfig getBossBarFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new BossBarConfig();

            ConfigurationSection section = config.getConfig().getConfigurationSection(path);
            if (section != null) {
                return new BossBarConfig(section);
            }

            return new BossBarConfig(path, config.getConfig());
        }

        return new BossBarConfig();
    }

    public static ScoreboardConfig getScoreboardFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ScoreboardConfig();

            return new ScoreboardConfig(path, config.getConfig());
        }

        return new ScoreboardConfig();
    }

    public static TitleConfig getTitleFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new TitleConfig();

            return new TitleConfig(path, config.getConfig());
        }

        return new TitleConfig();
    }

    public static List<ActionBarConfig> getActionBarListFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ArrayList<>();

            ConfigurationSection section = config.getConfig().getConfigurationSection(path);
            if (section == null) return new ArrayList<>();

            List<ActionBarConfig> result = new ArrayList<>();
            for (String key : section.getKeys(false)) {
                result.add(new ActionBarConfig(path + "." + key, config.getConfig()));
            }
            return result;
        }

        return new ArrayList<>();
    }

    public static List<BossBarConfig> getBossBarListFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ArrayList<>();

            ConfigurationSection section = config.getConfig().getConfigurationSection(path);
            if (section == null) return new ArrayList<>();

            List<BossBarConfig> result = new ArrayList<>();
            for (String key : section.getKeys(false)) {
                ConfigurationSection itemSection = section.getConfigurationSection(key);
                if (itemSection != null) {
                    result.add(new BossBarConfig(itemSection));
                } else {
                    result.add(new BossBarConfig(path + "." + key, config.getConfig()));
                }
            }
            return result;
        }

        return new ArrayList<>();
    }

    public static List<ScoreboardConfig> getScoreboardListFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ArrayList<>();

            ConfigurationSection section = config.getConfig().getConfigurationSection(path);
            if (section == null) return new ArrayList<>();

            List<ScoreboardConfig> result = new ArrayList<>();
            for (String key : section.getKeys(false)) {
                result.add(new ScoreboardConfig(path + "." + key, config.getConfig()));
            }
            return result;
        }

        return new ArrayList<>();
    }

    public static List<TitleConfig> getTitleListFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ArrayList<>();

            ConfigurationSection section = config.getConfig().getConfigurationSection(path);
            if (section == null) return new ArrayList<>();

            List<TitleConfig> result = new ArrayList<>();
            for (String key : section.getKeys(false)) {
                result.add(new TitleConfig(path + "." + key, config.getConfig()));
            }
            return result;
        }

        return new ArrayList<>();
    }

    public static List<String> getStringListFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ArrayList<>();

            return config.getConfig().getStringList(path);
        }

        return new ArrayList<>();
    }

    public static List<Integer> getIntListFromMethod(Class<? extends ConfigBase> configClass) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
        if (annotation != null) {
            String path = annotation.value();
            ConfigBase config = get(configClass);
            if (config == null || config.getConfig() == null) return new ArrayList<>();

            return config.getConfig().getIntegerList(path);
        }

        return new ArrayList<>();
    }

    private static int parseIntDefault(String value) {
        try { return value.isEmpty() ? 0 : Integer.parseInt(value); }
        catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleDefault(String value) {
        try { return value.isEmpty() ? 0.0 : Double.parseDouble(value); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private static long parseLongDefault(String value) {
        try { return value.isEmpty() ? 0L : Long.parseLong(value); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static ConfigValue findAnnotationInHierarchy(Class<? extends ConfigBase> configClass, String methodName) {
        Class<?> currentClass = configClass;

        while (currentClass != null && ConfigBase.class.isAssignableFrom(currentClass)) {
            try {
                java.lang.reflect.Method method = currentClass.getDeclaredMethod(methodName);
                ConfigValue annotation = method.getAnnotation(ConfigValue.class);
                if (annotation != null) {
                    return annotation;
                }
            } catch (NoSuchMethodException ignored) {}

            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends ConfigBase> findConfigClassFromStack() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (int i = 2; i < stack.length; i++) {
            try {
                String className = stack[i].getClassName();
                Class<?> clazz = Class.forName(className);

                if (ConfigBase.class.isAssignableFrom(clazz) && clazz != ConfigBase.class) {
                    return (Class<? extends ConfigBase>) clazz;
                }
            } catch (ClassNotFoundException ignored) {}
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends ConfigBase> findConfigClassForMethod(String methodName) {
        if (configClasses == null || configClasses.length == 0) {
            return null;
        }

        for (Class<? extends ConfigBase> configClass : configClasses) {
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                return configClass;
            }
        }

        return null;
    }

    public static String getStringFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        Class<? extends ConfigBase> foundClass = findConfigClassForMethod(methodName);
        if (foundClass != null) {
            ConfigValue annotation = findAnnotationInHierarchy(foundClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                String defaultValue = annotation.defaultValue();
                return getString(foundClass, path, defaultValue);
            }
        }

        return "";
    }

    public static boolean getBooleanFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        Class<? extends ConfigBase> foundClass = findConfigClassForMethod(methodName);
        if (foundClass != null) {
            ConfigValue annotation = findAnnotationInHierarchy(foundClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                boolean defaultValue = !annotation.defaultValue().isEmpty() && Boolean.parseBoolean(annotation.defaultValue());
                return getBoolean(foundClass, path, defaultValue);
            }
        }

        return false;
    }

    public static int getIntFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        Class<? extends ConfigBase> foundClass = findConfigClassForMethod(methodName);
        if (foundClass != null) {
            ConfigValue annotation = findAnnotationInHierarchy(foundClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                int defaultValue = parseIntDefault(annotation.defaultValue());
                return getInt(foundClass, path, defaultValue);
            }
        }

        return 0;
    }

    public static double getDoubleFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        Class<? extends ConfigBase> foundClass = findConfigClassForMethod(methodName);
        if (foundClass != null) {
            ConfigValue annotation = findAnnotationInHierarchy(foundClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                double defaultValue = parseDoubleDefault(annotation.defaultValue());
                return getDouble(foundClass, path, defaultValue);
            }
        }

        return 0.0;
    }

    public static long getLongFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        Class<? extends ConfigBase> foundClass = findConfigClassForMethod(methodName);
        if (foundClass != null) {
            ConfigValue annotation = findAnnotationInHierarchy(foundClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                long defaultValue = parseLongDefault(annotation.defaultValue());
                return getLong(foundClass, path, defaultValue);
            }
        }

        return 0L;
    }

    public static FileConfiguration getFile(String fileName) {
        return internalSystem.getFile(fileName);
    }

    public static FileConfiguration getFile(Class<? extends ConfigBase> configClass) {
        ConfigBase config = get(configClass);
        return config != null ? config.getConfig() : null;
    }

    public static ConfigurationSystem getSystem() {
        return internalSystem;
    }

    public static CompletableFuture<Boolean> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                 
                boolean systemReloadSuccess = internalSystem.reloadAllAsync().join();
                if (!systemReloadSuccess) {
                    DebugUtils.logInternalError("ERROR: Fallo en reload del sistema interno");
                    return false;
                }

                staticConfigs.clear();

                for (Class<? extends ConfigBase> configClass : configClasses) {
                    try {
                         
                        Object reloadedInstance = internalSystem.getConfig(configClass);
                        if (reloadedInstance != null) {
                            staticConfigs.put(configClass, reloadedInstance);
                        } else {
                            DebugUtils.logInternalError("ERROR: No se pudo obtener instancia recargada para: " + configClass.getSimpleName());
                            return false;
                        }
                    } catch (Exception e) {
                        DebugUtils.logInternalError("ERROR: Fallo recargando " + configClass.getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }

                return true;

            } catch (Exception e) {
                DebugUtils.logInternalError("ERROR: Fallo crítico en reload de ConfigManager: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
}
