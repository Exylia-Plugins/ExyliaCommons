package net.exylia.commons.item.cooldown;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CooldownConfiguration {

    @Builder.Default
    private final int maxItemsInCooldown = -1;

    @Builder.Default
    private final double globalCooldownSeconds = -1.0;

    @Builder.Default
    private final long saveIntervalTicks = 1200L;

    @Builder.Default
    private final long cleanupIntervalTicks = 6000L;

    @Builder.Default
    private final String dataFileName = "cooldowns.json";

    public boolean hasMaxItemsLimit() {
        return maxItemsInCooldown > 0;
    }

    public boolean hasGlobalCooldown() {
        return globalCooldownSeconds > 0;
    }

    public static CooldownConfiguration getDefault() {
        return CooldownConfiguration.builder().build();
    }

    public static CooldownConfiguration withMaxItems(int maxItems) {
        return CooldownConfiguration.builder()
                .maxItemsInCooldown(maxItems)
                .build();
    }

    public static CooldownConfiguration withGlobalCooldown(double globalCooldownSeconds) {
        return CooldownConfiguration.builder()
                .globalCooldownSeconds(globalCooldownSeconds)
                .build();
    }

    public static CooldownConfiguration withMaxItemsAndGlobalCooldown(int maxItems, double globalCooldownSeconds) {
        return CooldownConfiguration.builder()
                .maxItemsInCooldown(maxItems)
                .globalCooldownSeconds(globalCooldownSeconds)
                .build();
    }
}