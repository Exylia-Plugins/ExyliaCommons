package net.exylia.commons.v2.hologram.cache;

import net.exylia.commons.cache.CaffeineCache;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HologramCacheManager {
    private final CaffeineCache<String, Hologram> hologramCache;
    private final CaffeineCache<HologramCacheKey.LocationKey, List<Hologram>> locationCache;
    private final CaffeineCache<UUID, Set<String>> playerVisibilityCache;

    public HologramCacheManager() {
        this.hologramCache = CaffeineCache.<String, Hologram>builder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .recordStats()
                .build();

        this.locationCache = CaffeineCache.<HologramCacheKey.LocationKey, List<Hologram>>builder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();

        this.playerVisibilityCache = CaffeineCache.<UUID, Set<String>>builder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(2000)
                .recordStats()
                .build();
    }

    public void cache(Hologram hologram) {
        hologramCache.put(hologram.getId(), hologram);
    }

    public Optional<Hologram> get(String id) {
        return Optional.ofNullable(hologramCache.get(id));
    }

    public List<Hologram> getNearby(Location location, double radius) {
        HologramCacheKey.LocationKey key = new HologramCacheKey.LocationKey(location, radius);

        List<Hologram> cached = locationCache.get(key);
        if (cached != null) {
            return cached;
        }

        return Collections.emptyList();
    }

    public void cacheNearby(Location location, double radius, List<Hologram> holograms) {
        HologramCacheKey.LocationKey key = new HologramCacheKey.LocationKey(location, radius);
        locationCache.put(key, holograms);
    }

    public void invalidate(String id) {
        hologramCache.invalidate(id);
        locationCache.invalidateAll();
    }

    public void invalidatePlayer(UUID playerId) {
        playerVisibilityCache.invalidate(playerId);
    }

    public void invalidateAll() {
        hologramCache.invalidateAll();
        locationCache.invalidateAll();
        playerVisibilityCache.invalidateAll();
    }

    public void cleanup() {
        hologramCache.cleanUp();
        locationCache.cleanUp();
        playerVisibilityCache.cleanUp();
    }

    public void cachePlayerVisibility(UUID playerId, Set<String> visibleHologramIds) {
        playerVisibilityCache.put(playerId, visibleHologramIds);
    }

    public Optional<Set<String>> getPlayerVisibility(UUID playerId) {
        return Optional.ofNullable(playerVisibilityCache.get(playerId));
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        com.github.benmanes.caffeine.cache.stats.CacheStats hologramStats = hologramCache.getCache().stats();
        stats.put("hologram_cache", Map.of(
                "size", hologramCache.size(),
                "hitRate", hologramStats.hitRate(),
                "missRate", hologramStats.missRate(),
                "evictionCount", hologramStats.evictionCount()
        ));

        com.github.benmanes.caffeine.cache.stats.CacheStats locationStats = locationCache.getCache().stats();
        stats.put("location_cache", Map.of(
                "size", locationCache.size(),
                "hitRate", locationStats.hitRate(),
                "missRate", locationStats.missRate(),
                "evictionCount", locationStats.evictionCount()
        ));

        com.github.benmanes.caffeine.cache.stats.CacheStats visibilityStats = playerVisibilityCache.getCache().stats();
        stats.put("visibility_cache", Map.of(
                "size", playerVisibilityCache.size(),
                "hitRate", visibilityStats.hitRate(),
                "missRate", visibilityStats.missRate(),
                "evictionCount", visibilityStats.evictionCount()
        ));

        return stats;
    }
}
