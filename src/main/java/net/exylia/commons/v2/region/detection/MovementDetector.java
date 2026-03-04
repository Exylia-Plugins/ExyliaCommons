package net.exylia.commons.v2.region.detection;

import lombok.Getter;
import net.exylia.commons.v2.region.detection.PlayerTracker.PlayerMovementState;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class MovementDetector {
    private static final double TELEPORT_THRESHOLD = 100.0;

    private final SpatialIndex spatialIndex;
    private final PlayerTracker playerTracker;

    @Getter
    private volatile long totalMovements = 0;
    @Getter
    private volatile long processedMovements = 0;
    @Getter
    private volatile long regionChanges = 0;
    @Getter
    private volatile long teleportDetections = 0;

    public MovementDetector(SpatialIndex spatialIndex, PlayerTracker playerTracker) {
        this.spatialIndex = spatialIndex;
        this.playerTracker = playerTracker;
    }

    public MovementResult checkMovement(Player player, Location from, Location to) {
        totalMovements++;

        if (from == null || to == null) {
            return MovementResult.NO_CHANGE;
        }

        PlayerMovementState state = playerTracker.getState(player.getUniqueId());
        boolean crossWorld = !from.getWorld().equals(to.getWorld());

        if (!crossWorld && isMicroMovement(from, to) && state != null) {
            return new MovementResult(false, state.getCurrentRegions(), state.getCurrentRegionSet(),
                    Collections.emptyList(), Collections.emptyList(), false);
        }

        processedMovements++;

        List<Region> newRegions = spatialIndex.getRegionsAt(to);
        Set<Region> oldRegionSet = state != null ? state.getCurrentRegionSet() : Collections.emptySet();

        if (crossWorld) {
            teleportDetections++;
            List<Region> exitRegions = state != null && !state.getCurrentRegions().isEmpty()
                    ? new ArrayList<>(state.getCurrentRegions())
                    : Collections.emptyList();
            List<Region> enterRegions = newRegions.isEmpty() ? Collections.emptyList() : new ArrayList<>(newRegions);
            boolean hasChanges = !exitRegions.isEmpty() || !enterRegions.isEmpty();
            if (hasChanges) regionChanges++;
            playerTracker.updateState(player.getUniqueId(), to, newRegions);
            return new MovementResult(hasChanges, newRegions, oldRegionSet, enterRegions, exitRegions, true);
        }

        boolean isTeleport = isTeleport(from, to);
        if (isTeleport) teleportDetections++;

        if (newRegions.size() == oldRegionSet.size() && oldRegionSet.containsAll(newRegions)) {
            playerTracker.updateState(player.getUniqueId(), to, newRegions);
            return new MovementResult(false, newRegions, oldRegionSet,
                    Collections.emptyList(), Collections.emptyList(), isTeleport);
        }

        List<Region> enterRegions = null;
        List<Region> exitRegions = null;

        for (Region region : newRegions) {
            if (!oldRegionSet.contains(region)) {
                if (enterRegions == null) enterRegions = new ArrayList<>(2);
                enterRegions.add(region);
            }
        }

        if (state != null) {
            Set<Region> newRegionSet = newRegions.size() <= 4 ? null : new HashSet<>(newRegions);
            for (Region region : state.getCurrentRegions()) {
                boolean inNew = newRegionSet != null ? newRegionSet.contains(region) : newRegions.contains(region);
                if (!inNew) {
                    if (exitRegions == null) exitRegions = new ArrayList<>(2);
                    exitRegions.add(region);
                }
            }
        }

        if (enterRegions == null) enterRegions = Collections.emptyList();
        if (exitRegions == null) exitRegions = Collections.emptyList();

        boolean hasChanges = !enterRegions.isEmpty() || !exitRegions.isEmpty();
        if (hasChanges) regionChanges++;

        playerTracker.updateState(player.getUniqueId(), to, newRegions);

        return new MovementResult(hasChanges, newRegions, oldRegionSet,
                enterRegions, exitRegions, isTeleport);
    }

    private boolean isMicroMovement(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
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
        static final MovementResult NO_CHANGE = new MovementResult(false, Collections.emptyList(),
                Collections.emptySet(), Collections.emptyList(), Collections.emptyList(), false);

        private final boolean requiresUpdate;
        private final List<Region> currentRegions;
        private final Set<Region> previousRegions;
        private final List<Region> enterRegions;
        private final List<Region> exitRegions;
        private final boolean isTeleport;

        public MovementResult(boolean requiresUpdate, List<Region> currentRegions, Set<Region> previousRegions,
                              List<Region> enterRegions, List<Region> exitRegions, boolean isTeleport) {
            this.requiresUpdate = requiresUpdate;
            this.currentRegions = currentRegions;
            this.previousRegions = previousRegions;
            this.enterRegions = enterRegions;
            this.exitRegions = exitRegions;
            this.isTeleport = isTeleport;
        }

        public boolean hasRegionChanges() {
            return !enterRegions.isEmpty() || !exitRegions.isEmpty();
        }

        public Optional<Region> getHighestPriorityRegion() {
            return currentRegions.isEmpty() ? Optional.empty() : Optional.of(currentRegions.getFirst());
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
