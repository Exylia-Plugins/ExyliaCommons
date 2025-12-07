package net.exylia.commons.v2.formatter.cache;

import lombok.Value;
import net.exylia.commons.cache.CacheStats;

@Value
public class FormatterCacheStats {
    CacheStats patternCache;
    CacheStats dateTimeFormatterCache;
    CacheStats decimalFormatCache;
    CacheStats resultCache;
    CacheStats configCache;

    public double getOverallHitRate() {
        long totalHits = patternCache.getHitCount() + dateTimeFormatterCache.getHitCount()
                + decimalFormatCache.getHitCount() + resultCache.getHitCount()
                + configCache.getHitCount();
        long totalMisses = patternCache.getMissCount() + dateTimeFormatterCache.getMissCount()
                + decimalFormatCache.getMissCount() + resultCache.getMissCount()
                + configCache.getMissCount();
        long totalRequests = totalHits + totalMisses;

        return totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
    }

    public long getTotalEvictions() {
        return patternCache.getEvictionCount() + dateTimeFormatterCache.getEvictionCount()
                + decimalFormatCache.getEvictionCount() + resultCache.getEvictionCount()
                + configCache.getEvictionCount();
    }
}
