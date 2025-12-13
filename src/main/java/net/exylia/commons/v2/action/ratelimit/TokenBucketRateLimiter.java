package net.exylia.commons.v2.action.ratelimit;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.cache.RateLimitCache;

import java.util.UUID;

@RequiredArgsConstructor
public class TokenBucketRateLimiter implements RateLimiter {
    private final RateLimitCache rateLimitCache;

    @Override
    public boolean allowRequest(UUID playerId, String actionId, int maxRequests) {
        if (maxRequests <= 0) {
            return true;
        }

        int currentCount = rateLimitCache.increment(playerId, actionId);
        return currentCount <= maxRequests;
    }

    @Override
    public int getRequestCount(UUID playerId, String actionId) {
        return rateLimitCache.getCount(playerId, actionId);
    }

    @Override
    public void reset(UUID playerId, String actionId) {
        rateLimitCache.reset(playerId, actionId);
    }

    @Override
    public void resetAll() {
        rateLimitCache.invalidateAll();
    }
}
