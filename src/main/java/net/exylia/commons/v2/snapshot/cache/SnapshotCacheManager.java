package net.exylia.commons.v2.snapshot.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.exylia.commons.v2.snapshot.model.SnapshotData;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SnapshotCacheManager {

    private final Cache<String, SnapshotData> snapshotCache;

    public SnapshotCacheManager() {
        this.snapshotCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public void cache(UUID playerUuid, String snapshotId, SnapshotData snapshot) {
        snapshotCache.put(buildKey(playerUuid, snapshotId), snapshot);
    }

    public Optional<SnapshotData> get(UUID playerUuid, String snapshotId) {
        return Optional.ofNullable(snapshotCache.getIfPresent(buildKey(playerUuid, snapshotId)));
    }

    public void invalidate(UUID playerUuid, String snapshotId) {
        snapshotCache.invalidate(buildKey(playerUuid, snapshotId));
    }

    public void invalidateAll() {
        snapshotCache.invalidateAll();
    }

    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = snapshotCache.stats();
        return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount(),
                stats.evictionCount()
        );
    }

    private String buildKey(UUID playerUuid, String snapshotId) {
        return playerUuid.toString() + ":" + snapshotId;
    }

    @Getter
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final long loadSuccessCount;
        private final long loadFailureCount;
        private final long evictionCount;

        public CacheStats(long hitCount, long missCount, long loadSuccessCount,
                          long loadFailureCount, long evictionCount) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.loadSuccessCount = loadSuccessCount;
            this.loadFailureCount = loadFailureCount;
            this.evictionCount = evictionCount;
        }

        public double hitRate() {
            long requestCount = hitCount + missCount;
            return requestCount == 0 ? 1.0 : (double) hitCount / requestCount;
        }
    }
}
