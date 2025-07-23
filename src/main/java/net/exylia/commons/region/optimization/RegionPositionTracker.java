package net.exylia.commons.region.optimization;

import net.exylia.commons.region.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

/**
 * Tracker de posición que detecta movimientos muy lentos (shifteando)
 * que no activan el evento PlayerMoveEvent
 */
public class RegionPositionTracker {
    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final ConcurrentHashMap<UUID, TrackedPosition> lastKnownPositions;

    private BukkitRunnable positionTask;

    // Configuración
    private static final int CHECK_INTERVAL_TICKS = 10; // Cada 0.5 segundos
    private static final double MIN_MOVEMENT_DISTANCE = 0.05; // 10cm mínimo

    // Estadísticas
    private volatile long totalChecks = 0;
    private volatile long slowMovementsDetected = 0;

    public RegionPositionTracker(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.lastKnownPositions = new ConcurrentHashMap<>();
    }

    /**
     * Inicia el tracking de posiciones
     */
    public void start() {
        if (positionTask != null) {
            return; // Ya está iniciado
        }

        positionTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAllPlayerPositions();
            }
        };

        // Ejecutar cada 0.5 segundos en el hilo principal
        positionTask.runTaskTimer(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);

        logInternalDebug(debug(), "RegionPositionTracker iniciado - Checking every " +
                (CHECK_INTERVAL_TICKS / 20.0) + " segundos");
    }

    /**
     * Detiene el tracking
     */
    public void stop() {
        if (positionTask != null) {
            positionTask.cancel();
            positionTask = null;
        }

        lastKnownPositions.clear();
        logInternalDebug(debug(), "RegionPositionTracker detenido");
    }

    /**
     * Verifica las posiciones de todos los jugadores conectados
     */
    private void checkAllPlayerPositions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerPosition(player);
        }
        totalChecks++;
    }

    /**
     * Verifica la posición de un jugador específico
     */
    private void checkPlayerPosition(Player player) {
        UUID playerId = player.getUniqueId();
        Location currentLocation = player.getLocation();

        TrackedPosition lastPosition = lastKnownPositions.get(playerId);

        if (lastPosition == null) {
            // Primera vez que vemos a este jugador
            lastKnownPositions.put(playerId, new TrackedPosition(currentLocation));
            return;
        }

        // Verificar si se movió lo suficiente desde la última verificación
        double distanceMoved = lastPosition.location.distance(currentLocation);

        if (distanceMoved >= MIN_MOVEMENT_DISTANCE) {
            // El jugador se movió - procesar como movimiento lento
            slowMovementsDetected++;

//            logInternalDebug(debug(), String.format(
//                    "Slow movement detected for %s: %.3f blocks (from %s to %s)",
//                    player.getName(), distanceMoved,
//                    locationToString(lastPosition.location),
//                    locationToString(currentLocation)
//            ));

            // Procesar el movimiento usando el RegionManager
            boolean movementAllowed = regionManager.processPlayerMovement(
                    player, lastPosition.location, currentLocation);

            if (!movementAllowed) {
                // El movimiento no está permitido - teleportar de vuelta
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(lastPosition.location);
                        logInternalDebug(debug(), String.format(
                                "Slow movement blocked: Teleported %s back to %s",
                                player.getName(), locationToString(lastPosition.location)
                        ));
                    }
                });
            } else {
                // Actualizar posición conocida
                lastPosition.update(currentLocation);
            }
        }
    }

    /**
     * Actualiza la posición conocida de un jugador cuando se mueve normalmente
     * Esto debe ser llamado desde el PlayerMoveEvent para mantener sincronización
     */
    public void updatePlayerPosition(Player player, Location newLocation) {
        UUID playerId = player.getUniqueId();
        lastKnownPositions.put(playerId, new TrackedPosition(newLocation));
    }

    /**
     * Limpia la posición de un jugador cuando se desconecta
     */
    public void cleanupPlayer(UUID playerId) {
        lastKnownPositions.remove(playerId);
    }

    /**
     * Fuerza una verificación inmediata de un jugador específico
     */
    public void forceCheck(Player player) {
        checkPlayerPosition(player);
    }

    /**
     * Fuerza una verificación de todos los jugadores
     */
    public void forceCheckAll() {
        checkAllPlayerPositions();
    }

    /**
     * Obtiene estadísticas del tracker
     */
    public PositionTrackerStats getStats() {
        return new PositionTrackerStats(
                totalChecks,
                slowMovementsDetected,
                lastKnownPositions.size()
        );
    }

    /**
     * Reinicia estadísticas
     */
    public void resetStats() {
        totalChecks = 0;
        slowMovementsDetected = 0;
    }

    /**
     * Verifica si el tracker está activo
     */
    public boolean isActive() {
        return positionTask != null;
    }

    /**
     * Convierte ubicación a string legible
     */
    private String locationToString(Location loc) {
        return String.format("(%.2f,%.2f,%.2f)", loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Clase para almacenar posición rastreada
     */
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

    /**
     * Estadísticas del position tracker
     */
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