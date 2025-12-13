package net.exylia.commons.v2.scoreboard.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@With
@AllArgsConstructor
public class ScoreboardConfig {

    @Builder.Default
    private final UpdateConfig updateConfig = UpdateConfig.defaults();

    private final TeamConfig teamConfig;

    public static ScoreboardConfig defaults() {
        return ScoreboardConfig.builder().build();
    }

    public static ScoreboardConfig withTeam(TeamConfig teamConfig) {
        return ScoreboardConfig.builder()
                .teamConfig(teamConfig)
                .build();
    }

    public boolean hasTeam() {
        return teamConfig != null;
    }
}
