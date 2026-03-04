package net.exylia.commons.v2.formatter.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Value;

@Value
public class FormatterCacheStats {
    CacheStats patternCache;
    CacheStats dateTimeFormatterCache;
    CacheStats decimalFormatCache;
    CacheStats resultCache;
    CacheStats configCache;

    public double getOverallHitRate() {
        long totalHits = patternCache.hitCount() + dateTimeFormatterCache.hitCount()
                + decimalFormatCache.hitCount() + resultCache.hitCount()
                + configCache.hitCount();
        long totalMisses = patternCache.missCount() + dateTimeFormatterCache.missCount()
                + decimalFormatCache.missCount() + resultCache.missCount()
                + configCache.missCount();
        long totalRequests = totalHits + totalMisses;
        return totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
    }

    public long getTotalEvictions() {
        return patternCache.evictionCount() + dateTimeFormatterCache.evictionCount()
                + decimalFormatCache.evictionCount() + resultCache.evictionCount()
                + configCache.evictionCount();
    }
}
