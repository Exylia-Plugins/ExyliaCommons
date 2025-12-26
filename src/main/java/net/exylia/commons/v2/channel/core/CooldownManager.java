package net.exylia.commons.v2.channel.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final ConcurrentHashMap<String, Cache<UUID, Long>> cooldowns;

    public CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void registerChannel(String channelId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return;
        }

        long cooldownMillis = (long) (cooldownSeconds * 1000);

        Cache<UUID, Long> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(cooldownMillis))
                .maximumSize(10000)
                .build();

        cooldowns.put(channelId, cache);
    }

    public boolean isOnCooldown(String channelId, UUID playerId) {
        Cache<UUID, Long> cache = cooldowns.get(channelId);
        if (cache == null) {
            return false;
        }

        Long lastUse = cache.getIfPresent(playerId);
        return lastUse != null;
    }

    public void setCooldown(String channelId, UUID playerId) {
        Cache<UUID, Long> cache = cooldowns.get(channelId);
        if (cache == null) {
            return;
        }

        cache.put(playerId, System.currentTimeMillis());
    }

    public double getRemainingSeconds(String channelId, UUID playerId) {
        Cache<UUID, Long> cache = cooldowns.get(channelId);
        if (cache == null) {
            return 0;
        }

        Long lastUse = cache.getIfPresent(playerId);
        if (lastUse == null) {
            return 0;
        }

        return 0;
    }

    public void clearCooldown(String channelId, UUID playerId) {
        Cache<UUID, Long> cache = cooldowns.get(channelId);
        if (cache != null) {
            cache.invalidate(playerId);
        }
    }

    public void unregisterChannel(String channelId) {
        Cache<UUID, Long> cache = cooldowns.remove(channelId);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    public void clearAll() {
        cooldowns.values().forEach(Cache::invalidateAll);
        cooldowns.clear();
    }
}
