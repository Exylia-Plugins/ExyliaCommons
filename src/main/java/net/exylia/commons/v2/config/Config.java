package net.exylia.commons.v2.config;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConfigurationOptions;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class Config {
    @Getter
    private final JavaPlugin plugin;
    @Getter
    private final String fileName;
    private final File file;
    private FileConfiguration config;

    Config(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;

        if (fileName.contains("/") || fileName.contains("\\")) {
            String normalizedPath = fileName.replace("/", File.separator).replace("\\", File.separator);
            this.file = new File(plugin.getDataFolder(), normalizedPath + ".yml");
        } else {
            this.file = new File(plugin.getDataFolder(), fileName + ".yml");
        }
        load();
    }

    private void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            String resourcePath = fileName.replace("\\", "/") + ".yml";

            ClassLoader resourceLoader = Configs.getResourceClassLoader();
            if (resourceLoader != null) {
                saveResourceFromClassLoader(resourcePath, resourceLoader);
            } else {
                try {
                    plugin.saveResource(resourcePath, false);
                } catch (IllegalArgumentException e) {
                    DebugAPI.logLibInfo("Config file not found in resources: " + fileName + ".yml | Creating it with default values");
                    try {
                        file.createNewFile();
                    } catch (Exception ex) {
                        throw new RuntimeException("Could not create config file: " + fileName, ex);
                    }
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        ((YamlConfigurationOptions) config.options()).width(Integer.MAX_VALUE);
    }

    private void saveResourceFromClassLoader(String resourcePath, ClassLoader classLoader) {
        InputStream in = classLoader.getResourceAsStream(resourcePath);
        if (in == null) {
            DebugAPI.logLibInfo("Config file not found in resources: " + resourcePath + ".yml | Creating it with default values");
            try {
                file.createNewFile();
            } catch (Exception ex) {
                throw new RuntimeException("Could not create config file: " + fileName, ex);
            }
            return;
        }

        try (OutputStream out = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save " + file.getName());
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {}
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        ((YamlConfigurationOptions) config.options()).width(Integer.MAX_VALUE);
    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Error saving config: " + fileName, e);
        }
    }

    public String string(String path) {
        return config.getString(path);
    }

    public String string(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public int integer(String path) {
        return config.getInt(path);
    }

    public int integer(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public double decimal(String path) {
        return config.getDouble(path);
    }

    public double decimal(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    public boolean bool(String path) {
        return config.getBoolean(path);
    }

    public boolean bool(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public long longValue(String path) {
        return config.getLong(path);
    }

    public long longValue(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }

    public List<String> stringList(String path) {
        return config.getStringList(path);
    }

    public List<Integer> intList(String path) {
        return config.getIntegerList(path);
    }

    public <T> T get(String path, Class<T> type) {
        return type.cast(config.get(path));
    }

    public ConfigurationSection section(String path) {
        return config.getConfigurationSection(path);
    }

    public Set<String> getKeys(String path) {
        ConfigurationSection section = section(path);
        return section != null ? section.getKeys(false) : Set.of();
    }

    public <T> Map<String, T> map(String path, Function<ConfigurationSection, T> mapper) {
        ConfigurationSection section = section(path);
        if (section == null) return new HashMap<>();

        Map<String, T> result = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection sub = section.getConfigurationSection(key);
            if (sub != null) {
                result.put(key, mapper.apply(sub));
            }
        }
        return result;
    }

    public <T> List<T> list(String path, Function<ConfigurationSection, T> mapper) {
        ConfigurationSection section = section(path);
        if (section == null) return List.of();

        return section.getKeys(false).stream()
                .map(section::getConfigurationSection)
                .filter(sub -> sub != null)
                .map(mapper)
                .toList();
    }

    public Config set(String path, Object value) {
        config.set(path, value);
        return this;
    }

    public boolean exists(String path) {
        return config.contains(path);
    }

    public boolean isSet(String path) {
        return config.isSet(path);
    }

    public boolean merge(Collection<String> optionalPaths) {
        return ConfigMerger.merge(this, optionalPaths);
    }

    public boolean merge() {
        return ConfigMerger.merge(this, Set.of());
    }

    public FileConfiguration raw() {
        return config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
