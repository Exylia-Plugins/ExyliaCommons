package net.exylia.commons.v2.items.config;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

@Getter
public class ArmorTrimConfig {

    private String material;
    private String pattern;

    public static ArmorTrimConfig fromConfig(ConfigurationSection config) {
        if (config == null) return null;

        ArmorTrimConfig trimConfig = new ArmorTrimConfig();

        if (config.contains("material")) {
            trimConfig.setMaterial(config.getString("material"));
        }

        if (config.contains("pattern")) {
            trimConfig.setPattern(config.getString("pattern"));
        }

        return trimConfig;
    }

    public ArmorTrimConfig setMaterial(String material) {
        this.material = material;
        return this;
    }

    public ArmorTrimConfig setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public void applyTrim(ArmorMeta meta) {
        if (material == null || pattern == null) return;

        try {
            TrimMaterial trimMaterial = getTrimMaterial(material);
            TrimPattern trimPattern = getTrimPattern(pattern);

            if (trimMaterial != null && trimPattern != null) {
                ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
                meta.setTrim(trim);
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                    "Applied armor trim: " + pattern + " with material: " + material);
            } else {
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                    "Failed to apply armor trim - material or pattern not found");
            }
        } catch (Exception e) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "Error applying armor trim: " + e.getMessage());
        }
    }

    private TrimMaterial getTrimMaterial(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return Registry.TRIM_MATERIAL.get(key);
        } catch (Exception e) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "Invalid trim material: " + name + " - " + e.getMessage());
            return null;
        }
    }

    private TrimPattern getTrimPattern(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return Registry.TRIM_PATTERN.get(key);
        } catch (Exception e) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "Invalid trim pattern: " + name + " - " + e.getMessage());
            return null;
        }
    }

    public boolean hasConfiguration() {
        return material != null && pattern != null;
    }
}
