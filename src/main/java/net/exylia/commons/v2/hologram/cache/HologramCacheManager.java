package net.exylia.commons.v2.hologram.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HologramCacheManager {
    private final Cache<String, Hologram> hologramCache;
    private final Cache<UUID, Set<String>> playerVisibilityCache;

    public HologramCacheManager() {
        this.hologramCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .recordStats()
                .build();

        this.playerVisibilityCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(2000)
                .recordStats()
                .build();
    }

    public void cache(Hologram hologram) {
        hologramCache.put(hologram.getId(), hologram);
    }

    public Optional<Hologram> get(String id) {
        return Optional.ofNullable(hologramCache.getIfPresent(id));
    }

    public void invalidate(String id) {
        hologramCache.invalidate(id);
    }

    public void invalidatePlayer(UUID playerId) {
        playerVisibilityCache.invalidate(playerId);
    }

    public void invalidateAll() {
        hologramCache.invalidateAll();
        playerVisibilityCache.invalidateAll();
    }

    public void cleanup() {
        hologramCache.cleanUp();
        playerVisibilityCache.cleanUp();
    }

    public void cachePlayerVisibility(UUID playerId, Set<String> visibleHologramIds) {
        playerVisibilityCache.put(playerId, visibleHologramIds);
    }

    public Optional<Set<String>> getPlayerVisibility(UUID playerId) {
        return Optional.ofNullable(playerVisibilityCache.getIfPresent(playerId));
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        com.github.benmanes.caffeine.cache.stats.CacheStats hologramStats = hologramCache.stats();
        stats.put("hologram_cache", Map.of(
                "size", hologramCache.estimatedSize(),
                "hitRate", hologramStats.hitRate(),
                "missRate", hologramStats.missRate(),
                "evictionCount", hologramStats.evictionCount()
        ));

        com.github.benmanes.caffeine.cache.stats.CacheStats visibilityStats = playerVisibilityCache.stats();
        stats.put("visibility_cache", Map.of(
                "size", playerVisibilityCache.estimatedSize(),
                "hitRate", visibilityStats.hitRate(),
                "missRate", visibilityStats.missRate(),
                "evictionCount", visibilityStats.evictionCount()
        ));

        return stats;
    }
}
