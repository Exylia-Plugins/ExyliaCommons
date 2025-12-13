package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ScoreboardRegistry {

    private final ConcurrentHashMap<UUID, ScoreboardInstance> activeScoreboards;
    private final ConcurrentHashMap<String, Set<UUID>> scoreboardsByType;

    public ScoreboardRegistry() {
        this.activeScoreboards = new ConcurrentHashMap<>();
        this.scoreboardsByType = new ConcurrentHashMap<>();
    }

    public void register(UUID playerId, ScoreboardInstance instance) {
        activeScoreboards.put(playerId, instance);

        String scoreboardId = instance.getScoreboard().getId();
        scoreboardsByType.computeIfAbsent(scoreboardId, k -> ConcurrentHashMap.newKeySet())
                .add(playerId);
    }

    public void unregister(UUID playerId) {
        ScoreboardInstance removed = activeScoreboards.remove(playerId);

        if (removed != null) {
            String scoreboardId = removed.getScoreboard().getId();
            Set<UUID> players = scoreboardsByType.get(scoreboardId);

            if (players != null) {
                players.remove(playerId);

                if (players.isEmpty()) {
                    scoreboardsByType.remove(scoreboardId);
                }
            }
        }
    }

    public Optional<ScoreboardInstance> get(UUID playerId) {
        return Optional.ofNullable(activeScoreboards.get(playerId));
    }

    public Set<ScoreboardInstance> getAll() {
        return new HashSet<>(activeScoreboards.values());
    }

    public List<ScoreboardInstance> getAllAsList() {
        return new ArrayList<>(activeScoreboards.values());
    }

    public Set<ScoreboardInstance> getByType(String scoreboardId) {
        Set<UUID> players = scoreboardsByType.get(scoreboardId);

        if (players == null || players.isEmpty()) {
            return Collections.emptySet();
        }

        return players.stream()
                .map(activeScoreboards::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public int getActiveCount() {
        return activeScoreboards.size();
    }

    public boolean has(UUID playerId) {
        return activeScoreboards.containsKey(playerId);
    }

    public void clear() {
        activeScoreboards.values().forEach(ScoreboardInstance::hide);
        activeScoreboards.clear();
        scoreboardsByType.clear();
    }
}
