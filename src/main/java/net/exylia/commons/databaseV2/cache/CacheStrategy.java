package net.exylia.commons.databaseV2.cache;

import java.util.function.Function;

public interface CacheStrategy<K, V> {

    V get(K key, Function<K, V> loader);

    void put(K key, V value);

    void remove(K key);

    void removeAll();

    void invalidate(K key);

    void invalidateAll();

    CacheStats getStats();

    long size();

    void clear();
}
