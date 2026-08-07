package net.exylia.commons.v2.scoreboard.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.scoreboard.config.ScoreboardLoader;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScoreboardSerializer implements ConfigSerializer<Scoreboard> {

    @Override
    public Object serialize(Scoreboard value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", value.isEnabled());
        map.put("title", value.getTitle());
        map.put("lines", value.getLines().stream()
                .sorted(Comparator.comparingInt(ScoreboardLine::getPosition))
                .map(ScoreboardLine::getContent)
                .toList());

        UpdateConfig update = value.getEffectiveUpdateConfig();
        Map<String, Object> updateMap = new LinkedHashMap<>();
        updateMap.put("interval", update.getUpdateInterval());
        updateMap.put("smart", update.isSmartUpdate());
        updateMap.put("cache", update.isCacheEnabled());
        map.put("update", updateMap);
        return map;
    }

    @Override
    public Scoreboard deserialize(ConfigurationSection section) {
        return ScoreboardLoader.load(section);
    }

    @Override
    public Class<Scoreboard> getType() {
        return Scoreboard.class;
    }
}
