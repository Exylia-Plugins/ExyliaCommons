package net.exylia.commons.v2.clan.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClanStats {

    private final String providerName;
    private final int totalClans;
    private final long playerClanCacheSize;
    private final long clanDataCacheSize;
    private final double playerClanCacheHitRate;
    private final double clanDataCacheHitRate;

    @Override
    public String toString() {
        return String.format(
                "ClanStats{provider='%s', clans=%d, caches=[player=%d/%.2f%%, data=%d/%.2f%%]}",
                providerName, totalClans,
                playerClanCacheSize, playerClanCacheHitRate * 100,
                clanDataCacheSize, clanDataCacheHitRate * 100
        );
    }
}
