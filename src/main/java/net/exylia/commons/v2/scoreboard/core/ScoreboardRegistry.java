package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardRegistry {

    private final ConcurrentHashMap<UUID, ScoreboardInstance> scoreboards = new ConcurrentHashMap<>();

    public void set(UUID playerId, ScoreboardInstance instance) {
        scoreboards.put(playerId, instance);
    }

    public Optional<ScoreboardInstance> remove(UUID playerId) {
        return Optional.ofNullable(scoreboards.remove(playerId));
    }

    public Optional<ScoreboardInstance> get(UUID playerId) {
        return Optional.ofNullable(scoreboards.get(playerId));
    }

    public boolean has(UUID playerId) {
        return scoreboards.containsKey(playerId);
    }

    public int getActiveCount() {
        return scoreboards.size();
    }

    public Collection<ScoreboardInstance> getAll() {
        return scoreboards.values();
    }

    public void clear() {
        scoreboards.values().forEach(ScoreboardInstance::hide);
        scoreboards.clear();
    }
}
