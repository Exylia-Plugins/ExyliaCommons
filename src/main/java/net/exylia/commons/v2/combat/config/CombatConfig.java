package net.exylia.commons.v2.combat.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class CombatConfig {

    private boolean enableCache = true;
    private Duration combatDataCacheTTL = Duration.ofMinutes(2);
    private int combatDataCacheMaxSize = 5000;
    private boolean autoDetectProvider = true;
    private String preferredProvider = "auto";

    public CombatConfig() {
    }
}
