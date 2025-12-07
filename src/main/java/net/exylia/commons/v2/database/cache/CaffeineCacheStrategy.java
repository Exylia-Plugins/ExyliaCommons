package net.exylia.commons.v2.database.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Getter
public class CaffeineCacheStrategy<K, V> implements CacheStrategy<K, V> {

    private final Cache<K, V> cache;
    private final boolean recordStats;
    private final AtomicLong evictionCount = new AtomicLong(0);

    public CaffeineCacheStrategy(long ttlMinutes, int maxSize, boolean recordStats, boolean refreshAfterAccess) {
        this.recordStats = recordStats;

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize);

        if (recordStats) {
            builder.recordStats();
        }

        if (refreshAfterAccess) {
            builder.expireAfterAccess(Duration.ofMinutes(ttlMinutes));
        }

        builder.removalListener((key, value, cause) -> {
            if (cause != null && cause.wasEvicted()) {
                evictionCount.incrementAndGet();
                if (recordStats) {
                    DebugUtils.logDebug("Cache evicted: " + key);
                }
            }
        });

        this.cache = builder.build();
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        return cache.get(key, loader);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public void removeAll() {
        cache.invalidateAll();
    }

    @Override
    public void invalidate(K key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public CacheStats getStats() {
        if (recordStats) {
            com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
            return new CacheStats(
                    (long) stats.hitCount(),
                    (long) stats.missCount(),
                    evictionCount.get(),
                    (long) stats.loadCount(),
                    cache.estimatedSize()
            );
        }
        return new CacheStats(0, 0, evictionCount.get(), 0, cache.estimatedSize());
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        evictionCount.set(0);
    }
}
