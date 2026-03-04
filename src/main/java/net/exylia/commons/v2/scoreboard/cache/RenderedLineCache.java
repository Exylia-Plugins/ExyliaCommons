package net.exylia.commons.v2.scoreboard.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class RenderedLineCache {

    private final Cache<LineCacheKey, String> cache;

    public RenderedLineCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(2000)
                .recordStats()
                .build();
    }

    public String get(LineCacheKey key) {
        return cache.getIfPresent(key);
    }

    public void put(LineCacheKey key, String value) {
        cache.put(key, value);
    }

    public void invalidate(LineCacheKey key) {
        cache.invalidate(key);
    }

    public void clear() {
        cache.invalidateAll();
    }

    public long size() {
        return cache.estimatedSize();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }
}
