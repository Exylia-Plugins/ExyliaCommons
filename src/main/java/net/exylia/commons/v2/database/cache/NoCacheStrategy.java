package net.exylia.commons.v2.database.cache;

import java.util.function.Function;

public class NoCacheStrategy<K, V> implements CacheStrategy<K, V> {

    @Override
    public V get(K key, Function<K, V> loader) {
        return loader.apply(key);
    }

    @Override
    public void put(K key, V value) {
    }

    @Override
    public void remove(K key) {
    }

    @Override
    public void removeAll() {
    }

    @Override
    public void invalidate(K key) {
    }

    @Override
    public void invalidateAll() {
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(0, 0, 0, 0, 0);
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void clear() {
    }
}
