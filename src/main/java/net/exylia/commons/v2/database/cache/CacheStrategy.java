package net.exylia.commons.v2.database.cache;

import java.util.function.Function;

public interface CacheStrategy<K, V> {

    V get(K key, Function<K, V> loader);

    void put(K key, V value);

    void remove(K key);

    void removeAll();

    void invalidate(K key);

    void invalidateAll();

    /**
     * Drops the local (L1) copy of this entry only, without touching the shared L2
     * store (Redis) or publishing a cross-server invalidation. Used to force the next
     * read to go through to L2 for guaranteed freshness (e.g. on player join), without
     * paying the cost of a full network round-trip delete + pub/sub broadcast.
     * <p>
     * Default implementation delegates to {@link #invalidate(Object)} for strategies
     * that have no separate local layer (single-tier caches).
     */
    default void invalidateLocal(K key) {
        invalidate(key);
    }

    CacheStats getStats();

    long size();

    void clear();
}
