package net.exylia.commons.v2.action.cache;

import lombok.Getter;

@Getter
public class ActionCacheManager {
    private final CooldownCache cooldownCache;
    private final RateLimitCache rateLimitCache;
    private final ExecutionCache executionCache;

    public ActionCacheManager() {
        this.cooldownCache = new CooldownCache();
        this.rateLimitCache = new RateLimitCache();
        this.executionCache = new ExecutionCache();
    }

    public void invalidateAll() {
        cooldownCache.invalidateAll();
        rateLimitCache.invalidateAll();
        executionCache.invalidateAll();
    }

    public ActionCacheStats getStats() {
        return new ActionCacheStats(
                cooldownCache.getStats(),
                rateLimitCache.getStats(),
                executionCache.getStats()
        );
    }

    public long getTotalSize() {
        return cooldownCache.size() + rateLimitCache.size() + executionCache.size();
    }
}
