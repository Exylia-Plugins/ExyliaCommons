package net.exylia.commons.v2.scoreboard.cache;

import net.exylia.commons.cache.CaffeineCache;

import java.util.concurrent.TimeUnit;

public class RenderedLineCache {

    private final CaffeineCache<LineCacheKey, String> cache;

    public RenderedLineCache() {
        this.cache = CaffeineCache.<LineCacheKey, String>builder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(2000)
                .recordStats()
                .build();
    }

    public String get(LineCacheKey key) {
        return cache.get(key);
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
        return cache.size();
    }

    public double hitRate() {
        return cache.getStats().hitRate();
    }
}
