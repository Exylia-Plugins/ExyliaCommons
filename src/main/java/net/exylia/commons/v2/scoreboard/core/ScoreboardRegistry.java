package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardRegistry {
    private final ConcurrentHashMap<UUID, ScoreboardInstance> instances = new ConcurrentHashMap<>();

    public Optional<ScoreboardInstance> replace(UUID playerId, ScoreboardInstance instance) {
        return Optional.ofNullable(instances.put(playerId, instance));
    }

    public Optional<ScoreboardInstance> remove(UUID playerId) {
        return Optional.ofNullable(instances.remove(playerId));
    }

    public Optional<ScoreboardInstance> get(UUID playerId) {
        return Optional.ofNullable(instances.get(playerId));
    }

    public boolean has(UUID playerId) {
        return instances.containsKey(playerId);
    }

    public Collection<ScoreboardInstance> all() {
        return instances.values();
    }

    public int size() {
        return instances.size();
    }

    public void clear() {
        instances.clear();
    }
}
