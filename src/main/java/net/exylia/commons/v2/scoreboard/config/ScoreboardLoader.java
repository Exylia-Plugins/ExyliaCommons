package net.exylia.commons.v2.scoreboard.config;

import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public final class ScoreboardLoader {

    private ScoreboardLoader() {
    }

    public static Scoreboard load(ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("ConfigurationSection cannot be null");

        String title = section.getString("title", section.getString("Title", ""));
        List<String> rawLines = getStringList(section, "lines", "Lines");
        boolean enabled = getBoolean(section, true, "enabled", "Enabled");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Scoreboard title is required at " + section.getCurrentPath());
        }
        if (rawLines.isEmpty()) {
            throw new IllegalArgumentException("Scoreboard lines are required at " + section.getCurrentPath());
        }
        if (rawLines.size() > 15) {
            throw new IllegalArgumentException("Scoreboard cannot have more than 15 lines at " + section.getCurrentPath());
        }

        ConfigurationSection update = section.getConfigurationSection("update");
        if (update == null) update = section.getConfigurationSection("Update");

        UpdateConfig updateConfig = loadUpdateConfig(update);
        List<ScoreboardLine> lines = IntStream.range(0, rawLines.size())
                .mapToObj(i -> ScoreboardLine.of(i, rawLines.get(i)))
                .toList();

        return Scoreboard.builder()
                .id(section.getString("id", section.getString("Id", UUID.randomUUID().toString())))
                .title(title)
                .lines(lines)
                .updateConfig(updateConfig)
                .enabled(enabled)
                .build();
    }

    private static UpdateConfig loadUpdateConfig(ConfigurationSection section) {
        if (section == null) return UpdateConfig.defaults();
        long interval = Math.max(1L, getLong(section, 20L, "interval", "Interval", "update-interval", "Update-Interval"));
        boolean smart = getBoolean(section, true, "smart", "Smart", "smart-update", "Smart-Update");
        boolean cache = getBoolean(section, true, "cache", "Cache", "cache-enabled", "Cache-Enabled");
        return UpdateConfig.builder()
                .updateInterval(interval)
                .smartUpdate(smart)
                .cacheEnabled(cache)
                .build();
    }

    private static List<String> getStringList(ConfigurationSection section, String... paths) {
        for (String path : paths) {
            if (section.contains(path)) return new ArrayList<>(section.getStringList(path));
        }
        return List.of();
    }

    private static boolean getBoolean(ConfigurationSection section, boolean fallback, String... paths) {
        for (String path : paths) {
            if (section.contains(path)) return section.getBoolean(path, fallback);
        }
        return fallback;
    }

    private static long getLong(ConfigurationSection section, long fallback, String... paths) {
        for (String path : paths) {
            if (section.contains(path)) return section.getLong(path, fallback);
        }
        return fallback;
    }
}
