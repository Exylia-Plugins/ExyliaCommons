package net.exylia.commons.v2.scoreboard.config;

import lombok.experimental.UtilityClass;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ScoreboardLoader {

    public Scoreboard load(ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("ConfigurationSection cannot be null");
        }

        String title = section.getString("title");
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Scoreboard title is required");
        }

        List<String> rawLines = section.getStringList("lines");
        if (rawLines.isEmpty()) {
            throw new IllegalArgumentException("Scoreboard must have at least one line");
        }

        if (rawLines.size() > 15) {
            throw new IllegalArgumentException("Scoreboard cannot have more than 15 lines");
        }

        List<ScoreboardLine> lines = rawLines.stream()
                .map(line -> ScoreboardLine.of(rawLines.indexOf(line), line))
                .collect(Collectors.toList());

        UpdateConfig updateConfig = loadUpdateConfig(section);
        TeamConfig teamConfig = loadTeamConfig(section);
        boolean enabled = section.getBoolean("enabled", true);

        return Scoreboard.builder()
                .title(title)
                .lines(lines)
                .updateConfig(updateConfig)
                .teamConfig(teamConfig)
                .enabled(enabled)
                .build();
    }

    private UpdateConfig loadUpdateConfig(ConfigurationSection section) {
        ConfigurationSection updateSection = section.getConfigurationSection("update");
        if (updateSection == null) {
            return UpdateConfig.defaults();
        }

        long updateInterval = updateSection.getLong("interval", 20L);
        boolean smartUpdate = updateSection.getBoolean("smart", true);
        boolean cacheEnabled = updateSection.getBoolean("cache", true);

        return UpdateConfig.builder()
                .updateInterval(updateInterval)
                .smartUpdate(smartUpdate)
                .cacheEnabled(cacheEnabled)
                .build();
    }

    private TeamConfig loadTeamConfig(ConfigurationSection section) {
        ConfigurationSection teamSection = section.getConfigurationSection("team");
        if (teamSection == null) {
            return null;
        }

        String name = teamSection.getString("name", "main");
        String prefix = teamSection.getString("prefix", "");
        String suffix = teamSection.getString("suffix", "");

        ChatColor color = ChatColor.WHITE;
        String colorString = teamSection.getString("color");
        if (colorString != null) {
            try {
                color = ChatColor.valueOf(colorString.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Team.OptionStatus collisionRule = parseOptionStatus(
                teamSection.getString("collision-rule"),
                Team.OptionStatus.ALWAYS
        );

        Team.OptionStatus nametagVisibility = parseOptionStatus(
                teamSection.getString("nametag-visibility"),
                Team.OptionStatus.ALWAYS
        );

        boolean friendlyFire = teamSection.getBoolean("friendly-fire", true);
        boolean seeFriendlyInvisibles = teamSection.getBoolean("see-friendly-invisibles", true);

        return TeamConfig.builder()
                .name(name)
                .prefix(prefix)
                .suffix(suffix)
                .color(color)
                .collisionRule(collisionRule)
                .nametagVisibility(nametagVisibility)
                .friendlyFire(friendlyFire)
                .seeFriendlyInvisibles(seeFriendlyInvisibles)
                .build();
    }

    private Team.OptionStatus parseOptionStatus(String value, Team.OptionStatus defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return Team.OptionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
