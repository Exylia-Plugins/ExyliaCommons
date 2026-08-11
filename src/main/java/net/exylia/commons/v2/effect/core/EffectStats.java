package net.exylia.commons.v2.effect.core;

import lombok.Builder;
import lombok.Getter;

/** Runtime counters for the effect subsystem. Mirrors {@code RewardStats}. */
@Getter
@Builder
public class EffectStats {

    @Builder.Default
    private final long totalEffectsPlayed = 0;

    @Builder.Default
    private final long totalEffectsFailed = 0;

    @Builder.Default
    private final long totalEffectsSkipped = 0;

    public long getTotalEffectsProcessed() {
        return totalEffectsPlayed + totalEffectsFailed + totalEffectsSkipped;
    }

    public double getSuccessRate() {
        long total = getTotalEffectsProcessed();
        if (total == 0) return 0.0;
        return (double) totalEffectsPlayed / total * 100.0;
    }
}
