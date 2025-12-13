package net.exylia.commons.v2.scoreboard.cache;

import net.exylia.commons.cache.CacheStats;
import net.exylia.commons.cache.CaffeineCache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ScoreboardCacheManager {

    private final RenderedLineCache lineCache;
    private final CaffeineCache<String, String> placeholderCache;
    private final CaffeineCache<UUID, Map<String, String>> playerCache;

    public ScoreboardCacheManager() {
        this.lineCache = new RenderedLineCache();

        this.placeholderCache = CaffeineCache.<String, String>builder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats()
                .build();

        this.playerCache = CaffeineCache.<UUID, Map<String, String>>builder()
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .maximumSize(500)
                .recordStats()
                .build();
    }

    public String getLine(LineCacheKey key) {
        return lineCache.get(key);
    }

    public void cacheLine(LineCacheKey key, String value) {
        lineCache.put(key, value);
    }

    public void invalidateLine(LineCacheKey key) {
        lineCache.invalidate(key);
    }

    public String getPlaceholder(String key) {
        return placeholderCache.get(key);
    }

    public void cachePlaceholder(String key, String value) {
        placeholderCache.put(key, value);
    }

    public Map<String, String> getPlayerCache(UUID playerId) {
        return playerCache.get(playerId);
    }

    public void cachePlayer(UUID playerId, Map<String, String> data) {
        playerCache.put(playerId, data);
    }

    public void invalidatePlayer(UUID playerId) {
        playerCache.invalidate(playerId);
    }

    public void clearAll() {
        lineCache.clear();
        placeholderCache.invalidateAll();
        playerCache.invalidateAll();
    }

    public CacheStats getStats() {
        return placeholderCache.getStats();
    }

    public double getAverageHitRate() {
        double lineCacheHitRate = lineCache.hitRate();
        double placeholderCacheHitRate = placeholderCache.getStats().hitRate();
        double playerCacheHitRate = playerCache.getStats().hitRate();

        return (lineCacheHitRate + placeholderCacheHitRate + playerCacheHitRate) / 3.0;
    }
}
