package net.exylia.commons.v2.placeholders.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import java.util.concurrent.TimeUnit;

@Getter
public class PlaceholderCache {
    private final Cache<String, String> cache;
    private final Cache<String, String> nonCacheableCache;

    public PlaceholderCache(long defaultTtlMs) {
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
            String.format("Initializing placeholder cache with TTL: %dms", defaultTtlMs));
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(defaultTtlMs, TimeUnit.MILLISECONDS)
                .recordStats()
                .build();

        this.nonCacheableCache = Caffeine.newBuilder()
                .expireAfterWrite(100, TimeUnit.MILLISECONDS)
                .recordStats()
                .build();
    }

    public String get(String key, boolean cacheable) {
        Cache<String, String> targetCache = cacheable ? cache : nonCacheableCache;
        String value = targetCache.getIfPresent(key);
        if (value != null) {
            DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
                String.format("Cache HIT for key: %s (cacheable: %b)", key, cacheable));
        } else {
            DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
                String.format("Cache MISS for key: %s (cacheable: %b)", key, cacheable));
        }
        return value;
    }

    public void put(String key, String value, boolean cacheable) {
        Cache<String, String> targetCache = cacheable ? cache : nonCacheableCache;
        targetCache.put(key, value);
    }

    public String getOrCompute(String key, PlaceholderLoader loader, boolean cacheable) {
        Cache<String, String> targetCache = cacheable ? cache : nonCacheableCache;
        return targetCache.get(key, k -> {
            try {
                return loader.load();
            } catch (Exception e) {
                return null;
            }
        });
    }

    public void invalidate(String key) {
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Invalidating cache for key: " + key);
        cache.invalidate(key);
        nonCacheableCache.invalidate(key);
    }

    public void invalidateAll() {
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Invalidating all cache entries");
        cache.invalidateAll();
        nonCacheableCache.invalidateAll();
    }

    public void invalidatePattern(String pattern) {
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Invalidating cache for pattern: " + pattern);
        int beforeSize = cache.asMap().size() + nonCacheableCache.asMap().size();
        cache.asMap().keySet().removeIf(key -> key.contains(pattern));
        nonCacheableCache.asMap().keySet().removeIf(key -> key.contains(pattern));
        int afterSize = cache.asMap().size() + nonCacheableCache.asMap().size();
        int removed = beforeSize - afterSize;
        if (removed > 0) {
            DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
                String.format("Removed %d cache entries matching pattern: %s", removed, pattern));
        }
    }

    public long size() {
        return cache.estimatedSize() + nonCacheableCache.estimatedSize();
    }

    public PlaceholderCacheStats getStats() {
        return new PlaceholderCacheStats(
                cache.stats().hitCount(),
                cache.stats().missCount(),
                cache.stats().evictionCount(),
                nonCacheableCache.stats().hitCount(),
                nonCacheableCache.stats().missCount()
        );
    }

    public void cleanup() {
        cache.cleanUp();
        nonCacheableCache.cleanUp();
    }

    @FunctionalInterface
    public interface PlaceholderLoader {
        String load() throws Exception;
    }

    public record PlaceholderCacheStats(
            long cacheHits,
            long cacheMisses,
            long cacheEvictions,
            long nonCacheableHits,
            long nonCacheableMisses
    ) {
        public double hitRate() {
            long total = cacheHits + cacheMisses;
            return total == 0 ? 1.0 : (double) cacheHits / total;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d}",
                    cacheHits, cacheMisses, hitRate() * 100, cacheEvictions);
        }
    }
}
