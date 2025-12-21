package net.exylia.commons.v2.reward.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.reward.model.RewardType;

@Getter
@Builder
public class RewardConfig {
    private final RewardType type;
    private final Object rawData;

    @Builder.Default
    private final double chance = 100.0;

    private final String condition;
    private final String message;

    @Builder.Default
    private final int priority = 0;
}
