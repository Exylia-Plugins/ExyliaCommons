package net.exylia.commons.v2.clan.cache;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ClanCacheManager {

    private final PlayerClanCache playerClanCache;
    private final ClanDataCache clanDataCache;

    public ClanCacheManager() {
        this.playerClanCache = new PlayerClanCache();
        this.clanDataCache = new ClanDataCache();
    }

    public void invalidateAll() {
        playerClanCache.invalidateAll();
        clanDataCache.invalidateAll();
    }

    public void invalidatePlayer(UUID playerId) {
        playerClanCache.invalidate(playerId);
    }

    public void invalidateClan(String identifier) {
        clanDataCache.invalidate(identifier);
    }
}
