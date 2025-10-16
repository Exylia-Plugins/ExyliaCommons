package net.exylia.commons.cache;

import lombok.Getter;

@Getter
public class CacheStats {
    private final long hitCount;
    private final long missCount;
    private final long loadSuccessCount;
    private final long loadFailureCount;
    private final long totalLoadTime;
    private final long evictionCount;
    private final long evictionWeight;

    private CacheStats(long hitCount, long missCount, long loadSuccessCount,
                      long loadFailureCount, long totalLoadTime, long evictionCount,
                      long evictionWeight) {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.loadSuccessCount = loadSuccessCount;
        this.loadFailureCount = loadFailureCount;
        this.totalLoadTime = totalLoadTime;
        this.evictionCount = evictionCount;
        this.evictionWeight = evictionWeight;
    }

    public static CacheStats from(com.github.benmanes.caffeine.cache.stats.CacheStats stats) {
        return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount(),
                stats.totalLoadTime(),
                stats.evictionCount(),
                stats.evictionWeight()
        );
    }

    public double hitRate() {
        long requestCount = hitCount + missCount;
        return requestCount == 0 ? 1.0 : (double) hitCount / requestCount;
    }

    public double missRate() {
        long requestCount = hitCount + missCount;
        return requestCount == 0 ? 0.0 : (double) missCount / requestCount;
    }

    public double averageLoadPenalty() {
        long totalLoadCount = loadSuccessCount + loadFailureCount;
        return totalLoadCount == 0 ? 0.0 : (double) totalLoadTime / totalLoadCount;
    }

    @Override
    public String toString() {
        return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, avgLoadTime=%.2fms, evictions=%d}",
                hitCount, missCount, hitRate() * 100, averageLoadPenalty() / 1_000_000.0, evictionCount);
    }
}
