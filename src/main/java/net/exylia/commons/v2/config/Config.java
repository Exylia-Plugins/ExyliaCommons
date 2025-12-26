package net.exylia.commons.v2.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            try {
                String resourcePath = fileName.replace("\\", "/") + ".yml";
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Config file not found in resources: " + fileName + ".yml");
                plugin.getLogger().warning("Creating empty config file...");
                try {
                    file.createNewFile();
                } catch (Exception ex) {
                    throw new RuntimeException("Could not create config file: " + fileName, ex);
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
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

    public FileConfiguration raw() {
        return config;
    }
}
