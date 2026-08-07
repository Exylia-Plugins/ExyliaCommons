package net.exylia.commons.v2.scoreboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ScoreboardStats {

    private final int activeScoreboards;
    private final double cacheHitRate;
    private final long totalUpdates;
    private final double averageRenderTimeMs;

    public static ScoreboardStats empty() {
        return new ScoreboardStats(0, 0.0, 0, 0.0);
    }
}
