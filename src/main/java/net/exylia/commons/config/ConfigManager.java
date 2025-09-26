package net.exylia.commons.config;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static ConfigurationSystem internalSystem;
    private static final Map<Class<?>, Object> staticConfigs = new ConcurrentHashMap<>();
    private static Class<? extends ConfigBase>[] configClasses;

    public static void init(ConfigurationSystem existingSystem, Class<? extends ConfigBase>... configClasses) {
        ConfigManager.configClasses = configClasses;
        internalSystem = existingSystem; // Usar el sistema existente en lugar de crear uno nuevo

        // Registrar configs para acceso estático usando el sistema existente
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

    public static String getStringFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                String defaultValue = annotation.defaultValue();
                return getString(configClass, path, defaultValue);
            }
        }

        return "";
    }

    public static boolean getBooleanFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                boolean defaultValue = !annotation.defaultValue().isEmpty() && Boolean.parseBoolean(annotation.defaultValue());
                return getBoolean(configClass, path, defaultValue);
            }
        }

        return false;
    }

    public static int getIntFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                int defaultValue = parseIntDefault(annotation.defaultValue());
                return getInt(configClass, path, defaultValue);
            }
        }

        return 0;
    }

    public static double getDoubleFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                double defaultValue = parseDoubleDefault(annotation.defaultValue());
                return getDouble(configClass, path, defaultValue);
            }
        }

        return 0.0;
    }

    public static long getLongFromMethodAuto() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String methodName = caller.getMethodName();

        for (Class<?> clazz : staticConfigs.keySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends ConfigBase> configClass = (Class<? extends ConfigBase>) clazz;
            ConfigValue annotation = findAnnotationInHierarchy(configClass, methodName);
            if (annotation != null) {
                String path = annotation.value();
                long defaultValue = parseLongDefault(annotation.defaultValue());
                return getLong(configClass, path, defaultValue);
            }
        }

        return 0L;
    }

    // ===== ACCESO DIRECTO A ARCHIVOS =====
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
                // 1. Recargar el sistema interno
                boolean systemReloadSuccess = internalSystem.reloadAllAsync().join();
                if (!systemReloadSuccess) {
                    DebugUtils.logInternalError("ERROR: Fallo en reload del sistema interno");
                    return false;
                }

                // 2. CRÍTICO: Recrear todas las instancias de configuración
                staticConfigs.clear();

                // 3. Reinicializar cada clase de configuración usando el sistema existente
                for (Class<? extends ConfigBase> configClass : configClasses) {
                    try {
                        // Obtener la instancia ya recargada del sistema
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