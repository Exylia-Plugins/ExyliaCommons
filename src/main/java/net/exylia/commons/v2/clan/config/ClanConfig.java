package net.exylia.commons.v2.clan.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class ClanConfig {

    private boolean enableCache = true;
    private Duration playerClanCacheTTL = Duration.ofMinutes(5);
    private Duration clanDataCacheTTL = Duration.ofMinutes(10);
    private int playerClanCacheMaxSize = 5000;
    private int clanDataCacheMaxSize = 2000;
    private boolean autoDetectProvider = true;
    private String preferredProvider = "auto";

    public ClanConfig() {
    }
}
