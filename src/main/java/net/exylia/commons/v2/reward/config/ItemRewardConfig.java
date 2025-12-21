package net.exylia.commons.v2.reward.config;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class ItemRewardConfig {
    private final String material;

    @Builder.Default
    private final int amount = 1;

    private final String name;

    @Builder.Default
    private final List<String> lore = new ArrayList<>();
}
