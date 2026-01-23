package net.exylia.commons.redis.cache;

import net.exylia.commons.redis.RedisManager;
import net.exylia.commons.redis.serialization.RedisSerializer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.exylia.commons.utils.DebugUtils.logInternalError;

@Deprecated
public class RedisCache<T> {

    private final RedisManager redisManager;
    private final String cacheName;
    private final Class<T> type;
    private final RedisSerializer serializer;
    private final String keyPrefix;
    private final Map<String, CacheEntry<T>> localCache;
    private final boolean useLocalCache;
    private volatile boolean closed = false;

    public RedisCache(RedisManager redisManager, String cacheName, Class<T> type, RedisSerializer serializer) {
        this.redisManager = redisManager;
        this.cacheName = cacheName;
        this.type = type;
        this.serializer = serializer;
        this.keyPrefix = redisManager.getConfig().getKeyPrefix() + "cache:" + cacheName + ":";
        this.useLocalCache = true;  
        this.localCache = useLocalCache ? new ConcurrentHashMap<>() : null;
    }

    public T get(String key) {
        if (closed) return null;

        try {
             
            if (useLocalCache) {
                CacheEntry<T> localEntry = localCache.get(key);
                if (localEntry != null && !localEntry.isExpired()) {
                    return localEntry.getValue();
                } else if (localEntry != null && localEntry.isExpired()) {
                    localCache.remove(key);
                }
            }

            String redisKey = keyPrefix + key;
            String serialized = redisManager.get(redisKey);

            if (serialized == null) {
                return null;
            }

            T value = serializer.deserialize(serialized, type);

            if (useLocalCache && value != null) {
                long ttl = redisManager.getTTL(redisKey);
                if (ttl > 0) {
                    localCache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + (ttl * 1000)));
                } else if (ttl == -1) {  
                    localCache.put(key, new CacheEntry<>(value, -1));
                }
            }

            return value;

        } catch (Exception e) {
            logInternalError("Error obteniendo valor de caché '" + cacheName + "': " + e.getMessage());
            return null;
        }
    }

    public void put(String key, T value) {
        put(key, value, redisManager.getConfig().getDefaultTTL());
    }

    public void put(String key, T value, int ttlSeconds) {
        if (closed) return;

        try {
            String redisKey = keyPrefix + key;
            String serialized = serializer.serialize(value);

            if (ttlSeconds > 0) {
                redisManager.set(redisKey, serialized, ttlSeconds);
            } else {
                redisManager.set(redisKey, serialized);
            }

            if (useLocalCache) {
                long expirationTime = ttlSeconds > 0 ?
                        System.currentTimeMillis() + (ttlSeconds * 1000L) : -1;
                localCache.put(key, new CacheEntry<>(value, expirationTime));
            }

        } catch (Exception e) {
            logInternalError("Error almacenando valor en caché '" + cacheName + "': " + e.getMessage());
        }
    }

    public boolean remove(String key) {
        if (closed) return false;

        try {
            String redisKey = keyPrefix + key;
            boolean removed = redisManager.delete(redisKey);

            if (useLocalCache) {
                localCache.remove(key);
            }

            return removed;

        } catch (Exception e) {
            logInternalError("Error eliminando valor de caché '" + cacheName + "': " + e.getMessage());
            return false;
        }
    }

    public boolean exists(String key) {
        if (closed) return false;

        try {
             
            if (useLocalCache) {
                CacheEntry<T> localEntry = localCache.get(key);
                if (localEntry != null && !localEntry.isExpired()) {
                    return true;
                } else if (localEntry != null && localEntry.isExpired()) {
                    localCache.remove(key);
                }
            }

            String redisKey = keyPrefix + key;
            return redisManager.exists(redisKey);

        } catch (Exception e) {
            logInternalError("Error verificando existencia en caché '" + cacheName + "': " + e.getMessage());
            return false;
        }
    }

    public T getOrCompute(String key, Supplier<T> supplier) {
        return getOrCompute(key, supplier, redisManager.getConfig().getDefaultTTL());
    }

    public T getOrCompute(String key, Supplier<T> supplier, int ttlSeconds) {
        T value = get(key);
        if (value != null) {
            return value;
        }

        try {
            value = supplier.get();
            if (value != null) {
                put(key, value, ttlSeconds);
            }
            return value;
        } catch (Exception e) {
            logInternalError("Error computando valor para caché '" + cacheName + "': " + e.getMessage());
            return null;
        }
    }

    public Map<String, T> getMultiple(Collection<String> keys) {
        Map<String, T> result = new HashMap<>();

        for (String key : keys) {
            T value = get(key);
            if (value != null) {
                result.put(key, value);
            }
        }

        return result;
    }

    public void putMultiple(Map<String, T> values) {
        putMultiple(values, redisManager.getConfig().getDefaultTTL());
    }

    public void putMultiple(Map<String, T> values, int ttlSeconds) {
        for (Map.Entry<String, T> entry : values.entrySet()) {
            put(entry.getKey(), entry.getValue(), ttlSeconds);
        }
    }

    public boolean expire(String key, int ttlSeconds) {
        if (closed) return false;

        try {
            String redisKey = keyPrefix + key;
            boolean success = redisManager.expire(redisKey, ttlSeconds);

            if (useLocalCache && success) {
                CacheEntry<T> localEntry = localCache.get(key);
                if (localEntry != null) {
                    long newExpiration = System.currentTimeMillis() + (ttlSeconds * 1000L);
                    localCache.put(key, new CacheEntry<>(localEntry.getValue(), newExpiration));
                }
            }

            return success;

        } catch (Exception e) {
            logInternalError("Error actualizando TTL en caché '" + cacheName + "': " + e.getMessage());
            return false;
        }
    }

    public long getTTL(String key) {
        if (closed) return -2;

        try {
            String redisKey = keyPrefix + key;
            return redisManager.getTTL(redisKey);
        } catch (Exception e) {
            logInternalError("Error obteniendo TTL de caché '" + cacheName + "': " + e.getMessage());
            return -2;
        }
    }

    public CompletableFuture<T> getAsync(String key) {
        return CompletableFuture.supplyAsync(() -> get(key));
    }

    public CompletableFuture<Void> putAsync(String key, T value) {
        return CompletableFuture.runAsync(() -> put(key, value));
    }

    public CompletableFuture<Void> putAsync(String key, T value, int ttlSeconds) {
        return CompletableFuture.runAsync(() -> put(key, value, ttlSeconds));
    }

    public CompletableFuture<T> getOrComputeAsync(String key, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> getOrCompute(key, supplier));
    }

    public void cleanup() {
        if (!useLocalCache || localCache == null) return;

        try {
            localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        } catch (Exception e) {
            logInternalError("Error limpiando caché local '" + cacheName + "': " + e.getMessage());
        }
    }

    public void clearLocalCache() {
        if (useLocalCache && localCache != null) {
            localCache.clear();
        }
    }

    public CacheStats getLocalCacheStats() {
        if (!useLocalCache || localCache == null) {
            return new CacheStats(0, 0);
        }

        int total = localCache.size();
        int expired = (int) localCache.values().stream()
                .mapToLong(entry -> entry.isExpired() ? 1 : 0)
                .sum();

        return new CacheStats(total, total - expired);
    }

    public void close() {
        closed = true;
        if (useLocalCache && localCache != null) {
            localCache.clear();
        }
    }

    private static class CacheEntry<T> {
        private final T value;
        private final long expirationTime;

        public CacheEntry(T value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        public T getValue() {
            return value;
        }

        public boolean isExpired() {
            return expirationTime != -1 && System.currentTimeMillis() > expirationTime;
        }
    }

    public static class CacheStats {
        private final int totalEntries;
        private final int validEntries;

        public CacheStats(int totalEntries, int validEntries) {
            this.totalEntries = totalEntries;
            this.validEntries = validEntries;
        }

        public int getTotalEntries() {
            return totalEntries;
        }

        public int getValidEntries() {
            return validEntries;
        }

        public int getExpiredEntries() {
            return totalEntries - validEntries;
        }

        @Override
        public String toString() {
            return "CacheStats{" +
                    "total=" + totalEntries +
                    ", valid=" + validEntries +
                    ", expired=" + getExpiredEntries() +
                    '}';
        }
    }

    public String getCacheName() {
        return cacheName;
    }

    public Class<T> getType() {
        return type;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public boolean isClosed() {
        return closed;
    }
}
