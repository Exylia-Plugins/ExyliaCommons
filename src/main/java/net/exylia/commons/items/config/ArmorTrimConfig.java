package net.exylia.commons.items.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ArmorMeta;

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
            Object trimMaterial = getTrimMaterial(material);
            Object trimPattern = getTrimPattern(pattern);

            if (trimMaterial != null && trimPattern != null) {
                Object trim = createTrim(trimMaterial, trimPattern);
                if (trim != null) {
                    meta.getClass().getMethod("setTrim", trim.getClass()).invoke(meta, trim);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Object getTrimMaterial(String name) {
        try {
            Class<?> trimMaterialClass = Class.forName("org.bukkit.inventory.meta.trim.TrimMaterial");
            return trimMaterialClass.getMethod("getMaterial", String.class).invoke(null, name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private Object getTrimPattern(String name) {
        try {
            Class<?> trimPatternClass = Class.forName("org.bukkit.inventory.meta.trim.TrimPattern");
            return trimPatternClass.getMethod("getPattern", String.class).invoke(null, name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private Object createTrim(Object trimMaterial, Object trimPattern) {
        try {
            Class<?> trimClass = Class.forName("org.bukkit.inventory.meta.ArmorMeta$Trim");
            return trimClass.getConstructor(trimMaterial.getClass(), trimPattern.getClass())
                    .newInstance(trimMaterial, trimPattern);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasConfiguration() {
        return material != null && pattern != null;
    }
}
