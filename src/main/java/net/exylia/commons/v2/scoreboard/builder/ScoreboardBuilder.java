package net.exylia.commons.v2.scoreboard.builder;

import net.exylia.commons.v2.scoreboard.config.TeamConfig;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardBuilder {

    private String title = "";
    private final List<String> lines = new ArrayList<>();
    private TeamConfig teamConfig;
    private long updateInterval = 20L;
    private boolean enabled = true;

    private boolean hasTeam = false;
    private String teamName = "main";
    private String teamPrefix = "";
    private String teamSuffix = "";
    private ChatColor teamColor = ChatColor.WHITE;

    public static ScoreboardBuilder create() {
        return new ScoreboardBuilder();
    }

    public ScoreboardBuilder title(String title) {
        this.title = title != null ? title : "";
        return this;
    }

    public ScoreboardBuilder line(String line) {
        if (line != null) {
            this.lines.add(line);
        }
        return this;
    }

    public ScoreboardBuilder lines(String... lines) {
        if (lines != null) {
            this.lines.addAll(Arrays.asList(lines));
        }
        return this;
    }

    public ScoreboardBuilder lines(List<String> lines) {
        if (lines != null) {
            this.lines.addAll(lines);
        }
        return this;
    }

    public ScoreboardBuilder updateInterval(long ticks) {
        this.updateInterval = Math.max(1L, ticks);
        return this;
    }

    public ScoreboardBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ScoreboardBuilder withTeam() {
        this.hasTeam = true;
        return this;
    }

    public ScoreboardBuilder teamName(String name) {
        this.teamName = name != null ? name : "main";
        this.hasTeam = true;
        return this;
    }

    public ScoreboardBuilder teamPrefix(String prefix) {
        this.teamPrefix = prefix != null ? prefix : "";
        this.hasTeam = true;
        return this;
    }

    public ScoreboardBuilder teamSuffix(String suffix) {
        this.teamSuffix = suffix != null ? suffix : "";
        this.hasTeam = true;
        return this;
    }

    public ScoreboardBuilder teamColor(ChatColor color) {
        this.teamColor = color != null ? color : ChatColor.WHITE;
        this.hasTeam = true;
        return this;
    }

    public Scoreboard build() {
        validate();

        UpdateConfig updateConfig = UpdateConfig.builder()
                .updateInterval(updateInterval)
                .build();

        TeamConfig finalTeamConfig = null;
        if (hasTeam) {
            finalTeamConfig = TeamConfig.builder()
                    .name(teamName)
                    .prefix(teamPrefix)
                    .suffix(teamSuffix)
                    .color(teamColor)
                    .build();
        }

        List<ScoreboardLine> scoreboardLines = lines.stream()
                .map(line -> ScoreboardLine.of(lines.indexOf(line), line))
                .collect(Collectors.toList());

        return Scoreboard.builder()
                .title(title)
                .lines(scoreboardLines)
                .teamConfig(finalTeamConfig)
                .updateConfig(updateConfig)
                .enabled(enabled)
                .build();
    }

    private void validate() {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Scoreboard title cannot be null or empty");
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Scoreboard must have at least one line");
        }

        if (lines.size() > 15) {
            throw new IllegalArgumentException("Scoreboard cannot have more than 15 lines");
        }

        if (updateInterval < 1) {
            throw new IllegalArgumentException("Update interval must be at least 1 tick");
        }
    }
}
