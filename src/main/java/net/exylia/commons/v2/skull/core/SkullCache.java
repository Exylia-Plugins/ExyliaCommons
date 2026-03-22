package net.exylia.commons.v2.skull.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.skull.config.SkullConfig;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SkullCache {

    private final Cache<String, ItemStack> textureCache;
    private final Cache<String, ItemStack> playerCache;
    private final ConcurrentHashMap<String, CompletableFuture<ItemStack>> pendingRequests;

    @Getter
    private volatile long rateLimitBackoff = 0L;

    public SkullCache(SkullConfig config) {
        this.textureCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxCacheSize())
                .expireAfterWrite(config.getCacheExpiration(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build();

        this.playerCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxCacheSize())
                .expireAfterWrite(config.getCacheExpiration(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build();

        this.pendingRequests = new ConcurrentHashMap<>();
    }

    public ItemStack getTexture(String key) {
        ItemStack result = textureCache.getIfPresent(key);
        if (result != null) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Cache HIT (texture): " + key);
        }
        return result;
    }

    public void putTexture(String key, ItemStack skull) {
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Cache PUT (texture): " + key);
        textureCache.put(key, skull);
    }

    public ItemStack getPlayer(String key) {
        ItemStack result = playerCache.getIfPresent(key);
        if (result != null) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Cache HIT (player): " + key);
        }
        return result;
    }

    public void putPlayer(String key, ItemStack skull) {
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Cache PUT (player): " + key);
        playerCache.put(key, skull);
    }

    public CompletableFuture<ItemStack> getPending(String key) {
        return pendingRequests.get(key);
    }

    public void putPending(String key, CompletableFuture<ItemStack> future) {
        pendingRequests.put(key, future);
        future.whenComplete((r, t) -> pendingRequests.remove(key));
    }

    public void setRateLimitBackoff(long millis) {
        this.rateLimitBackoff = System.currentTimeMillis() + millis;
        DebugAPI.logLibWarn(DebugCategory.SKULL, "Rate limit backoff set for " + millis + "ms");
    }

    public void setNetworkErrorBackoff(long millis) {
        long until = System.currentTimeMillis() + millis;
        if (until > this.rateLimitBackoff) {
            this.rateLimitBackoff = until;
            DebugAPI.logLibWarn(DebugCategory.SKULL, "Network error backoff set for " + millis + "ms");
        }
    }

    public boolean isRateLimited() {
        return System.currentTimeMillis() < rateLimitBackoff;
    }

    public void clearTextures() {
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Clearing texture cache");
        textureCache.invalidateAll();
    }

    public void clearPlayer(String key) {
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Clearing player cache entry: " + key);
        playerCache.invalidate(key.toLowerCase());
    }

    public void clearPlayers() {
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Clearing player cache");
        playerCache.invalidateAll();
    }

    public void clearAll() {
        DebugAPI.logLibDebug(DebugCategory.SKULL, "Clearing all caches");
        textureCache.invalidateAll();
        playerCache.invalidateAll();
        pendingRequests.clear();
    }

    public String getStats() {
        CacheStats textureStats = textureCache.stats();
        CacheStats playerStats = playerCache.stats();

        long backoffRemaining = Math.max(0, rateLimitBackoff - System.currentTimeMillis());

        return String.format(
                "Textures[Size: %d, Hits: %d, Misses: %d, Hit Rate: %.2f%%] " +
                "Players[Size: %d, Hits: %d, Misses: %d, Hit Rate: %.2f%%] " +
                "Pending[%d] RateLimit[%dms]",
                textureCache.estimatedSize(),
                textureStats.hitCount(),
                textureStats.missCount(),
                textureStats.hitRate() * 100,
                playerCache.estimatedSize(),
                playerStats.hitCount(),
                playerStats.missCount(),
                playerStats.hitRate() * 100,
                pendingRequests.size(),
                backoffRemaining
        );
    }
}
