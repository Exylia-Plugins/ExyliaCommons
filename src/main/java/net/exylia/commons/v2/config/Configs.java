package net.exylia.commons.v2.config;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.debug.config.DebugDefaults;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Configs {
    private static JavaPlugin plugin;
    private static final Map<String, Config> cache = new ConcurrentHashMap<>();
    private static Config mainConfig;

    public static void init(JavaPlugin plugin) {
        Configs.plugin = plugin;
        mainConfig = get("config");
    }

    public static Config get(String fileName) {
        if (plugin == null) {
            throw new IllegalStateException("Configs not initialized. Call Configs.init(plugin) first.");
        }
        return cache.computeIfAbsent(fileName, name -> new Config(plugin, name));
    }

    public static Config file(String fileName) {
        return get(fileName);
    }

    public static FileConfiguration raw(String fileName) {
        return get(fileName).raw();
    }

    public static FileConfiguration raw() {
        if (mainConfig == null) return null;
        return mainConfig.raw();
    }

    public static String string(String path) {
        if (mainConfig == null) return null;
        return mainConfig.string(path);
    }

    public static String string(String path, String defaultValue) {
        if (mainConfig == null) return defaultValue;
        return mainConfig.string(path, defaultValue);
    }

    public static int integer(String path) {
        if (mainConfig == null) return 0;
        return mainConfig.integer(path);
    }

    public static int integer(String path, int defaultValue) {
        if (mainConfig == null) return defaultValue;
        return mainConfig.integer(path, defaultValue);
    }

    @Deprecated
    public static boolean debug() {
        if (mainConfig == null) return false;
        return DebugDefaults.Debug.LEVEL > 0;
    }

    public static boolean bool(String path) {
        if (mainConfig == null) return false;
        return mainConfig.bool(path);
    }

    public static boolean bool(String path, boolean defaultValue) {
        if (mainConfig == null) return defaultValue;
        return mainConfig.bool(path, defaultValue);
    }

    public static double decimal(String path) {
        if (mainConfig == null) return 0.0;
        return mainConfig.decimal(path);
    }

    public static double decimal(String path, double defaultValue) {
        if (mainConfig == null) return defaultValue;
        return mainConfig.decimal(path, defaultValue);
    }

    public static long longValue(String path) {
        if (mainConfig == null) return 0L;
        return mainConfig.longValue(path);
    }

    public static long longValue(String path, long defaultValue) {
        if (mainConfig == null) return defaultValue;
        return mainConfig.longValue(path, defaultValue);
    }

    public static List<String> stringList(String path) {
        if (mainConfig == null) return List.of();
        return mainConfig.stringList(path);
    }

    public static List<Integer> intList(String path) {
        if (mainConfig == null) return List.of();
        return mainConfig.intList(path);
    }

    public static <T> T getValue(String path, Class<T> type) {
        if (mainConfig == null) return null;
        return mainConfig.get(path, type);
    }

    public static ConfigurationSection section(String path) {
        if (mainConfig == null) return null;
        return mainConfig.section(path);
    }

    public static Set<String> getKeys(String path) {
        if (mainConfig == null) return Set.of();
        return mainConfig.getKeys(path);
    }

    public static <T> Map<String, T> map(String path, Function<ConfigurationSection, T> mapper) {
        if (mainConfig == null) return Map.of();
        return mainConfig.map(path, mapper);
    }

    public static <T> List<T> list(String path, Function<ConfigurationSection, T> mapper) {
        if (mainConfig == null) return List.of();
        return mainConfig.list(path, mapper);
    }

    public static Configs set(String path, Object value) {
        if (mainConfig == null) return null;
        mainConfig.set(path, value);
        return null;
    }

    public static boolean exists(String path) {
        if (mainConfig == null) return false;
        return mainConfig.exists(path);
    }

    public static void reload(String fileName) {
        Config config = cache.get(fileName);
        if (config != null) {
            config.reload();
        }
    }

    public static void reloadAll() {
        cache.values().forEach(Config::reload);
        cache.values().forEach((config) -> {
            DebugUtils.logInternalInfo("Reloaded config: " + config.getFileName());
        });
    }

    public static void save(String fileName) {
        Config config = cache.get(fileName);
        if (config != null) {
            config.save();
        }
    }

    public static void save() {
        if (mainConfig == null) return;
        mainConfig.save();
    }

    public static void saveAll() {
        cache.values().forEach(Config::save);
    }

    public static void unload(String fileName) {
        cache.remove(fileName);
    }

    public static void unloadAll() {
        cache.clear();
    }
}
