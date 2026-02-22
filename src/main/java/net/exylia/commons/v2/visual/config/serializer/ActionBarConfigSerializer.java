package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class ActionBarConfigSerializer implements ConfigSerializer<ActionBarConfig> {

    @Override
    public Object serialize(ActionBarConfig value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", value.getText());
        map.put("update-interval", value.getUpdateInterval());
        return map;
    }

    @Override
    public ActionBarConfig deserialize(ConfigurationSection section) {
        return ActionBarConfig.builder(section.getString("text", ""))
                .updateInterval(section.getLong("update-interval", 20L))
                .build();
    }

    @Override
    public Class<ActionBarConfig> getType() {
        return ActionBarConfig.class;
    }
}
