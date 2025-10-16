package net.exylia.commons.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CaffeineCache<K, V> {

    @Getter
    private final Cache<K, V> cache;
    private final LoadingCache<K, V> loadingCache;
    private final CacheConfig config;

    private CaffeineCache(Cache<K, V> cache, LoadingCache<K, V> loadingCache, CacheConfig config) {
        this.cache = cache;
        this.loadingCache = loadingCache;
        this.config = config;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public V get(K key) {
        if (loadingCache != null) {
            return loadingCache.get(key);
        }
        return cache.getIfPresent(key);
    }

    public V get(K key, Function<K, V> loader) {
        if (cache != null) {
            return cache.get(key, loader);
        }
        return loadingCache.get(key);
    }

    public CompletableFuture<V> getAsync(K key) {
        if (loadingCache != null) {
            return CompletableFuture.supplyAsync(() -> loadingCache.get(key));
        }
        return CompletableFuture.supplyAsync(() -> cache.getIfPresent(key));
    }

    public void put(K key, V value) {
        if (cache != null) {
            cache.put(key, value);
        } else {
            loadingCache.put(key, value);
        }
    }

    public void putAll(Map<K, V> map) {
        if (cache != null) {
            cache.putAll(map);
        } else {
            loadingCache.putAll(map);
        }
    }

    public void invalidate(K key) {
        if (cache != null) {
            cache.invalidate(key);
        } else {
            loadingCache.invalidate(key);
        }
    }

    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        } else {
            loadingCache.invalidateAll();
        }
    }

    public void invalidateAll(Iterable<K> keys) {
        if (cache != null) {
            cache.invalidateAll(keys);
        } else {
            loadingCache.invalidateAll(keys);
        }
    }

    public long size() {
        if (cache != null) {
            return cache.estimatedSize();
        }
        return loadingCache.estimatedSize();
    }

    public Map<K, V> asMap() {
        if (cache != null) {
            return cache.asMap();
        }
        return loadingCache.asMap();
    }

    public void cleanUp() {
        if (cache != null) {
            cache.cleanUp();
        } else {
            loadingCache.cleanUp();
        }
    }

    public CacheStats getStats() {
        if (cache != null) {
            return CacheStats.from(cache.stats());
        }
        return CacheStats.from(loadingCache.stats());
    }

    public static class Builder<K, V> {
        private Long expireAfterWrite;
        private Long expireAfterAccess;
        private Long refreshAfterWrite;
        private Long maximumSize;
        private Function<K, V> loader;
        private CacheRemovalListener<K, V> removalListener;
        private boolean recordStats = false;
        private int initialCapacity = 16;
        private boolean weakKeys = false;
        private boolean weakValues = false;
        private boolean softValues = false;

        public Builder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
            this.expireAfterWrite = unit.toMillis(duration);
            return this;
        }

        public Builder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
            this.expireAfterAccess = unit.toMillis(duration);
            return this;
        }

        public Builder<K, V> refreshAfterWrite(long duration, TimeUnit unit) {
            this.refreshAfterWrite = unit.toMillis(duration);
            return this;
        }

        public Builder<K, V> maximumSize(long size) {
            this.maximumSize = size;
            return this;
        }

        public Builder<K, V> loader(Function<K, V> loader) {
            this.loader = loader;
            return this;
        }

        public Builder<K, V> removalListener(CacheRemovalListener<K, V> listener) {
            this.removalListener = listener;
            return this;
        }

        public Builder<K, V> recordStats() {
            this.recordStats = true;
            return this;
        }

        public Builder<K, V> initialCapacity(int capacity) {
            this.initialCapacity = capacity;
            return this;
        }

        public Builder<K, V> weakKeys() {
            this.weakKeys = true;
            return this;
        }

        public Builder<K, V> weakValues() {
            this.weakValues = true;
            return this;
        }

        public Builder<K, V> softValues() {
            this.softValues = true;
            return this;
        }

        public CaffeineCache<K, V> build() {
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                    .initialCapacity(initialCapacity);

            if (expireAfterWrite != null) {
                caffeine.expireAfterWrite(expireAfterWrite, TimeUnit.MILLISECONDS);
            }

            if (expireAfterAccess != null) {
                caffeine.expireAfterAccess(expireAfterAccess, TimeUnit.MILLISECONDS);
            }

            if (maximumSize != null) {
                caffeine.maximumSize(maximumSize);
            }

            if (weakKeys) {
                caffeine.weakKeys();
            }

            if (weakValues) {
                caffeine.weakValues();
            }

            if (softValues) {
                caffeine.softValues();
            }

            if (recordStats) {
                caffeine.recordStats();
            }

            if (removalListener != null) {
                caffeine.removalListener((K key, V value, RemovalCause cause) -> {
                    removalListener.onRemoval(key, value, cause);
                });
            }

            CacheConfig config = new CacheConfig(
                    expireAfterWrite,
                    expireAfterAccess,
                    refreshAfterWrite,
                    maximumSize,
                    recordStats
            );

            if (loader != null) {
                LoadingCache<K, V> loadingCache = caffeine.build(loader::apply);
                return new CaffeineCache<>(null, loadingCache, config);
            } else {
                Cache<K, V> cache = caffeine.build();
                return new CaffeineCache<>(cache, null, config);
            }
        }
    }

    @Getter
    public static class CacheConfig {
        private final Long expireAfterWrite;
        private final Long expireAfterAccess;
        private final Long refreshAfterWrite;
        private final Long maximumSize;
        private final boolean recordStats;

        public CacheConfig(Long expireAfterWrite, Long expireAfterAccess, Long refreshAfterWrite,
                          Long maximumSize, boolean recordStats) {
            this.expireAfterWrite = expireAfterWrite;
            this.expireAfterAccess = expireAfterAccess;
            this.refreshAfterWrite = refreshAfterWrite;
            this.maximumSize = maximumSize;
            this.recordStats = recordStats;
        }
    }

    @FunctionalInterface
    public interface CacheRemovalListener<K, V> {
        void onRemoval(K key, V value, RemovalCause cause);
    }
}
