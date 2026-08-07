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

    public static ScoreboardConfig defaults() {
        return ScoreboardConfig.builder().build();
    }
}
