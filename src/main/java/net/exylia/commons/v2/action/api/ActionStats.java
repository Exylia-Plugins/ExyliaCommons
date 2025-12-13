package net.exylia.commons.v2.action.api;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.action.cache.ActionCacheStats;

import java.util.Map;

@Getter
@Builder
public class ActionStats {
    private final int totalActions;
    private final int totalNamespaces;
    private final Map<String, Integer> actionsByNamespace;
    private final ActionCacheStats cacheStats;
    private final long cooldownCacheSize;
    private final long rateLimitCacheSize;
    private final long executionCacheSize;
    private final int auditLogSize;
    private final boolean auditEnabled;
}
