package net.exylia.commons.region.optimization;

import net.exylia.commons.region.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MovementOptimizer CORREGIDO - Detección inmediata y precisa de cambios de región
 * Elimina los filtros agresivos que causaban detección tardía
 */
public class MovementOptimizer {
    private final Map<UUID, PlayerMovementState> playerStates;
    private final RegionSpatialIndex spatialIndex;

    // CONFIGURACIÓN MENOS AGRESIVA PARA MEJOR DETECCIÓN
    private static final double MICRO_MOVEMENT_SQUARED = 0.01; // Solo 0.1 bloques - muy pequeño
    private static final long MAX_MOVEMENT_AGE_MS = 10000; // 10 segundos

    // Estadísticas para monitoring
    private volatile long totalMovements = 0;
    private volatile long processedMovements = 0; // Cambio: contar los que procesamos, no los que saltamos
    private volatile long regionChanges = 0;

    public MovementOptimizer(RegionSpatialIndex spatialIndex) {
        this.playerStates = new ConcurrentHashMap<>();
        this.spatialIndex = spatialIndex;
    }

    /**
     * CORREGIDO: Verifica movimiento con detección más sensible
     */
    public MovementResult checkMovement(Player player, Location from, Location to) {
        UUID playerId = player.getUniqueId();
        PlayerMovementState state = playerStates.get(playerId);

        totalMovements++;

        // Verificación de validez básica
        if (from == null || to == null || !from.getWorld().equals(to.getWorld())) {
            return new MovementResult(false, Collections.emptySet(), Collections.emptySet());
        }

        if (state == null) {
            // Primera vez que vemos a este jugador - inicializar estado
            List<Region> initialRegions = spatialIndex.getRegionsAt(to); // CORREGIDO: usar 'to' no 'from'
            state = new PlayerMovementState(to, initialRegions);
            playerStates.put(playerId, state);
            processedMovements++;
            regionChanges++;
            return new MovementResult(true, new HashSet<>(initialRegions), Collections.emptySet());
        }

        // CORREGIDO: Solo filtrar micro-movimientos extremadamente pequeños (0.1 bloque)
        if (isMicroMovement(from, to)) {
            state.updateLastSeen();
            return new MovementResult(false, state.getCurrentRegionsAsSet(), state.getCurrentRegionsAsSet());
        }

        processedMovements++;

        // SIEMPRE obtener regiones nuevas - no usar cache agresivo
        List<Region> newRegions = spatialIndex.getRegionsAt(to);
        Set<Region> newRegionSet = new LinkedHashSet<>(newRegions);

        // DETECCIÓN MÁS SENSIBLE: Comparar sets completos, no solo el más prioritario
        Set<Region> oldRegionSet = state.getCurrentRegionsAsSet();
        boolean regionsChanged = !newRegionSet.equals(oldRegionSet);

        if (regionsChanged) {
            regionChanges++;
            state.updateState(to, newRegions);
            return new MovementResult(true, newRegionSet, oldRegionSet);
        }

        // Las regiones no cambiaron, actualizar ubicación
        state.updateLocation(to);
        return new MovementResult(false, newRegionSet, newRegionSet);
    }

    /**
     * Obtiene el estado actual de un jugador
     */
    public PlayerMovementState getPlayerState(UUID playerId) {
        return playerStates.get(playerId);
    }

    /**
     * Fuerza una actualización del estado de un jugador
     */
    public MovementResult forceUpdate(Player player) {
        UUID playerId = player.getUniqueId();
        Location location = player.getLocation();

        List<Region> currentRegions = spatialIndex.getRegionsAt(location);
        PlayerMovementState state = playerStates.get(playerId);

        if (state == null) {
            state = new PlayerMovementState(location, currentRegions);
            playerStates.put(playerId, state);
            return new MovementResult(true, new HashSet<>(currentRegions), Collections.emptySet());
        }

        Set<Region> oldRegions = state.getCurrentRegionsAsSet();
        state.updateState(location, currentRegions);

        return new MovementResult(true, new HashSet<>(currentRegions), oldRegions);
    }

    /**
     * Limpia el estado de un jugador
     */
    public void cleanupPlayer(UUID playerId) {
        playerStates.remove(playerId);
    }

    /**
     * Limpia estados de jugadores inactivos
     */
    public int cleanupInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        int cleaned = 0;

        Iterator<Map.Entry<UUID, PlayerMovementState>> iterator = playerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerMovementState> entry = iterator.next();
            PlayerMovementState state = entry.getValue();

            if (currentTime - state.lastSeenTime > MAX_MOVEMENT_AGE_MS) {
                iterator.remove();
                cleaned++;
            }
        }

        return cleaned;
    }

    /**
     * CORREGIDO: Solo micro-movimientos extremadamente pequeños
     */
    private boolean isMicroMovement(Location from, Location to) {
        return from.distanceSquared(to) <= MICRO_MOVEMENT_SQUARED;
    }

    /**
     * Obtiene estadísticas del optimizador
     */
    public MovementOptimizerStats getStats() {
        return new MovementOptimizerStats(
                totalMovements,
                processedMovements,
                regionChanges,
                playerStates.size()
        );
    }

    /**
     * Reinicia las estadísticas
     */
    public void resetStats() {
        totalMovements = 0;
        processedMovements = 0;
        regionChanges = 0;
    }

    /**
     * Estado de movimiento de un jugador - Thread-safe
     */
    public static class PlayerMovementState {
        private volatile Location lastCheckedLocation;
        private volatile List<Region> currentRegions;
        private volatile long lastSeenTime;
        private volatile long lastUpdateTime;

        PlayerMovementState(Location location, List<Region> regions) {
            this.lastCheckedLocation = location.clone();
            this.currentRegions = new ArrayList<>(regions);
            this.lastSeenTime = System.currentTimeMillis();
            this.lastUpdateTime = this.lastSeenTime;
        }

        void updateState(Location newLocation, List<Region> newRegions) {
            this.lastCheckedLocation = newLocation.clone();
            this.currentRegions = new ArrayList<>(newRegions);
            this.lastSeenTime = System.currentTimeMillis();
            this.lastUpdateTime = this.lastSeenTime;
        }

        void updateLocation(Location newLocation) {
            this.lastCheckedLocation = newLocation.clone();
            this.lastSeenTime = System.currentTimeMillis();
        }

        void updateLastSeen() {
            this.lastSeenTime = System.currentTimeMillis();
        }

        public Location getLastCheckedLocation() {
            return lastCheckedLocation.clone();
        }

        public List<Region> getCurrentRegions() {
            return new ArrayList<>(currentRegions);
        }

        public Set<Region> getCurrentRegionsAsSet() {
            return new LinkedHashSet<>(currentRegions);
        }

        public Region getHighestPriorityRegion() {
            return currentRegions.isEmpty() ? null : currentRegions.get(0);
        }

        public boolean isInRegion(Region region) {
            return currentRegions.contains(region);
        }

        public long getLastSeenTime() {
            return lastSeenTime;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public long getTimeSinceLastUpdate() {
            return System.currentTimeMillis() - lastUpdateTime;
        }

        @Override
        public String toString() {
            return String.format("PlayerMovementState{location=%s, regions=%d, lastSeen=%dms ago}",
                    lastCheckedLocation, currentRegions.size(),
                    System.currentTimeMillis() - lastSeenTime);
        }
    }

    /**
     * Resultado de verificación de movimiento
     */
    public static class MovementResult {
        public final boolean requiresUpdate;
        public final Set<Region> currentRegions;
        public final Set<Region> previousRegions;

        // Caches para evitar recálculo
        private Set<Region> enterRegions;
        private Set<Region> exitRegions;

        public MovementResult(boolean requiresUpdate, Set<Region> currentRegions, Set<Region> previousRegions) {
            this.requiresUpdate = requiresUpdate;
            this.currentRegions = new LinkedHashSet<>(currentRegions);
            this.previousRegions = new LinkedHashSet<>(previousRegions);
        }

        public Set<Region> getEnterRegions() {
            if (enterRegions == null) {
                enterRegions = new LinkedHashSet<>(currentRegions);
                enterRegions.removeAll(previousRegions);
            }
            return enterRegions;
        }

        public Set<Region> getExitRegions() {
            if (exitRegions == null) {
                exitRegions = new LinkedHashSet<>(previousRegions);
                exitRegions.removeAll(currentRegions);
            }
            return exitRegions;
        }

        public boolean hasRegionChanges() {
            return !getEnterRegions().isEmpty() || !getExitRegions().isEmpty();
        }

        public Region getHighestPriorityCurrentRegion() {
            return currentRegions.isEmpty() ? null : currentRegions.iterator().next();
        }

        @Override
        public String toString() {
            return String.format("MovementResult{update=%b, current=%d, previous=%d, enter=%d, exit=%d}",
                    requiresUpdate, currentRegions.size(), previousRegions.size(),
                    getEnterRegions().size(), getExitRegions().size());
        }
    }

    /**
     * Estadísticas del optimizador de movimiento
     */
    public static class MovementOptimizerStats {
        private final long totalMovements;
        private final long processedMovements;
        private final long regionChanges;
        private final int activePlayerStates;

        public MovementOptimizerStats(long totalMovements, long processedMovements,
                                      long regionChanges, int activePlayerStates) {
            this.totalMovements = totalMovements;
            this.processedMovements = processedMovements;
            this.regionChanges = regionChanges;
            this.activePlayerStates = activePlayerStates;
        }

        public long getTotalMovements() { return totalMovements; }
        public long getProcessedMovements() { return processedMovements; }
        public long getRegionChanges() { return regionChanges; }
        public int getActivePlayerStates() { return activePlayerStates; }

        /**
         * Porcentaje de movimientos que fueron procesados
         */
        public double getProcessedMovementRatio() {
            return totalMovements > 0 ? (double) processedMovements / totalMovements : 0.0;
        }

        /**
         * Porcentaje de movimientos procesados que resultaron en cambios de región
         */
        public double getRegionChangeRatio() {
            return processedMovements > 0 ? (double) regionChanges / processedMovements : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "MovementOptimizerStats{total=%d, processed=%d (%.1f%%), region_changes=%d (%.1f%%), active_states=%d}",
                    totalMovements, processedMovements, getProcessedMovementRatio() * 100,
                    regionChanges, getRegionChangeRatio() * 100, activePlayerStates
            );
        }
    }
}