package net.exylia.commons.v2.action.ratelimit;

import java.util.UUID;

public interface RateLimiter {
    boolean allowRequest(UUID playerId, String actionId, int maxRequests);

    int getRequestCount(UUID playerId, String actionId);

    void reset(UUID playerId, String actionId);

    void resetAll();
}
