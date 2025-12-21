package net.exylia.commons.v2.ui.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.ui.cache.MenuCacheManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class UIConfigLoader {
    private final JavaPlugin plugin;
    private final MenuCacheManager cacheManager;
    private final Map<String, String> configPaths = new HashMap<>();

    public UIConfigLoader(JavaPlugin plugin, MenuCacheManager cacheManager) {
        this.plugin = plugin;
        this.cacheManager = cacheManager;
    }

    public MenuConfig loadMenuConfig(String fileName) {
        return cacheManager.getConfig(fileName).orElseGet(() -> {
            MenuConfig config = loadFromFile(fileName);
            if (config != null) {
                cacheManager.cacheConfig(fileName, config);
            }
            return config;
        });
    }

    private MenuConfig loadFromFile(String fileName) {
        String path = "menus/" + fileName;
        if (!path.endsWith(".yml")) {
            path += ".yml";
        }

        configPaths.put(fileName, path);

        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }

        try {
            Config config = Configs.get(path);
            FileConfiguration fileConfig = config.raw();

            return MenuParser.parseMenuConfig(fileConfig);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load menu config: " + fileName + " - " + e.getMessage());
            return null;
        }
    }

    public Optional<MenuConfig> getConfig(String fileName) {
        return cacheManager.getConfig(fileName);
    }

    public void reloadConfig(String fileName) {
        cacheManager.invalidateConfig(fileName);
        String path = configPaths.get(fileName);
        if (path != null) {
            Configs.reload(path);
        }
        loadMenuConfig(fileName);
    }

    public void reloadAll() {
        cacheManager.invalidateAllConfigs();

        configPaths.values().forEach(Configs::reload);

        configPaths.keySet().forEach(this::loadMenuConfig);
    }

    public int getLoadedConfigCount() {
        return configPaths.size();
    }
}
