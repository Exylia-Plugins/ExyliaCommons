package net.exylia.commons.v2.channel.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

public class PermissionCache {

    private static final Duration TTL = Duration.ofSeconds(5);
    private static final int MAX_SIZE = 1000;

    private final Cache<String, Boolean> cache;

    public PermissionCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .recordStats()
                .build();
    }

    public boolean hasPermission(Player player, String permission) {
        String key = player.getUniqueId() + ":" + permission;
        return cache.get(key, k -> player.hasPermission(permission));
    }

    public void invalidate(UUID playerId) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(playerId.toString() + ":"));
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public long size() {
        return cache.estimatedSize();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }
}
