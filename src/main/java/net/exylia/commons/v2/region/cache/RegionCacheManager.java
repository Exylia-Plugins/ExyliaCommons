package net.exylia.commons.v2.region.cache;

import lombok.Getter;
import net.exylia.commons.cache.CaffeineCache;
import net.exylia.commons.v2.region.detection.SpatialIndex;
import net.exylia.commons.v2.region.model.RegionFlag;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class RegionCacheManager {
    private final SpatialIndex spatialIndex;
    private final CaffeineCache<FlagCacheKey, Boolean> flagCache;
    private final CaffeineCache<UUID, PlayerRegionState> playerStateCache;

    public RegionCacheManager(SpatialIndex spatialIndex) {
        this.spatialIndex = spatialIndex;

        this.flagCache = CaffeineCache.<FlagCacheKey, Boolean>builder()
                .expireAfterWrite(CacheStrategy.FLAG_CACHE.getDuration(), CacheStrategy.FLAG_CACHE.getTimeUnit())
                .maximumSize(CacheStrategy.FLAG_CACHE.getMaxSize())
                .recordStats()
                .build();

        this.playerStateCache = CaffeineCache.<UUID, PlayerRegionState>builder()
                .expireAfterAccess(CacheStrategy.PLAYER_STATE_CACHE.getDuration(), CacheStrategy.PLAYER_STATE_CACHE.getTimeUnit())
                .maximumSize(CacheStrategy.PLAYER_STATE_CACHE.getMaxSize())
                .recordStats()
                .build();
    }

    public List<Region> getRegionsAt(Location location) {
        return spatialIndex.getRegionsAt(location);
    }

    public void invalidateRegion(Region region) {
        flagCache.asMap().keySet().removeIf(key -> key.regionId.equals(region.getId()));
    }

    public Boolean getFlagValue(String regionId, UUID playerId, RegionFlag flag) {
        FlagCacheKey key = new FlagCacheKey(regionId, playerId, flag);
        return flagCache.get(key);
    }

    public void cacheFlagValue(String regionId, UUID playerId, RegionFlag flag, boolean value) {
        FlagCacheKey key = new FlagCacheKey(regionId, playerId, flag);
        flagCache.put(key, value);
    }

    public void invalidateFlag(String regionId, RegionFlag flag) {
        flagCache.asMap().keySet().removeIf(key -> key.regionId.equals(regionId) && key.flag == flag);
    }

    public void invalidatePlayerFlags(UUID playerId) {
        flagCache.asMap().keySet().removeIf(key -> key.playerId.equals(playerId));
    }

    public PlayerRegionState getPlayerState(UUID playerId) {
        return playerStateCache.get(playerId);
    }

    public void updatePlayerState(UUID playerId, PlayerRegionState state) {
        playerStateCache.put(playerId, state);
    }

    public void removePlayerState(UUID playerId) {
        playerStateCache.invalidate(playerId);
    }

    public CacheStats getStats() {
        return new CacheStats(
                flagCache.getStats(),
                playerStateCache.getStats()
        );
    }

    public void cleanup() {
        flagCache.cleanUp();
        playerStateCache.cleanUp();
    }

    public void invalidateAll() {
        flagCache.invalidateAll();
        playerStateCache.invalidateAll();
    }

    @Getter
    public static class PlayerRegionState {
        private final List<Region> regions;
        private final long lastUpdate;

        public PlayerRegionState(List<Region> regions) {
            this.regions = regions;
            this.lastUpdate = System.currentTimeMillis();
        }

        public long getTimeSinceUpdate() {
            return System.currentTimeMillis() - lastUpdate;
        }
    }

    private record FlagCacheKey(String regionId, UUID playerId, RegionFlag flag) {
    }

    @Getter
    public static class CacheStats {
        private final net.exylia.commons.cache.CacheStats flagStats;
        private final net.exylia.commons.cache.CacheStats playerStateStats;

        public CacheStats(net.exylia.commons.cache.CacheStats flagStats, net.exylia.commons.cache.CacheStats playerStateStats) {
            this.flagStats = flagStats;
            this.playerStateStats = playerStateStats;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{flags=%s, playerState=%s}", flagStats, playerStateStats);
        }
    }
}
