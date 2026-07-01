package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.visual.builder.FireworkBuilder;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class FireworkConfigSerializer implements ConfigSerializer<FireworkConfig> {

    @Override
    public Object serialize(FireworkConfig value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", value.getType().name());
        map.put("colors", value.getColors().stream().map(FireworkConfigSerializer::colorToString).toList());
        map.put("fade-colors", value.getFadeColors().stream().map(FireworkConfigSerializer::colorToString).toList());
        map.put("flicker", value.isFlicker());
        map.put("trail", value.isTrail());
        map.put("power", value.getPower());
        return map;
    }

    @Override
    public FireworkConfig deserialize(ConfigurationSection section) {
        return FireworkBuilder.fromSection(section);
    }

    @Override
    public Class<FireworkConfig> getType() {
        return FireworkConfig.class;
    }

    static String colorToString(Color color) {
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(Color.GREEN)) return "GREEN";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(Color.YELLOW)) return "YELLOW";
        if (color.equals(Color.ORANGE)) return "ORANGE";
        if (color.equals(Color.PURPLE)) return "PURPLE";
        if (color.equals(Color.WHITE)) return "WHITE";
        if (color.equals(Color.BLACK)) return "BLACK";
        if (color.equals(Color.fromRGB(255, 215, 0))) return "GOLD";
        if (color.equals(Color.LIME)) return "LIME";
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
