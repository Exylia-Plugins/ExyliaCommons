package net.exylia.commons.v2.region.detection;

import lombok.Getter;
import net.exylia.commons.v2.region.cache.RegionCacheManager;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class MovementDetector {
    private static final double MICRO_MOVEMENT_THRESHOLD = 0.01;
    private static final double TELEPORT_THRESHOLD = 100.0;

    private final RegionCacheManager cacheManager;
    private final PlayerTracker playerTracker;

    @Getter
    private volatile long totalMovements = 0;
    @Getter
    private volatile long processedMovements = 0;
    @Getter
    private volatile long regionChanges = 0;
    @Getter
    private volatile long teleportDetections = 0;

    public MovementDetector(RegionCacheManager cacheManager, PlayerTracker playerTracker) {
        this.cacheManager = cacheManager;
        this.playerTracker = playerTracker;
    }

    public MovementResult checkMovement(Player player, Location from, Location to) {
        totalMovements++;

        if (from == null || to == null || !from.getWorld().equals(to.getWorld())) {
            return new MovementResult(false, Collections.emptySet(), Collections.emptySet(), Collections.emptyList());
        }

        if (isMicroMovement(from, to)) {
            PlayerTracker.PlayerMovementState state = playerTracker.getState(player.getUniqueId());
            if (state != null) {
                return new MovementResult(false, state.getCurrentRegionsAsSet(), state.getCurrentRegionsAsSet(), state.getCurrentRegions());
            }
        }

        processedMovements++;

        boolean isTeleport = isTeleport(from, to);
        if (isTeleport) {
            teleportDetections++;
        }

        PlayerTracker.PlayerMovementState state = playerTracker.getState(player.getUniqueId());
        List<Region> newRegions = cacheManager.getRegionsAt(to);
        Set<Region> newRegionSet = new LinkedHashSet<>(newRegions);

        Set<Region> oldRegionSet;
        if (state == null) {
            oldRegionSet = Collections.emptySet();
        } else {
            oldRegionSet = state.getCurrentRegionsAsSet();
        }

        Set<Region> enterRegions = new LinkedHashSet<>(newRegionSet);
        enterRegions.removeAll(oldRegionSet);

        Set<Region> exitRegions = new LinkedHashSet<>(oldRegionSet);
        exitRegions.removeAll(newRegionSet);

        boolean hasChanges = !enterRegions.isEmpty() || !exitRegions.isEmpty();
        if (hasChanges) {
            regionChanges++;
        }

        playerTracker.updateState(player.getUniqueId(), to, newRegions);

        return new MovementResult(hasChanges, newRegionSet, oldRegionSet, newRegions, enterRegions, exitRegions, isTeleport);
    }

    private boolean isMicroMovement(Location from, Location to) {
        return from.distanceSquared(to) <= MICRO_MOVEMENT_THRESHOLD;
    }

    private boolean isTeleport(Location from, Location to) {
        return from.distanceSquared(to) > TELEPORT_THRESHOLD;
    }

    public void resetStats() {
        totalMovements = 0;
        processedMovements = 0;
        regionChanges = 0;
        teleportDetections = 0;
    }

    public MovementStats getStats() {
        return new MovementStats(totalMovements, processedMovements, regionChanges, teleportDetections, playerTracker.getTrackedPlayerCount());
    }

    @Getter
    public static class MovementResult {
        private final boolean requiresUpdate;
        private final Set<Region> currentRegions;
        private final Set<Region> previousRegions;
        private final List<Region> currentRegionsList;
        private final Set<Region> enterRegions;
        private final Set<Region> exitRegions;
        private final boolean isTeleport;

        public MovementResult(boolean requiresUpdate, Set<Region> currentRegions, Set<Region> previousRegions, List<Region> currentRegionsList) {
            this(requiresUpdate, currentRegions, previousRegions, currentRegionsList, Collections.emptySet(), Collections.emptySet(), false);
        }

        public MovementResult(boolean requiresUpdate, Set<Region> currentRegions, Set<Region> previousRegions,
                              List<Region> currentRegionsList, Set<Region> enterRegions, Set<Region> exitRegions, boolean isTeleport) {
            this.requiresUpdate = requiresUpdate;
            this.currentRegions = new LinkedHashSet<>(currentRegions);
            this.previousRegions = new LinkedHashSet<>(previousRegions);
            this.currentRegionsList = new ArrayList<>(currentRegionsList);
            this.enterRegions = new LinkedHashSet<>(enterRegions);
            this.exitRegions = new LinkedHashSet<>(exitRegions);
            this.isTeleport = isTeleport;
        }

        public boolean hasRegionChanges() {
            return !enterRegions.isEmpty() || !exitRegions.isEmpty();
        }

        public Optional<Region> getHighestPriorityRegion() {
            return currentRegionsList.isEmpty() ? Optional.empty() : Optional.of(currentRegionsList.get(0));
        }

        @Override
        public String toString() {
            return String.format("MovementResult{update=%b, current=%d, previous=%d, enter=%d, exit=%d, teleport=%b}",
                    requiresUpdate, currentRegions.size(), previousRegions.size(), enterRegions.size(), exitRegions.size(), isTeleport);
        }
    }

    @Getter
    public static class MovementStats {
        private final long totalMovements;
        private final long processedMovements;
        private final long regionChanges;
        private final long teleportDetections;
        private final int trackedPlayers;

        public MovementStats(long totalMovements, long processedMovements, long regionChanges, long teleportDetections, int trackedPlayers) {
            this.totalMovements = totalMovements;
            this.processedMovements = processedMovements;
            this.regionChanges = regionChanges;
            this.teleportDetections = teleportDetections;
            this.trackedPlayers = trackedPlayers;
        }

        public double getProcessingRatio() {
            return totalMovements > 0 ? (double) processedMovements / totalMovements : 0.0;
        }

        public double getRegionChangeRatio() {
            return processedMovements > 0 ? (double) regionChanges / processedMovements : 0.0;
        }

        @Override
        public String toString() {
            return String.format("MovementStats{total=%d, processed=%d (%.1f%%), changes=%d (%.1f%%), teleports=%d, tracked=%d}",
                    totalMovements, processedMovements, getProcessingRatio() * 100, regionChanges, getRegionChangeRatio() * 100, teleportDetections, trackedPlayers);
        }
    }
}
