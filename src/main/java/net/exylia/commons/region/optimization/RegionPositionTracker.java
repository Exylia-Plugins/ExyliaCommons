package net.exylia.commons.region.optimization;

import net.exylia.commons.region.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class RegionPositionTracker {
    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final ConcurrentHashMap<UUID, TrackedPosition> lastKnownPositions;

    private BukkitRunnable positionTask;

    private static final int CHECK_INTERVAL_TICKS = 10;  
    private static final double MIN_MOVEMENT_DISTANCE = 0.05;  

    private volatile long totalChecks = 0;
    private volatile long slowMovementsDetected = 0;

    public RegionPositionTracker(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.lastKnownPositions = new ConcurrentHashMap<>();
    }

    public void start() {
        if (positionTask != null) {
            return;  
        }

        positionTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAllPlayerPositions();
            }
        };

        positionTask.runTaskTimer(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);

        logInternalDebug("RegionPositionTracker iniciado - Checking every " +
                (CHECK_INTERVAL_TICKS / 20.0) + " segundos");
    }

    public void stop() {
        if (positionTask != null) {
            positionTask.cancel();
            positionTask = null;
        }

        lastKnownPositions.clear();
        logInternalDebug("RegionPositionTracker detenido");
    }

    private void checkAllPlayerPositions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerPosition(player);
        }
        totalChecks++;
    }

    private void checkPlayerPosition(Player player) {
        UUID playerId = player.getUniqueId();
        Location currentLocation = player.getLocation();

        TrackedPosition lastPosition = lastKnownPositions.get(playerId);

        if (lastPosition == null) {

            lastKnownPositions.put(playerId, new TrackedPosition(currentLocation));
            return;
        }

        if (!lastPosition.location.getWorld().equals(currentLocation.getWorld())) {
            lastKnownPositions.put(playerId, new TrackedPosition(currentLocation));
            return;
        }

        double distanceMoved = lastPosition.location.distance(currentLocation);

        if (distanceMoved >= MIN_MOVEMENT_DISTANCE) {
             
            slowMovementsDetected++;

            boolean movementAllowed = regionManager.processPlayerMovement(
                    player, lastPosition.location, currentLocation);

            if (!movementAllowed) {
                 
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(lastPosition.location);
                        logInternalDebug(String.format(
                                "Slow movement blocked: Teleported %s back to %s",
                                player.getName(), locationToString(lastPosition.location)
                        ));
                    }
                });
            } else {
                 
                lastPosition.update(currentLocation);
            }
        }
    }

    public void updatePlayerPosition(Player player, Location newLocation) {
        UUID playerId = player.getUniqueId();
        lastKnownPositions.put(playerId, new TrackedPosition(newLocation));
    }

    public void cleanupPlayer(UUID playerId) {
        lastKnownPositions.remove(playerId);
    }

    public void forceCheck(Player player) {
        checkPlayerPosition(player);
    }

    public void forceCheckAll() {
        checkAllPlayerPositions();
    }

    public PositionTrackerStats getStats() {
        return new PositionTrackerStats(
                totalChecks,
                slowMovementsDetected,
                lastKnownPositions.size()
        );
    }

    public void resetStats() {
        totalChecks = 0;
        slowMovementsDetected = 0;
    }

    public boolean isActive() {
        return positionTask != null;
    }

    private String locationToString(Location loc) {
        return String.format("(%.2f,%.2f,%.2f)", loc.getX(), loc.getY(), loc.getZ());
    }

    private static class TrackedPosition {
        volatile Location location;
        volatile long lastUpdated;

        TrackedPosition(Location location) {
            this.location = location.clone();
            this.lastUpdated = System.currentTimeMillis();
        }

        void update(Location newLocation) {
            this.location = newLocation.clone();
            this.lastUpdated = System.currentTimeMillis();
        }

        long getTimeSinceUpdate() {
            return System.currentTimeMillis() - lastUpdated;
        }
    }

    public static class PositionTrackerStats {
        private final long totalChecks;
        private final long slowMovementsDetected;
        private final int trackedPlayers;

        public PositionTrackerStats(long totalChecks, long slowMovementsDetected, int trackedPlayers) {
            this.totalChecks = totalChecks;
            this.slowMovementsDetected = slowMovementsDetected;
            this.trackedPlayers = trackedPlayers;
        }

        public long getTotalChecks() { return totalChecks; }
        public long getSlowMovementsDetected() { return slowMovementsDetected; }
        public int getTrackedPlayers() { return trackedPlayers; }

        public double getSlowMovementRatio() {
            return totalChecks > 0 ? (double) slowMovementsDetected / totalChecks : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "PositionTrackerStats{checks=%d, slow_movements=%d (%.3f%%), tracked_players=%d}",
                    totalChecks, slowMovementsDetected, getSlowMovementRatio() * 100, trackedPlayers
            );
        }
    }
}
