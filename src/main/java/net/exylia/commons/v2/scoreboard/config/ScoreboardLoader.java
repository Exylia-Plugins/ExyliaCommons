package net.exylia.commons.v2.scoreboard.config;

import lombok.experimental.UtilityClass;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        List<ScoreboardLine> lines = IntStream.range(0, rawLines.size())
                .mapToObj(i -> ScoreboardLine.of(i, rawLines.get(i)))
                .collect(Collectors.toList());

        UpdateConfig updateConfig = loadUpdateConfig(section);
        boolean enabled = section.getBoolean("enabled", true);

        return Scoreboard.builder()
                .title(title)
                .lines(lines)
                .updateConfig(updateConfig)
                .enabled(enabled)
                .build();
    }

    private UpdateConfig loadUpdateConfig(ConfigurationSection section) {
        ConfigurationSection updateSection = section.getConfigurationSection("update");
        if (updateSection == null) {
            return UpdateConfig.defaults();
        }

        return UpdateConfig.builder()
                .updateInterval(updateSection.getLong("interval", 20L))
                .smartUpdate(updateSection.getBoolean("smart", true))
                .cacheEnabled(updateSection.getBoolean("cache", true))
                .build();
    }
}
