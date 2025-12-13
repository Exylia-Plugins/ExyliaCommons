package net.exylia.commons.v2.action.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ActionCacheStats {
    private final CacheStats cooldownStats;
    private final CacheStats rateLimitStats;
    private final CacheStats executionStats;
}
