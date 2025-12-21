package net.exylia.commons.v2.ui.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UICacheStats {
    private final CacheStats templateCacheStats;
    private final CacheStats instanceCacheStats;
    private final CacheStats configCacheStats;
    private final CacheStats historyCacheStats;

    private final long templateCacheSize;
    private final long instanceCacheSize;
    private final long configCacheSize;
    private final long historyCacheSize;

    public double getTemplateCacheHitRate() {
        return templateCacheStats != null ? templateCacheStats.hitRate() : 0.0;
    }

    public double getInstanceCacheHitRate() {
        return instanceCacheStats != null ? instanceCacheStats.hitRate() : 0.0;
    }

    public double getConfigCacheHitRate() {
        return configCacheStats != null ? configCacheStats.hitRate() : 0.0;
    }

    public double getHistoryCacheHitRate() {
        return historyCacheStats != null ? historyCacheStats.hitRate() : 0.0;
    }

    public double getOverallHitRate() {
        long totalHits = 0;
        long totalRequests = 0;

        if (templateCacheStats != null) {
            totalHits += templateCacheStats.hitCount();
            totalRequests += templateCacheStats.requestCount();
        }
        if (instanceCacheStats != null) {
            totalHits += instanceCacheStats.hitCount();
            totalRequests += instanceCacheStats.requestCount();
        }
        if (configCacheStats != null) {
            totalHits += configCacheStats.hitCount();
            totalRequests += configCacheStats.requestCount();
        }
        if (historyCacheStats != null) {
            totalHits += historyCacheStats.hitCount();
            totalRequests += historyCacheStats.requestCount();
        }

        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }

    public long getTotalCacheSize() {
        return templateCacheSize + instanceCacheSize + configCacheSize + historyCacheSize;
    }
}
