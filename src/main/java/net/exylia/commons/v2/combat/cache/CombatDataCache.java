package net.exylia.commons.v2.combat.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.combat.model.CombatData;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class CombatDataCache {

    private static final Duration TTL = Duration.ofMinutes(2);
    private static final int MAX_SIZE = 5000;

    private final Cache<UUID, Optional<CombatData>> cache;

    public CombatDataCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .recordStats()
                .build();
    }

    public Optional<Optional<CombatData>> get(UUID playerId) {
        return Optional.ofNullable(cache.getIfPresent(playerId));
    }

    public void put(UUID playerId, Optional<CombatData> data) {
        cache.put(playerId, data);
    }

    public void invalidate(UUID playerId) {
        cache.invalidate(playerId);
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
