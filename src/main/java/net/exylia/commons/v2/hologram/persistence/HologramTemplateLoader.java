package net.exylia.commons.v2.hologram.persistence;

import net.exylia.commons.v2.hologram.model.HologramConfig;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.hologram.model.HologramTemplate;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HologramTemplateLoader {

    public static HologramTemplate fromConfig(ConfigurationSection section) {
        return fromConfigAsync(section).join();
    }

    public static CompletableFuture<HologramTemplate> fromConfigAsync(ConfigurationSection section) {
        return CompletableFuture.supplyAsync(() -> {
            HologramTemplate.HologramTemplateBuilder builder = HologramTemplate.builder();

            builder.enabled(section.getBoolean("enabled", true));

            builder.lines(loadLines(section));
            builder.properties(loadProperties(section));
            builder.config(loadConfig(section));

            if (section.contains("persistent")) {
                builder.persistent(section.getBoolean("persistent"));
            } else {
                builder.persistent(false);
            }

            if (section.contains("perPlayer") || section.contains("per-player")) {
                String key = section.contains("perPlayer") ? "perPlayer" : "per-player";
                builder.perPlayer(section.getBoolean(key));
            } else {
                builder.perPlayer(false);
            }

            if (section.contains("viewDistance") || section.contains("view-distance")) {
                String key = section.contains("viewDistance") ? "viewDistance" : "view-distance";
                builder.viewDistance(section.getDouble(key));
            } else {
                builder.viewDistance(50.0);
            }

            ConfigurationSection offsetSection = section.getConfigurationSection("offset");
            if (offsetSection != null) {
                builder.offsetX(offsetSection.getDouble("x", 0.0));
                builder.offsetY(offsetSection.getDouble("y", 0.0));
                builder.offsetZ(offsetSection.getDouble("z", 0.0));
            } else {
                builder.offsetX(0.0);
                builder.offsetY(0.0);
                builder.offsetZ(0.0);
            }

            builder.visibilityCondition(null);

            return builder.build();
        });
    }

    private static List<String> loadLines(ConfigurationSection section) {
        List<String> lines = section.getStringList("lines");
        if (lines.isEmpty()) {
            String singleLine = section.getString("line");
            if (singleLine != null) {
                lines = new ArrayList<>();
                lines.add(singleLine);
            } else {
                lines = new ArrayList<>();
            }
        }
        return lines;
    }

    private static HologramProperties loadProperties(ConfigurationSection section) {
        HologramProperties.HologramPropertiesBuilder builder = HologramProperties.builder();

        ConfigurationSection props = section.getConfigurationSection("properties");
        if (props == null) {
            return builder.build();
        }

        if (props.contains("billboard")) {
            String billboardStr = props.getString("billboard");
            try {
                Display.Billboard billboard = Display.Billboard.valueOf(billboardStr.toUpperCase());
                builder.billboard(billboard);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid billboard value: " + billboardStr);
            }
        }

        if (props.contains("alignment")) {
            String alignmentStr = props.getString("alignment");
            try {
                TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.valueOf(alignmentStr.toUpperCase());
                builder.alignment(alignment);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid alignment value: " + alignmentStr);
            }
        }

        if (props.contains("scale")) {
            Object scaleValue = props.get("scale");
            if (scaleValue instanceof Number) {
                float scale = ((Number) scaleValue).floatValue();
                builder.scaleX(scale).scaleY(scale).scaleZ(scale);
            } else {
                ConfigurationSection scaleSection = props.getConfigurationSection("scale");
                if (scaleSection != null) {
                    float x = (float) scaleSection.getDouble("x", 1.0);
                    float y = (float) scaleSection.getDouble("y", 1.0);
                    float z = (float) scaleSection.getDouble("z", 1.0);
                    builder.scaleX(x).scaleY(y).scaleZ(z);
                }
            }
        }

        if (props.contains("shadow")) {
            builder.shadow(props.getBoolean("shadow"));
        }

        if (props.contains("seeThrough") || props.contains("see-through")) {
            String key = props.contains("seeThrough") ? "seeThrough" : "see-through";
            builder.seeThrough(props.getBoolean(key));
        }

        if (props.contains("lineWidth") || props.contains("line-width")) {
            String key = props.contains("lineWidth") ? "lineWidth" : "line-width";
            builder.lineWidth(props.getInt(key));
        }

        String bgColorKey = props.contains("backgroundColor") ? "backgroundColor" :
                           props.contains("background-color") ? "background-color" : null;

        if (bgColorKey != null) {
            Object bgColorValue = props.get(bgColorKey);
            if (bgColorValue instanceof String) {
                String hexColor = (String) bgColorValue;
                if (hexColor.equalsIgnoreCase("transparent") || hexColor.equalsIgnoreCase("none")) {
                    builder.backgroundColor(null);
                    builder.backgroundAlpha(0);
                } else {
                    if (hexColor.startsWith("#")) {
                        hexColor = hexColor.substring(1);
                    }
                    try {
                        if (hexColor.length() == 8) {
                            long argb = Long.parseLong(hexColor, 16);
                            int alpha = (int) ((argb >> 24) & 0xFF);
                            int rgb = (int) (argb & 0xFFFFFF);
                            builder.backgroundColor(Color.fromRGB(rgb));
                            builder.backgroundAlpha(alpha);
                        } else if (hexColor.length() == 6) {
                            int rgb = Integer.parseInt(hexColor, 16);
                            builder.backgroundColor(Color.fromRGB(rgb));
                        } else {
                            throw new IllegalArgumentException("Invalid hex color length: " + bgColorValue);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid hex color value: " + bgColorValue);
                    }
                }
            } else {
                ConfigurationSection bgColor = props.getConfigurationSection(bgColorKey);
                if (bgColor != null) {
                    int r = bgColor.getInt("r", 0);
                    int g = bgColor.getInt("g", 0);
                    int b = bgColor.getInt("b", 0);
                    int a = bgColor.getInt("a", 255);
                    builder.backgroundColor(Color.fromRGB(r, g, b));
                    builder.backgroundAlpha(a);
                }
            }
        }

        String bgAlphaKey = props.contains("backgroundAlpha") ? "backgroundAlpha" :
                           props.contains("background-alpha") ? "background-alpha" : null;
        if (bgAlphaKey != null) {
            builder.backgroundAlpha(props.getInt(bgAlphaKey));
        }

        String textOpacityKey = props.contains("textOpacity") ? "textOpacity" :
                               props.contains("text-opacity") ? "text-opacity" : null;
        if (textOpacityKey != null) {
            builder.textOpacity((byte) props.getInt(textOpacityKey));
        }

        String defaultBgKey = props.contains("defaultBackground") ? "defaultBackground" :
                             props.contains("default-background") ? "default-background" : null;
        if (defaultBgKey != null) {
            builder.defaultBackground(props.getBoolean(defaultBgKey));
        }

        if (props.contains("lineSpacing") || props.contains("line-spacing")) {
            String key = props.contains("lineSpacing") ? "lineSpacing" : "line-spacing";
            builder.lineSpacing(props.getDouble(key));
        }

        if (props.contains("brightness")) {
            builder.brightness(props.getInt("brightness"));
        }

        return builder.build();
    }

    private static HologramConfig loadConfig(ConfigurationSection section) {
        HologramConfig.HologramConfigBuilder builder = HologramConfig.builder();

        ConfigurationSection config = section.getConfigurationSection("config");
        if (config == null) {
            return builder.build();
        }

        if (config.contains("updateInterval") || config.contains("update-interval")) {
            String key = config.contains("updateInterval") ? "updateInterval" : "update-interval";
            builder.updateInterval(config.getLong(key));
        }

        if (config.contains("autoUpdate") || config.contains("auto-update")) {
            String key = config.contains("autoUpdate") ? "autoUpdate" : "auto-update";
            builder.autoUpdate(config.getBoolean(key));
        }

        if (config.contains("spawnOnChunkLoad") || config.contains("spawn-on-chunk-load")) {
            String key = config.contains("spawnOnChunkLoad") ? "spawnOnChunkLoad" : "spawn-on-chunk-load";
            builder.spawnOnChunkLoad(config.getBoolean(key));
        }

        if (config.contains("removeOnChunkUnload") || config.contains("remove-on-chunk-unload")) {
            String key = config.contains("removeOnChunkUnload") ? "removeOnChunkUnload" : "remove-on-chunk-unload";
            builder.removeOnChunkUnload(config.getBoolean(key));
        }

        return builder.build();
    }

    public static void saveToConfig(HologramTemplate template, ConfigurationSection section) {
        section.set("enabled", template.isEnabled());
        section.set("lines", template.getLines());

        if (template.getProperties() != null) {
            saveProperties(template.getProperties(), section.createSection("properties"));
        }

        if (template.getConfig() != null) {
            saveConfig(template.getConfig(), section.createSection("config"));
        }

        section.set("persistent", template.isPersistent());
        section.set("perPlayer", template.isPerPlayer());
        section.set("viewDistance", template.getViewDistance());

        if (template.getOffsetX() != 0.0 || template.getOffsetY() != 0.0 || template.getOffsetZ() != 0.0) {
            ConfigurationSection offsetSection = section.createSection("offset");
            offsetSection.set("x", template.getOffsetX());
            offsetSection.set("y", template.getOffsetY());
            offsetSection.set("z", template.getOffsetZ());
        }
    }

    private static void saveProperties(HologramProperties properties, ConfigurationSection section) {
        section.set("billboard", properties.getBillboard().name());
        section.set("alignment", properties.getAlignment().name());

        ConfigurationSection scaleSection = section.createSection("scale");
        scaleSection.set("x", properties.getScaleX());
        scaleSection.set("y", properties.getScaleY());
        scaleSection.set("z", properties.getScaleZ());

        section.set("shadow", properties.isShadow());
        section.set("seeThrough", properties.isSeeThrough());
        section.set("lineWidth", properties.getLineWidth());

        if (properties.getBackgroundColor() != null) {
            Color color = properties.getBackgroundColor();
            int alpha = properties.getBackgroundAlpha();
            if (alpha < 255) {
                int argb = (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                String hexColor = String.format("#%08X", argb);
                section.set("backgroundColor", hexColor);
            } else {
                int rgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                String hexColor = String.format("#%06X", rgb);
                section.set("backgroundColor", hexColor);
            }
        }

        if (properties.getTextOpacity() != (byte) 255) {
            section.set("textOpacity", (int) properties.getTextOpacity() & 0xFF);
        }

        if (properties.isDefaultBackground()) {
            section.set("defaultBackground", true);
        }

        section.set("lineSpacing", properties.getLineSpacing());
        section.set("brightness", properties.getBrightness());
    }

    private static void saveConfig(HologramConfig config, ConfigurationSection section) {
        section.set("updateInterval", config.getUpdateInterval());
        section.set("autoUpdate", config.isAutoUpdate());
        section.set("spawnOnChunkLoad", config.isSpawnOnChunkLoad());
        section.set("removeOnChunkUnload", config.isRemoveOnChunkUnload());
    }
}
