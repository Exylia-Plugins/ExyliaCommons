package net.exylia.commons.v2.items.config;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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

    public void applyTrim(ArmorMeta meta, Player player, PlaceholderContext context) {
        String resolvedMaterial = resolvePlaceholder(material, player, context);
        String resolvedPattern = resolvePlaceholder(pattern, player, context);

        if (resolvedMaterial == null || resolvedMaterial.isEmpty() ||
                resolvedPattern == null || resolvedPattern.isEmpty()) {
            return;
        }

        try {
            TrimMaterial trimMaterial = getTrimMaterial(resolvedMaterial);
            TrimPattern trimPattern = getTrimPattern(resolvedPattern);

            if (trimMaterial != null && trimPattern != null) {
                ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
                meta.setTrim(trim);
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                        "Applied armor trim: " + resolvedPattern + " with material: " + resolvedMaterial);
            } else {
                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                        "Failed to apply armor trim - material or pattern not found");
            }
        } catch (Exception e) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                    "Error applying armor trim: " + e.getMessage());
        }
    }

    public void applyTrim(ArmorMeta meta) {
        applyTrim(meta, null, null);
    }

    private String resolvePlaceholder(String value, Player player, PlaceholderContext context) {
        if (value == null) return null;
        if (player != null && context != null) {
            return Placeholders.process(value, player, context);
        }
        return value;
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
