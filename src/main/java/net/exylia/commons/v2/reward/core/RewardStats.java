package net.exylia.commons.v2.reward.core;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RewardStats {

    @Builder.Default
    private final long totalRewardsGiven = 0;

    @Builder.Default
    private final long totalRewardsFailed = 0;

    public double getSuccessRate() {
        long total = getTotalRewardsProcessed();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalRewardsGiven / total * 100.0;
    }

    public long getTotalRewardsProcessed() {
        return totalRewardsGiven + totalRewardsFailed;
    }
}
