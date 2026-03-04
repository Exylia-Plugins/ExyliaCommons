package net.exylia.commons.v2.visual.color;

import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.visual.config.ColorDefaults;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorPresetManager {
    private static volatile ColorPresetManager instance;
    private static final Object LOCK = new Object();

    private final Map<String, String> colorPresets = new ConcurrentHashMap<>();
    private final Pattern PRESET_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private JavaPlugin plugin;
    private boolean initialized = false;

    private ColorPresetManager() {
    }

    public static ColorPresetManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ColorPresetManager();
                }
            }
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        if (initialized) {
            return;
        }

        ConfigSchemaRegistry.ensureDefaults(ColorDefaults.class);
        this.plugin = plugin;
        loadColorPresets();
        this.initialized = true;
    }

    public void initialize(JavaPlugin plugin, Map<String, String> customPresets) {
        if (initialized) {
            return;
        }

        ConfigSchemaRegistry.ensureDefaults(ColorDefaults.class);
        this.plugin = plugin;

        if (customPresets != null && !customPresets.isEmpty()) {
            colorPresets.clear();
            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String value = entry.getValue();
                if (key != null && value != null) {
                    colorPresets.put(key, value);
                }
            }
            createCustomColorPresetsFile(customPresets);
        } else {
            loadColorPresets();
        }

        this.initialized = true;
    }

    private void loadColorPresets() {
        if (plugin == null) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), "colors.yml");

        if (!configFile.exists()) {
            createDefaultColorPresets(configFile);
        }

        FileConfiguration colorConfig = YamlConfiguration.loadConfiguration(configFile);
        colorPresets.clear();

        for (String key : colorConfig.getKeys(false)) {
            String value = colorConfig.getString(key);
            if (value != null) {
                colorPresets.put(key.toLowerCase(), value);
            }
        }

        DebugAPI.logLibSuccess(DebugCategory.VISUAL, "Loaded " + colorPresets.size() + " color presets.");
    }

    private void createDefaultColorPresets(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            config.set("primary", "<#8a51c4>");
            config.set("secondary", "<#aa76de>");
            config.set("secondary_light", "<#b48fd9>");

            config.set("letters", "<#e7cfff>");
            config.set("letters_black", "<#a89ab5>");

            config.set("error", "<#a33b53>");
            config.set("success", "<#8fffc1>");
            config.set("success_light", "<#a1ffc3>");
            config.set("warning", "<#ff9500>");
            config.set("warning_light", "<#ffd2a8>");
            config.set("info", "<#59a4ff>");
            config.set("info_light", "<#7db7ff>");

            config.set("accent", "<#ff6b9d>");
            config.set("neutral", "<#6c757d>");
            config.set("highlight", "<#ffd700>");
            config.set("muted", "<#868e96>");

            config.set("gradient_primary", "<gradient:#8a51c4:#aa76de>");
            config.set("gradient_success", "<gradient:#8fffc1:#a1ffc3>");
            config.set("gradient_warning", "<gradient:#ff9500:#ffd2a8>");
            config.set("gradient_error", "<gradient:#a33b53:#ff6b9d>");

            config.save(configFile);
        } catch (IOException e) {
            DebugAPI.logLibError(DebugCategory.VISUAL, "Error creating colors.yml: " + e.getMessage());
        }
    }

    private void createCustomColorPresetsFile(Map<String, String> customPresets) {
        if (plugin == null) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), "colors.yml");

        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            for (String key : config.getKeys(false)) {
                config.set(key, null);
            }

            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    config.set(key, value);
                }
            }

            config.save(configFile);
        } catch (IOException e) {
            DebugAPI.logLibError(DebugCategory.VISUAL, "Error creating colors.yml: " + e.getMessage());
        }
    }

    public String applyColorPresets(String message) {
        if (message == null || message.isEmpty() || !initialized || !message.contains("{")) {
            return message;
        }

        Matcher matcher = PRESET_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String presetName = matcher.group(1).toLowerCase();
            String colorCode = colorPresets.get(presetName);

            if (colorCode != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(colorCode));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public String getColorPreset(String presetName) {
        return colorPresets.get(presetName.toLowerCase());
    }

    public Map<String, String> getAllColorPresets() {
        return new HashMap<>(colorPresets);
    }

    public void addCustomColorPreset(String name, String colorCode) {
        if (name != null && colorCode != null) {
            colorPresets.put(name.toLowerCase(), colorCode);
        }
    }

    public void addCustomColorPresets(Map<String, String> customPresets) {
        if (customPresets != null && !customPresets.isEmpty()) {
            for (Map.Entry<String, String> entry : customPresets.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    colorPresets.put(key.toLowerCase(), value);
                }
            }
        }
    }

    public void reload() {
        if (initialized) {
            loadColorPresets();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
