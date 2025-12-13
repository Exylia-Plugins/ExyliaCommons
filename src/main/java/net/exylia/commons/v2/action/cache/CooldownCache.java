package net.exylia.commons.v2.action.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.time.Duration;
import java.util.UUID;

public class CooldownCache {
    private final Cache<ActionCacheKey, Long> cache;

    public CooldownCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    public void setCooldown(UUID playerId, String actionId, long durationMillis) {
        ActionCacheKey key = new ActionCacheKey(playerId, actionId);
        long expiryTime = System.currentTimeMillis() + durationMillis;
        cache.put(key, expiryTime);
    }

    public boolean isOnCooldown(UUID playerId, String actionId) {
        ActionCacheKey key = new ActionCacheKey(playerId, actionId);
        Long expiry = cache.getIfPresent(key);

        if (expiry == null) {
            return false;
        }

        if (System.currentTimeMillis() > expiry) {
            cache.invalidate(key);
            return false;
        }

        return true;
    }

    public long getRemainingMillis(UUID playerId, String actionId) {
        ActionCacheKey key = new ActionCacheKey(playerId, actionId);
        Long expiry = cache.getIfPresent(key);

        if (expiry == null) {
            return 0;
        }

        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void invalidate(UUID playerId, String actionId) {
        ActionCacheKey key = new ActionCacheKey(playerId, actionId);
        cache.invalidate(key);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    public long size() {
        return cache.estimatedSize();
    }
}
