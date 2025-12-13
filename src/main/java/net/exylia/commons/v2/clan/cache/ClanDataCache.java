package net.exylia.commons.v2.clan.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.clan.model.Clan;

import java.time.Duration;
import java.util.Optional;

public class ClanDataCache {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final int MAX_SIZE = 2000;

    private final Cache<String, Optional<Clan>> cache;

    public ClanDataCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .recordStats()
                .build();
    }

    public Optional<Optional<Clan>> get(String identifier) {
        return Optional.ofNullable(cache.getIfPresent(identifier));
    }

    public void put(String identifier, Optional<Clan> clan) {
        cache.put(identifier, clan);
    }

    public void invalidate(String identifier) {
        cache.invalidate(identifier);
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
