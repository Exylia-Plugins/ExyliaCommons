package net.exylia.commons.v2.action.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitCache {
    private final Cache<ActionCacheKey, AtomicInteger> cache;

    public RateLimitCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(50000)
                .recordStats()
                .build();
    }

    public int increment(UUID playerId, String actionId) {
        ActionCacheKey key = new ActionCacheKey(playerId, actionId);
        AtomicInteger counter = cache.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet();
    }

    public int getCount(UUID playerId, String actionId) {
        ActionCacheKey key = new ActionCacheKey(playerId, actionId);
        AtomicInteger counter = cache.getIfPresent(key);
        return counter != null ? counter.get() : 0;
    }

    public void reset(UUID playerId, String actionId) {
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
