package net.exylia.commons.v2.reward.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Reward {
    private final String id;
    private final RewardType type;
    private final Object data;

    @Builder.Default
    private final double chance = 100.0;

    private final String condition;
    private final String message;

    @Builder.Default
    private final int priority = 0;

    @Builder.Default
    private final boolean async = false;
}
