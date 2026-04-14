package net.exylia.commons.v2.clan.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.clan.model.Clan;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class PlayerClanCache {

    private static final Duration TTL = Duration.ofSeconds(15);
    private static final int MAX_SIZE = 5000;

    private final Cache<UUID, Optional<Clan>> cache;

    public PlayerClanCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .recordStats()
                .build();
    }

    public Optional<Optional<Clan>> get(UUID playerId) {
        return Optional.ofNullable(cache.getIfPresent(playerId));
    }

    public void put(UUID playerId, Optional<Clan> clan) {
        cache.put(playerId, clan);
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
