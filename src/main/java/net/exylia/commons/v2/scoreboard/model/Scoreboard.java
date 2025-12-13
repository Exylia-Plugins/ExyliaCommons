package net.exylia.commons.v2.scoreboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import net.exylia.commons.v2.scoreboard.config.ScoreboardConfig;
import net.exylia.commons.v2.scoreboard.config.TeamConfig;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class Scoreboard {

    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    private final String title;
    private final List<ScoreboardLine> lines;
    private final TeamConfig teamConfig;
    private final UpdateConfig updateConfig;

    @Builder.Default
    private final boolean enabled = true;

    public static Scoreboard from(ScoreboardConfig config, String title, List<ScoreboardLine> lines) {
        return Scoreboard.builder()
                .title(title)
                .lines(lines)
                .teamConfig(config.getTeamConfig())
                .updateConfig(config.getUpdateConfig())
                .build();
    }

    public boolean hasTeam() {
        return teamConfig != null;
    }

    public long getUpdateInterval() {
        return updateConfig != null ? updateConfig.getUpdateInterval() : UpdateConfig.defaults().getUpdateInterval();
    }
}
