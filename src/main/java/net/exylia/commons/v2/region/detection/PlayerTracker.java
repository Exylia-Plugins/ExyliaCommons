package net.exylia.commons.v2.region.detection;

import lombok.Getter;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTracker {
    private final Map<UUID, PlayerMovementState> playerStates;
    private static final long MAX_STATE_AGE_MS = 600_000;

    public PlayerTracker() {
        this.playerStates = new ConcurrentHashMap<>();
    }

    public PlayerMovementState getState(UUID playerId) {
        return playerStates.get(playerId);
    }

    public void updateState(UUID playerId, Location location, List<Region> regions) {
        PlayerMovementState state = playerStates.get(playerId);
        if (state == null) {
            state = new PlayerMovementState(location, regions);
            playerStates.put(playerId, state);
        } else {
            state.update(location, regions);
        }
    }

    public void removePlayer(UUID playerId) {
        playerStates.remove(playerId);
    }

    public int cleanupInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        int cleaned = 0;

        Iterator<Map.Entry<UUID, PlayerMovementState>> iterator = playerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerMovementState> entry = iterator.next();
            if (currentTime - entry.getValue().getLastUpdate() > MAX_STATE_AGE_MS) {
                iterator.remove();
                cleaned++;
            }
        }

        return cleaned;
    }

    public int getTrackedPlayerCount() {
        return playerStates.size();
    }

    public void clear() {
        playerStates.clear();
    }

    @Getter
    public static class PlayerMovementState {
        private volatile Location lastLocation;
        private volatile List<Region> currentRegions;
        private volatile Set<Region> currentRegionSet;
        private volatile long lastUpdate;

        public PlayerMovementState(Location location, List<Region> regions) {
            this.lastLocation = location;
            this.currentRegions = List.copyOf(regions);
            this.currentRegionSet = Set.copyOf(regions);
            this.lastUpdate = System.currentTimeMillis();
        }

        public void update(Location location, List<Region> regions) {
            this.lastLocation = location;
            this.currentRegions = List.copyOf(regions);
            this.currentRegionSet = Set.copyOf(regions);
            this.lastUpdate = System.currentTimeMillis();
        }

        public long getTimeSinceUpdate() {
            return System.currentTimeMillis() - lastUpdate;
        }

        @Override
        public String toString() {
            return String.format("PlayerMovementState{location=%s, regions=%d, lastUpdate=%dms ago}",
                    lastLocation, currentRegions.size(), getTimeSinceUpdate());
        }
    }
}
