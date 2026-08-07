package net.exylia.commons.v2.scoreboard.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.scoreboard.config.ScoreboardConfig;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScoreboardConfigSerializer implements ConfigSerializer<ScoreboardConfig> {

    @Override
    public Object serialize(ScoreboardConfig value) {
        UpdateConfig update = value.getUpdateConfig() == null ? UpdateConfig.defaults() : value.getUpdateConfig();
        Map<String, Object> updateMap = new LinkedHashMap<>();
        updateMap.put("interval", update.getUpdateInterval());
        updateMap.put("smart", update.isSmartUpdate());
        updateMap.put("cache", update.isCacheEnabled());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("update", updateMap);
        return map;
    }

    @Override
    public ScoreboardConfig deserialize(ConfigurationSection section) {
        if (section == null) return ScoreboardConfig.defaults();
        ConfigurationSection update = section.getConfigurationSection("update");
        if (update == null) update = section.getConfigurationSection("Update");
        if (update == null) return ScoreboardConfig.defaults();

        return ScoreboardConfig.builder()
                .updateConfig(UpdateConfig.builder()
                        .updateInterval(Math.max(1L, update.getLong("interval", update.getLong("Interval", 20L))))
                        .smartUpdate(update.getBoolean("smart", update.getBoolean("Smart", true)))
                        .cacheEnabled(update.getBoolean("cache", update.getBoolean("Cache", true)))
                        .build())
                .build();
    }

    @Override
    public Class<ScoreboardConfig> getType() {
        return ScoreboardConfig.class;
    }
}
