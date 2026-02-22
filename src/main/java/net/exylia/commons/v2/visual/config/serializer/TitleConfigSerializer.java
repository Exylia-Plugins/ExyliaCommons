package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.visual.config.TitleConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class TitleConfigSerializer implements ConfigSerializer<TitleConfig> {

    @Override
    public Object serialize(TitleConfig value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", value.getTitle());
        map.put("subtitle", value.getSubtitle());
        map.put("fade-in", value.getFadeIn());
        map.put("stay", value.getStay());
        map.put("fade-out", value.getFadeOut());
        map.put("update-interval", value.getUpdateInterval());
        return map;
    }

    @Override
    public TitleConfig deserialize(ConfigurationSection section) {
        return TitleConfig.builder()
                .title(section.getString("title", ""))
                .subtitle(section.getString("subtitle", ""))
                .fadeIn(section.getInt("fade-in", 10))
                .stay(section.getInt("stay", 70))
                .fadeOut(section.getInt("fade-out", 20))
                .updateInterval(section.getLong("update-interval", 20L))
                .build();
    }

    @Override
    public Class<TitleConfig> getType() {
        return TitleConfig.class;
    }
}
