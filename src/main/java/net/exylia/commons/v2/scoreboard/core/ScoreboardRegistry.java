package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ScoreboardRegistry {

    private final ConcurrentHashMap<UUID, Deque<ScoreboardInstance>> scoreboardStacks;
    private final ConcurrentHashMap<String, Set<UUID>> scoreboardsByType;
    private final ConcurrentHashMap<UUID, Scoreboard> originalBukkitScoreboards;

    public ScoreboardRegistry() {
        this.scoreboardStacks = new ConcurrentHashMap<>();
        this.scoreboardsByType = new ConcurrentHashMap<>();
        this.originalBukkitScoreboards = new ConcurrentHashMap<>();
    }

    public void push(UUID playerId, ScoreboardInstance instance) {
        Deque<ScoreboardInstance> stack = scoreboardStacks.computeIfAbsent(
                playerId,
                k -> new ArrayDeque<>()
        );

        stack.push(instance);

        String scoreboardId = instance.getScoreboard().getId();
        scoreboardsByType.computeIfAbsent(scoreboardId, k -> ConcurrentHashMap.newKeySet())
                .add(playerId);
    }

    public Optional<ScoreboardInstance> pop(UUID playerId) {
        Deque<ScoreboardInstance> stack = scoreboardStacks.get(playerId);

        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        ScoreboardInstance removed = stack.pop();

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

        if (stack.isEmpty()) {
            scoreboardStacks.remove(playerId);
        }

        return Optional.ofNullable(removed);
    }

    public Optional<ScoreboardInstance> peek(UUID playerId) {
        Deque<ScoreboardInstance> stack = scoreboardStacks.get(playerId);

        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(stack.peek());
    }

    @Deprecated
    public void register(UUID playerId, ScoreboardInstance instance) {
        push(playerId, instance);
    }

    @Deprecated
    public void unregister(UUID playerId) {
        pop(playerId);
    }

    public Optional<ScoreboardInstance> get(UUID playerId) {
        return peek(playerId);
    }

    public Set<ScoreboardInstance> getAll() {
        return scoreboardStacks.values().stream()
                .flatMap(Deque::stream)
                .collect(Collectors.toSet());
    }

    public List<ScoreboardInstance> getAllAsList() {
        return scoreboardStacks.values().stream()
                .flatMap(Deque::stream)
                .collect(Collectors.toList());
    }

    public Set<ScoreboardInstance> getByType(String scoreboardId) {
        Set<UUID> players = scoreboardsByType.get(scoreboardId);

        if (players == null || players.isEmpty()) {
            return Collections.emptySet();
        }

        return players.stream()
                .map(this::peek)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(instance -> instance.getScoreboard().getId().equals(scoreboardId))
                .collect(Collectors.toSet());
    }

    public int getActiveCount() {
        return scoreboardStacks.size();
    }

    public int getTotalStackedCount() {
        return scoreboardStacks.values().stream()
                .mapToInt(Deque::size)
                .sum();
    }

    public boolean has(UUID playerId) {
        Deque<ScoreboardInstance> stack = scoreboardStacks.get(playerId);
        return stack != null && !stack.isEmpty();
    }

    public int getStackSize(UUID playerId) {
        Deque<ScoreboardInstance> stack = scoreboardStacks.get(playerId);
        return stack != null ? stack.size() : 0;
    }

    public void saveOriginalScoreboard(UUID playerId, Scoreboard scoreboard) {
        if (scoreboard != null && !originalBukkitScoreboards.containsKey(playerId)) {
            originalBukkitScoreboards.put(playerId, scoreboard);
        }
    }

    public Optional<Scoreboard> getOriginalScoreboard(UUID playerId) {
        return Optional.ofNullable(originalBukkitScoreboards.get(playerId));
    }

    public void removeOriginalScoreboard(UUID playerId) {
        originalBukkitScoreboards.remove(playerId);
    }

    public void clear() {
        scoreboardStacks.values().stream()
                .flatMap(Deque::stream)
                .forEach(ScoreboardInstance::hide);
        scoreboardStacks.clear();
        scoreboardsByType.clear();
        originalBukkitScoreboards.clear();
    }
}
