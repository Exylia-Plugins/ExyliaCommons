package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.hologram.model.HologramConfig;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.hologram.model.HologramTemplate;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.LinkedHashMap;
import java.util.Map;

public class HologramTemplateSerializer implements ConfigSerializer<HologramTemplate> {

    public static final String TRANSPARENT = "#00000000";

    @Override
    public Object serialize(HologramTemplate value) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("lines", value.getLines());
        map.put("enabled", value.isEnabled());
        map.put("persistent", value.isPersistent());
        map.put("per-player", value.isPerPlayer());
        map.put("view-distance", value.getViewDistance());
        map.put("offset-x", value.getOffsetX());
        map.put("offset-y", value.getOffsetY());
        map.put("offset-z", value.getOffsetZ());

        if (value.getProperties() != null) {
            HologramProperties p = value.getProperties();
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("billboard", p.getBillboard().name());
            props.put("alignment", p.getAlignment().name());
            props.put("scale-x", p.getScaleX());
            props.put("scale-y", p.getScaleY());
            props.put("scale-z", p.getScaleZ());
            props.put("shadow", p.isShadow());
            props.put("see-through", p.isSeeThrough());
            props.put("line-width", p.getLineWidth());
            props.put("line-spacing", p.getLineSpacing());
            props.put("text-opacity", (int) p.getTextOpacity());
            props.put("default-background", p.isDefaultBackground());
            props.put("brightness", p.getBrightness());
            if (p.getBackgroundColor() != null) {
                int argb = (p.getBackgroundAlpha() << 24) | (p.getBackgroundColor().asRGB() & 0xFFFFFF);
                props.put("background-color", String.format("#%08X", argb));
            }
            map.put("properties", props);
        }

        if (value.getConfig() != null) {
            HologramConfig c = value.getConfig();
            Map<String, Object> cfg = new LinkedHashMap<>();
            cfg.put("update-interval", c.getUpdateInterval());
            cfg.put("auto-update", c.isAutoUpdate());
            cfg.put("spawn-on-chunk-load", c.isSpawnOnChunkLoad());
            cfg.put("remove-on-chunk-unload", c.isRemoveOnChunkUnload());
            map.put("config", cfg);
        }

        return map;
    }

    @Override
    public HologramTemplate deserialize(ConfigurationSection section) {
        HologramTemplate.HologramTemplateBuilder builder = HologramTemplate.builder()
                .enabled(section.getBoolean("enabled", true))
                .persistent(section.getBoolean("persistent", false))
                .perPlayer(section.getBoolean("per-player", false))
                .viewDistance(section.getDouble("view-distance", 48.0))
                .offsetX(section.getDouble("offset-x", 0.0))
                .offsetY(section.getDouble("offset-y", 0.0))
                .offsetZ(section.getDouble("offset-z", 0.0))
                .lines(section.getStringList("lines"));

        ConfigurationSection propsSection = section.getConfigurationSection("properties");
        if (propsSection != null) {
            HologramProperties.HologramPropertiesBuilder props = HologramProperties.builder()
                    .billboard(parseEnum(Display.Billboard.class, propsSection.getString("billboard"), Display.Billboard.CENTER))
                    .alignment(parseEnum(TextDisplay.TextAlignment.class, propsSection.getString("alignment"), TextDisplay.TextAlignment.CENTER))
                    .scaleX((float) propsSection.getDouble("scale-x", 1.0))
                    .scaleY((float) propsSection.getDouble("scale-y", 1.0))
                    .scaleZ((float) propsSection.getDouble("scale-z", 1.0))
                    .shadow(propsSection.getBoolean("shadow", true))
                    .seeThrough(propsSection.getBoolean("see-through", false))
                    .lineWidth(propsSection.getInt("line-width", 200))
                    .lineSpacing(propsSection.getDouble("line-spacing", 0.25))
                    .textOpacity((byte) propsSection.getInt("text-opacity", 255))
                    .defaultBackground(propsSection.getBoolean("default-background", false))
                    .brightness(propsSection.getInt("brightness", -1));

            String bgColor = propsSection.getString("background-color");
            if (bgColor != null) {
                int[] argb = parseArgbColor(bgColor);
                props.backgroundColor(Color.fromRGB(argb[1], argb[2], argb[3]));
                props.backgroundAlpha(argb[0]);
            }

            builder.properties(props.build());
        } else {
            builder.properties(HologramProperties.defaultProperties());
        }

        ConfigurationSection cfgSection = section.getConfigurationSection("config");
        if (cfgSection != null) {
            builder.config(HologramConfig.builder()
                    .updateInterval(cfgSection.getLong("update-interval", 20L))
                    .autoUpdate(cfgSection.getBoolean("auto-update", true))
                    .spawnOnChunkLoad(cfgSection.getBoolean("spawn-on-chunk-load", true))
                    .removeOnChunkUnload(cfgSection.getBoolean("remove-on-chunk-unload", true))
                    .build());
        } else {
            builder.config(HologramConfig.defaultConfig());
        }

        return builder.build();
    }

    @Override
    public Class<HologramTemplate> getType() {
        return HologramTemplate.class;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(clazz, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private int[] parseArgbColor(String hex) {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            long value = Long.parseLong(clean, 16);
            if (clean.length() == 8) {
                int a = (int) ((value >> 24) & 0xFF);
                int r = (int) ((value >> 16) & 0xFF);
                int g = (int) ((value >> 8) & 0xFF);
                int b = (int) (value & 0xFF);
                return new int[]{a, r, g, b};
            } else {
                int r = (int) ((value >> 16) & 0xFF);
                int g = (int) ((value >> 8) & 0xFF);
                int b = (int) (value & 0xFF);
                return new int[]{255, r, g, b};
            }
        } catch (NumberFormatException e) {
            return new int[]{255, 0, 0, 0};
        }
    }
}
