package net.exylia.commons.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Cache<K, V> {

    private final Map<K, CacheEntry<V>> map;
    private final long defaultExpirationMs;
    private final int maxSize;
    private final ScheduledExecutorService cleanupService;

    public Cache(long defaultExpirationMs, int maxSize, long cleanupIntervalMs) {
        this.defaultExpirationMs = defaultExpirationMs;
        this.maxSize = maxSize;
        this.map = Collections.synchronizedMap(new LinkedHashMap<K, CacheEntry<V>>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return maxSize > 0 && size() > maxSize;
            }
        });
        if (cleanupIntervalMs > 0) {
            this.cleanupService = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Cache-Cleanup-Thread");
                t.setDaemon(true);
                return t;
            });
            this.cleanupService.scheduleAtFixedRate(this::cleanup, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        } else {
            this.cleanupService = null;
        }
    }

    public Cache() {
        this(600000, 0, 300000);
    }

    public V get(K key, Function<K, V> loadFunction) {
        CacheEntry<V> entry = map.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        if (entry != null) {
            map.remove(key);
        }
        V value = loadFunction.apply(key);
        put(key, value);
        return value;
    }

    public void put(K key, V value) {
        put(key, value, defaultExpirationMs);
    }

    public void put(K key, V value, long expirationMs) {
        long expiration = expirationMs > 0 ? System.currentTimeMillis() + expirationMs : 0;
        map.put(key, new CacheEntry<>(value, expiration));
    }

    public boolean remove(K key) {
        return map.remove(key) != null;
    }

    public boolean contains(K key) {
        CacheEntry<V> entry = map.get(key);
        return entry != null && !entry.isExpired();
    }

    public void clear() {
        map.clear();
    }

    public int size() {
        return map.size();
    }

    public void shutdown() {
        if (cleanupService != null) {
            cleanupService.shutdown();
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        synchronized (map) {
            map.entrySet().removeIf(e -> {
                CacheEntry<V> ce = e.getValue();
                return ce.getExpirationTime() > 0 && ce.getExpirationTime() <= now;
            });
        }
    }

    private static class CacheEntry<V> {
        private final V value;
        private final long expirationTime;

        public CacheEntry(V value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        public V getValue() {
            return value;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public boolean isExpired() {
            return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
        }
    }
}
