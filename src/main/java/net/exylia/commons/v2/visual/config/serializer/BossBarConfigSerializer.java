package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class BossBarConfigSerializer implements ConfigSerializer<BossBarConfig> {

    @Override
    public Object serialize(BossBarConfig value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", value.getText());
        map.put("color", value.getColor().name());
        map.put("style", value.getStyle().name());
        map.put("update-interval", value.getUpdateInterval());
        return map;
    }

    @Override
    public BossBarConfig deserialize(ConfigurationSection section) {
        return BossBarConfig.builder(section.getString("text", ""))
                .color(parseEnum(BossBar.Color.class, section.getString("color"), BossBar.Color.BLUE))
                .style(parseEnum(BossBar.Overlay.class, section.getString("style"), BossBar.Overlay.PROGRESS))
                .updateInterval(section.getLong("update-interval", 20L))
                .build();
    }

    @Override
    public Class<BossBarConfig> getType() {
        return BossBarConfig.class;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(clazz, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
