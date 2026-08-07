package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.scoreboard.config.ScoreboardConfig;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScoreboardConfigSerializer implements ConfigSerializer<ScoreboardConfig> {

    @Override
    public Object serialize(ScoreboardConfig value) {
        Map<String, Object> map = new LinkedHashMap<>();

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("update-interval", value.getUpdateConfig().getUpdateInterval());
        update.put("smart-update", value.getUpdateConfig().isSmartUpdate());
        update.put("cache-enabled", value.getUpdateConfig().isCacheEnabled());
        map.put("update", update);

        return map;
    }

    @Override
    public ScoreboardConfig deserialize(ConfigurationSection section) {
        UpdateConfig updateConfig = UpdateConfig.defaults();
        ConfigurationSection updateSection = section.getConfigurationSection("update");
        if (updateSection != null) {
            updateConfig = UpdateConfig.builder()
                    .updateInterval(updateSection.getLong("update-interval", 20L))
                    .smartUpdate(updateSection.getBoolean("smart-update", true))
                    .cacheEnabled(updateSection.getBoolean("cache-enabled", true))
                    .build();
        }

        return ScoreboardConfig.builder()
                .updateConfig(updateConfig)
                .build();
    }

    @Override
    public Class<ScoreboardConfig> getType() {
        return ScoreboardConfig.class;
    }
}
