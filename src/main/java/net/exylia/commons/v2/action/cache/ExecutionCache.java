package net.exylia.commons.v2.action.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import net.exylia.commons.v2.action.model.ActionResult;

import java.time.Duration;
import java.util.UUID;

public class ExecutionCache {
    private final Cache<UUID, ActionResult> cache;

    public ExecutionCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    public void recordExecution(UUID executionId, ActionResult result) {
        cache.put(executionId, result);
    }

    public ActionResult getExecution(UUID executionId) {
        return cache.getIfPresent(executionId);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    public long size() {
        return cache.estimatedSize();
    }
}
