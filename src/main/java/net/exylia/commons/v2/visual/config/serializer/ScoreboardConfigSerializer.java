package net.exylia.commons.v2.visual.config.serializer;

import net.exylia.commons.v2.config.schema.ConfigSerializer;
import net.exylia.commons.v2.scoreboard.config.ScoreboardConfig;
import net.exylia.commons.v2.scoreboard.config.TeamConfig;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scoreboard.Team;

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

        if (value.hasTeam()) {
            TeamConfig team = value.getTeamConfig();
            Map<String, Object> teamMap = new LinkedHashMap<>();
            teamMap.put("name", team.getName());
            teamMap.put("prefix", team.getPrefix());
            teamMap.put("suffix", team.getSuffix());
            teamMap.put("color", team.getColor().name());
            teamMap.put("collision-rule", team.getCollisionRule().name());
            teamMap.put("nametag-visibility", team.getNametagVisibility().name());
            teamMap.put("friendly-fire", team.isFriendlyFire());
            teamMap.put("see-friendly-invisibles", team.isSeeFriendlyInvisibles());
            map.put("team", teamMap);
        }

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

        TeamConfig teamConfig = null;
        ConfigurationSection teamSection = section.getConfigurationSection("team");
        if (teamSection != null) {
            teamConfig = TeamConfig.builder()
                    .name(teamSection.getString("name", "main"))
                    .prefix(teamSection.getString("prefix", ""))
                    .suffix(teamSection.getString("suffix", ""))
                    .color(parseEnum(ChatColor.class, teamSection.getString("color"), ChatColor.WHITE))
                    .collisionRule(parseEnum(Team.OptionStatus.class, teamSection.getString("collision-rule"), Team.OptionStatus.ALWAYS))
                    .nametagVisibility(parseEnum(Team.OptionStatus.class, teamSection.getString("nametag-visibility"), Team.OptionStatus.ALWAYS))
                    .friendlyFire(teamSection.getBoolean("friendly-fire", true))
                    .seeFriendlyInvisibles(teamSection.getBoolean("see-friendly-invisibles", true))
                    .build();
        }

        return ScoreboardConfig.builder()
                .updateConfig(updateConfig)
                .teamConfig(teamConfig)
                .build();
    }

    @Override
    public Class<ScoreboardConfig> getType() {
        return ScoreboardConfig.class;
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
