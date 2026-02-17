package net.exylia.commons.v2.combat.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CombatStats {

    private final String providerName;
    private final long combatDataCacheSize;
    private final double combatDataCacheHitRate;

    @Override
    public String toString() {
        return String.format(
                "CombatStats{provider='%s', cache=[size=%d/%.2f%%]}",
                providerName,
                combatDataCacheSize, combatDataCacheHitRate * 100
        );
    }
}
