package net.exylia.commons.region;

import lombok.Getter;
import net.exylia.commons.region.blocks.AllowedBlocksManager;
import net.exylia.commons.region.blocks.TemporaryBlocksManager;
import net.exylia.commons.region.cloning.RegionCloner;
import net.exylia.commons.region.events.*;
import net.exylia.commons.region.listener.UnifiedRegionListener;
import net.exylia.commons.region.model.*;
import net.exylia.commons.region.optimization.RegionPositionTracker;
import net.exylia.commons.region.optimization.RegionSpatialIndex;
import net.exylia.commons.region.optimization.MovementOptimizer;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.exylia.commons.region.flags.FlagManager;
import net.exylia.commons.region.regeneration.RegionRegenerationManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

/**
 * Manager principal para el sistema de regiones - OPTIMIZADO con spatial index
 */
public class RegionManager {
    private static RegionManager instance;

    private final JavaPlugin plugin;
    private final Map<String, Region> regions; // regionId -> Region
    private final Map<UUID, Set<Region>> playerRegions; // Jugador -> Regiones donde está

    // ===== NUEVAS OPTIMIZACIONES CORREGIDAS =====
    @Getter
    private final RegionSpatialIndex spatialIndex; // Índice espacial para búsquedas O(1)
    @Getter
    private final MovementOptimizer movementOptimizer; // Optimizador de movimiento

    // ELIMINADO: Batching system que causaba detección tardía
    // ELIMINADO: pendingMovements y movementProcessor

    // Configuración del sistema
    @Getter
    private boolean asyncMovementChecking = true;
    @Getter
    private int movementCheckInterval = 1; // VUELTO a 1 tick para respuesta inmediata
    @Getter
    private final RegionPositionTracker positionTracker; // NUEVO: Detecta shifteado lento
    @Getter
    private boolean slowMovementDetection = true;
    @Getter
    private FlagManager flagManager;

    // LISTENER UNIFICADO
    private UnifiedRegionListener unifiedListener;

    private BukkitRunnable cleanupTask;

    private RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();

        // Inicializar optimizaciones CORREGIDAS
        this.spatialIndex = new RegionSpatialIndex();
        this.movementOptimizer = new MovementOptimizer(spatialIndex);
        this.positionTracker = new RegionPositionTracker(plugin, this); // NUEVO

        // Inicializar sistema de flags
        FlagManager.initialize(plugin);
        this.flagManager = FlagManager.getInstance();

        // Inicializar sistemas auxiliares
        RegionRegenerationManager.initialize(plugin);
        AllowedBlocksManager.initialize(plugin);
        TemporaryBlocksManager.initialize(plugin, this);
        PlayerBlockTracker.initialize(plugin);

        // USAR LISTENER UNIFICADO CORREGIDO
        this.unifiedListener = new UnifiedRegionListener(plugin, this);

        if (asyncMovementChecking) {
            startCleanupTask(); // Solo tarea de limpieza, no batching
        }
        if (slowMovementDetection) {
            positionTracker.start(); // NUEVO: Iniciar detección de movimiento lento
        }

        logInternalDebug("RegionManager inicializado con detección inmediata optimizada");
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionManager(plugin);
        }
    }

    public static RegionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionManager no ha sido inicializado");
        }
        return instance;
    }

    // ===== GESTIÓN DE REGIONES OPTIMIZADA =====

    public boolean registerRegion(Region region) {
        if (!region.isValid()) {
            return false;
        }

        RegionCreateEvent createEvent = new RegionCreateEvent(region);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(createEvent);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(createEvent);
            });
        }

        if (createEvent.isCancelled()) {
            return false;
        }

        // Registrar región
        regions.put(region.getId(), region);

        // OPTIMIZACIÓN: Registrar en spatial index
        spatialIndex.addRegion(region);

        logInternalDebug("Región registrada con spatial index: " + region.getInfo());

        return true;
    }

    public Region createRegion(String regionId, Selection selection) {
        Region region = new Region(regionId, selection);

        if (registerRegion(region)) {
            return region;
        }

        return null;
    }

    public boolean unregisterRegion(String regionId) {
        Region region = regions.remove(regionId);
        if (region == null) {
            return false;
        }

        // Disparar evento de eliminación
        RegionDeleteEvent deleteEvent = new RegionDeleteEvent(region);
        Bukkit.getPluginManager().callEvent(deleteEvent);

        // OPTIMIZACIÓN: Remover del spatial index
        spatialIndex.removeRegion(region);

        // Remover jugadores de la región
        Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
        for (Player player : playersToRemove) {
            handlePlayerExit(player, region);
        }

        logInternalDebug("Región eliminada del spatial index: " + region.getInfo());

        return true;
    }

    public void unregisterAllRegions() {
        Collection<Region> allRegions = new ArrayList<>(regions.values());

        for (Region region : allRegions) {
            // Remover del spatial index
            spatialIndex.removeRegion(region);

            // Remover jugadores de la región
            Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
            for (Player player : playersToRemove) {
                handlePlayerExit(player, region);
            }
        }

        regions.clear();
        logInternalDebug("Todas las regiones han sido eliminadas y spatial index limpiado");
    }

    // ===== BÚSQUEDA DE REGIONES OPTIMIZADA =====

    public Optional<Region> getRegion(String regionId) {
        return Optional.ofNullable(regions.get(regionId));
    }

    public Collection<Region> getRegions() {
        return new ArrayList<>(regions.values());
    }

    public Collection<Region> getAllRegions() {
        return getRegions();
    }

    /**
     * OPTIMIZADO: Encuentra regiones que contienen una ubicación usando spatial index
     * Complejidad: O(1) promedio en lugar de O(n)
     */
    public List<Region> getRegionsAt(Location location) {
        return spatialIndex.getRegionsAt(location);
    }

    /**
     * OPTIMIZADO: Encuentra la región de mayor prioridad en una ubicación
     */
    public Optional<Region> getHighestPriorityRegionAt(Location location) {
        List<Region> regions = getRegionsAt(location);
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(0));
    }

    /**
     * Busca regiones con un filtro personalizado
     */
    public List<Region> findRegions(Predicate<Region> filter) {
        return regions.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }
    /**
     * Encuentra la región más cercana al jugador de una colección dada
     * @param location La ubicación de referencia
     * @param regions La colección de regiones a evaluar
     * @return Optional con la región más cercana, o empty si la colección está vacía
     */
    public Optional<Region> findClosestRegion(Location location, Collection<Region> regions) {
        if (regions == null || regions.isEmpty()) {
            return Optional.empty();
        }

        Region closestRegion = null;
        double minDistance = Double.MAX_VALUE;

        for (Region region : regions) {
            double distance = calculateDistanceToRegion(location, region);

            if (distance < minDistance) {
                minDistance = distance;
                closestRegion = region;
            }
        }

        return Optional.ofNullable(closestRegion);
    }

    /**
     * Calcula la distancia mínima desde una ubicación hasta una región
     * @param location La ubicación de referencia
     * @param region La región a evaluar
     * @return La distancia mínima a la región (0 si está dentro)
     */
    private double calculateDistanceToRegion(Location location, Region region) {
        // Si el jugador está dentro de la región, la distancia es 0
        if (region.contains(location)) {
            return 0.0;
        }

        // Obtener los puntos mínimo y máximo de la región
        Location minPoint = region.getMinimumPoint();
        Location maxPoint = region.getMaximumPoint();

        // Verificar que estén en el mismo mundo
        if (!location.getWorld().equals(minPoint.getWorld())) {
            return Double.MAX_VALUE; // Distancia infinita si están en mundos diferentes
        }

        // Calcular el punto más cercano en la región al jugador
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        // Clamp coordinates to region bounds
        double closestX = Math.max(minPoint.getX(), Math.min(x, maxPoint.getX()));
        double closestY = Math.max(minPoint.getY(), Math.min(y, maxPoint.getY()));
        double closestZ = Math.max(minPoint.getZ(), Math.min(z, maxPoint.getZ()));

        // Calcular distancia euclidiana
        double dx = x - closestX;
        double dy = y - closestY;
        double dz = z - closestZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ===== GESTIÓN DE JUGADORES =====

    public Set<Region> getPlayerRegions(Player player) {
        return playerRegions.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    public boolean isPlayerInRegion(Player player, Region region) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        return regions != null && regions.contains(region);
    }

    public boolean isPlayerInAnyRegion(Player player) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        return regions != null && !regions.isEmpty();
    }

    // ===== MANEJO DE EVENTOS OPTIMIZADO =====

    /**
     * CORREGIDO: Procesa el movimiento de un jugador INMEDIATAMENTE
     * Sin batching para mejor detección de cambios de región
     */
    public boolean processPlayerMovement(Player player, Location from, Location to) {
        // Usar movement optimizer CORREGIDO para reducir verificaciones innecesarias
        MovementOptimizer.MovementResult result = movementOptimizer.checkMovement(player, from, to);

        if (!result.requiresUpdate) {
            positionTracker.updatePlayerPosition(player, to);
            return true;
        }

        // Verificar primero si el movimiento está permitido por las flags
        boolean movementAllowed = flagManager.canPlayerPerformActionAt(player, to, RegionFlag.ENTRY);
        if (!movementAllowed) {
            logInternalDebug(String.format(
                    "Movement blocked by flags for player %s to location %s",
                    player.getName(), to));
            return false;
        }

        Set<Region> currentRegions = result.currentRegions;
        Set<Region> previousRegions = result.previousRegions;

        // Verificar restricciones de entrada para nuevas regiones
        Set<Region> enterRegions = result.getEnterRegions();
        for (Region region : enterRegions) {
            if (!canPlayerEnterRegion(player, region, from, to)) {
                logInternalDebug(String.format(
                        "Entry blocked for player %s to region %s",
                        player.getName(), region.getId()));
                return false;
            }
        }

        // Verificar restricciones de salida para regiones que abandona
        Set<Region> exitRegions = result.getExitRegions();
        for (Region region : exitRegions) {
            if (!canPlayerExitRegion(player, region, from, to)) {
                logInternalDebug(String.format(
                        "Exit blocked for player %s from region %s",
                        player.getName(), region.getId()));
                return false;
            }
        }

        if (!exitRegions.isEmpty() || !enterRegions.isEmpty()) {
            logInternalDebug(String.format(
                    "Player %s movement: entering %d regions, exiting %d regions",
                    player.getName(), enterRegions.size(), exitRegions.size()));
        }

        // Procesar salidas PRIMERO
        for (Region region : exitRegions) {
            handlePlayerExit(player, region);
        }

        // Procesar entradas DESPUÉS
        for (Region region : enterRegions) {
            handlePlayerEnter(player, region);
        }

        // Procesar movimiento dentro de regiones que permanecen
        Set<Region> stayRegions = new HashSet<>(previousRegions);
        stayRegions.retainAll(currentRegions);

        for (Region region : stayRegions) {
            handlePlayerMove(player, region, from, to);
        }
        positionTracker.updatePlayerPosition(player, to);
        return true; // Movimiento permitido
    }

    /**
     * Verifica si un jugador puede entrar a una región específica
     */
    private boolean canPlayerEnterRegion(Player player, Region region, Location from, Location to) {
        // Disparar evento de verificación de entrada
        RegionPreEnterEvent preEnterEvent = new RegionPreEnterEvent(player, region, from, to);
        Bukkit.getPluginManager().callEvent(preEnterEvent);

        if (preEnterEvent.isCancelled()) {
            if (preEnterEvent.getCancelMessage() != null) {
                player.sendMessage(preEnterEvent.getCancelMessage());
            }
            return false;
        }

        // Verificar flag de entrada
        if (!region.getFlagValue(RegionFlag.ENTRY)) {
            // Verificar si el jugador tiene permisos para ignorar la restricción
            if (!player.hasPermission("exylia.region.bypass.entry") &&
                    !region.isMember(player.getUniqueId()) &&
                    !region.isOwner(player.getUniqueId())) {
                return false;
            }
        }

        // Verificar whitelist
        if (region.getMetadata("whitelist-only", Boolean.class) == Boolean.TRUE) {
            return region.isMember(player.getUniqueId()) || region.isOwner(player.getUniqueId());
        }

        return true;
    }

    /**
     * Verifica si un jugador puede salir de una región específica
     */
    private boolean canPlayerExitRegion(Player player, Region region, Location from, Location to) {
        // Disparar evento de verificación de salida
        RegionPreExitEvent preExitEvent = new RegionPreExitEvent(player, region, from, to);
        Bukkit.getPluginManager().callEvent(preExitEvent);

        if (preExitEvent.isCancelled()) {
            if (preExitEvent.getCancelMessage() != null) {
                player.sendMessage(preExitEvent.getCancelMessage());
            }
            return false;
        }

        // Verificar flag de salida
        if (!region.getFlagValue(RegionFlag.EXIT)) {
            // Verificar si el jugador tiene permisos para ignorar la restricción
            if (!player.hasPermission("exylia.region.bypass.exit") &&
                    !region.isOwner(player.getUniqueId())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Maneja la entrada de un jugador a una región
     */
    private void handlePlayerEnter(Player player, Region region) {
        logInternalDebug(String.format(
                "Processing ENTER for player %s to region %s",
                player.getName(), region.getId()));

        // Agregar jugador a la región
        region.addPlayer(player);
        playerRegions.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(region);

        // Aplicar efectos de flags
        flagManager.applyRegionEffects(player, region);

        // Disparar evento
        RegionEnterEvent enterEvent = new RegionEnterEvent(player, region);
        Bukkit.getPluginManager().callEvent(enterEvent);

        // Ejecutar callback personalizado
        if (region.getOnEnter() != null) {
            try {
                region.getOnEnter().execute(player, region);
            } catch (Exception e) {
                logInternalWarn("Error executing onEnter callback: " + e.getMessage());
            }
        }
    }

    /**
     * Maneja la salida de un jugador de una región
     */
    private void handlePlayerExit(Player player, Region region) {
        logInternalDebug(String.format(
                "Processing EXIT for player %s from region %s",
                player.getName(), region.getId()));

        // Remover efectos de flags ANTES de remover al jugador
        flagManager.removeRegionEffects(player, region);

        // Remover jugador de la región
        region.removePlayer(player);
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        if (regions != null) {
            regions.remove(region);
        }

        // Disparar evento
        RegionExitEvent exitEvent = new RegionExitEvent(player, region);
        Bukkit.getPluginManager().callEvent(exitEvent);

        logInternalDebug(String.format(
                "EXIT event fired for player %s from region %s with flags removed",
                player.getName(), region.getId()));

        // Ejecutar callback personalizado
        if (region.getOnExit() != null) {
            try {
                region.getOnExit().execute(player, region);
            } catch (Exception e) {
                logInternalWarn("Error executing onExit callback: " + e.getMessage());
            }
        }
    }

    /**
     * Maneja el movimiento de un jugador dentro de una región
     */
    private void handlePlayerMove(Player player, Region region, Location from, Location to) {
        // Disparar evento
        RegionMoveEvent moveEvent = new RegionMoveEvent(player, region, from, to);
        Bukkit.getPluginManager().callEvent(moveEvent);

        // Ejecutar callback personalizado
        if (region.getOnMove() != null) {
            region.getOnMove().execute(player, region);
        }
    }

    /**
     * CORREGIDO: Limpia un jugador desconectado sin batching
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        // Limpiar del movement optimizer
        movementOptimizer.cleanupPlayer(playerId);
        positionTracker.cleanupPlayer(playerId);

        Set<Region> regions = playerRegions.remove(playerId);
        if (regions != null) {
            for (Region region : regions) {
                // Remover efectos de flags antes de limpiar
                flagManager.removeRegionEffects(player, region);

                region.removePlayer(player);

                // Disparar evento de salida
                RegionExitEvent exitEvent = new RegionExitEvent(player, region);
                Bukkit.getPluginManager().callEvent(exitEvent);
            }
        }

        // Limpiar estado de flags del jugador
        flagManager.cleanupPlayerState(player);
    }

    // ===== API PARA VALIDACIONES (sin cambios, usa las optimizaciones internas) =====

    public boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        return flagManager.canPlayerPerformAction(player, flag);
    }

    public boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        return flagManager.canPlayerPerformActionAt(player, location, flag);
    }

    public boolean isPvpAllowed(Player attacker, Player target) {
        return flagManager.isPvpAllowed(attacker, target);
    }

    public boolean canPlayerPlaceMaterial(Player player, Location location, Material material) {
        // Verificar permisos básicos de construcción primero
        if (!canPlayerPerformActionAt(player, location, RegionFlag.BUILD)) {
            return false;
        }

        // Obtener región de mayor prioridad usando spatial index
        List<Region> regions = getRegionsAt(location);
        if (regions.isEmpty()) {
            return true; // No hay regiones, permitir
        }

        Region region = regions.get(0);

        // Si la región tiene restricción de bloques permitidos, verificar
        if (region.hasAllowedBlocksOnly()) {
            return region.isMaterialAllowed(material);
        }

        return true; // No hay restricciones especiales
    }

    public void refreshPlayerFlags(Player player) {
        Set<Region> currentRegions = playerRegions.get(player.getUniqueId());
        if (currentRegions != null) {
            // Remover efectos actuales
            for (Region region : currentRegions) {
                flagManager.removeRegionEffects(player, region);
            }

            // Reaplicar efectos basados en ubicación actual
            List<Region> newRegions = getRegionsAt(player.getLocation());
            for (Region region : newRegions) {
                if (currentRegions.contains(region)) {
                    flagManager.applyRegionEffects(player, region);
                }
            }
        }

        // Invalidar cache específico del jugador
        flagManager.invalidatePlayerCache(player);
    }

    public void onRegionFlagChanged(Region region, RegionFlag flag) {
        // Invalidar cache
        flagManager.invalidateRegionFlagCache(region, flag);

        // Rerefrescar jugadores en la región
        for (Player player : region.getPlayersInside()) {
            // Solo rerefrescar si es una flag que afecta al jugador actual
            if (flag.affectsCombat() || flag.affectsMovement() || flag.affectsBuilding()) {
                refreshPlayerFlags(player);
            }
        }
    }

    public void setSlowMovementDetection(boolean enabled) {
        this.slowMovementDetection = enabled;

        if (enabled) {
            positionTracker.start();
            logInternalDebug("Slow movement detection ENABLED");
        } else {
            positionTracker.stop();
            logInternalDebug("Slow movement detection DISABLED");
        }
    }

    public void forcePositionCheck(Player player) {
        positionTracker.forceCheck(player);
    }

    public void forcePositionCheckAll() {
        positionTracker.forceCheckAll();
    }

    public RegionPositionTracker.PositionTrackerStats getPositionTrackerStats() {
        return positionTracker.getStats();
    }

    // ===== CONFIGURACIÓN CORREGIDA =====

    public void setAsyncMovementChecking(boolean enabled) {
        this.asyncMovementChecking = enabled;

        if (enabled && cleanupTask == null) {
            startCleanupTask();
        } else if (!enabled && cleanupTask != null) {
            stopCleanupTask();
        }
    }

    public void setMovementCheckInterval(int ticks) {
        this.movementCheckInterval = Math.max(1, ticks);
        // Note: No necesitamos reiniciar tareas ya que no hay batching
    }

    // ===== TAREAS CORREGIDAS (solo limpieza) =====

    private void startCleanupTask() {
        if (cleanupTask != null) {
            return;
        }

        // Solo tarea de limpieza - SIN procesamiento de batch
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Limpiar jugadores offline
                for (Player player : Bukkit.getOnlinePlayers()) {
                    regions.values().forEach(Region::cleanupOfflinePlayers);
                }

                // Limpiar estados inactivos del movement optimizer
                movementOptimizer.cleanupInactivePlayers();
            }
        };
        cleanupTask.runTaskTimerAsynchronously(plugin, 20L * 30, 20L * 30); // Cada 30 segundos
    }

    private void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    // ===== ESTADÍSTICAS Y GESTIÓN =====

    public RegionRegenerationManager.RegenerationStats getRegenerationStats() {
        return RegionRegenerationManager.getInstance().getStats();
    }

    public void clearRegenerationCache() {
        RegionRegenerationManager.getInstance().clearCache();
    }

    public void clearRegionPlayerBlocks(String regionId) {
        PlayerBlockTracker.getInstance().clearRegionBlocks(regionId);
    }

    public PlayerBlockTracker.BlockTrackerStats getBlockTrackerStats() {
        return PlayerBlockTracker.getInstance().getStats();
    }

    public CompletableFuture<Void> savePlayerBlocksAsync() {
        return PlayerBlockTracker.getInstance().saveDataAsync();
    }

    public TemporaryBlocksManager.TemporaryBlocksStats getTemporaryBlocksStats() {
        return TemporaryBlocksManager.getInstance().getStats();
    }

    public List<Region> getRegionsWithTemporaryBlocks() {
        return regions.values().stream()
                .filter(Region::hasTemporaryBlocks)
                .collect(Collectors.toList());
    }

    public Map<String, List<TemporaryBlocksManager.TemporaryBlock>> getActiveTemporaryBlocks() {
        Map<String, List<TemporaryBlocksManager.TemporaryBlock>> activeBlocks = new HashMap<>();

        for (Region region : getRegionsWithTemporaryBlocks()) {
            List<TemporaryBlocksManager.TemporaryBlock> regionBlocks = new ArrayList<>();

            // Recorrer el área de la región buscando bloques temporales
            Location min = region.getMinimumPoint();
            Location max = region.getMaximumPoint();

            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        Location loc = new Location(min.getWorld(), x, y, z);
                        TemporaryBlocksManager.TemporaryBlock tempBlock =
                                TemporaryBlocksManager.getInstance().getTemporaryBlock(loc);

                        if (tempBlock != null) {
                            regionBlocks.add(tempBlock);
                        }
                    }
                }
            }

            if (!regionBlocks.isEmpty()) {
                activeBlocks.put(region.getId(), regionBlocks);
            }
        }

        return activeBlocks;
    }

    public CompletableFuture<Integer> clearAllTemporaryBlocks() {
        return CompletableFuture.supplyAsync(() -> {
            int cleared = 0;
            Map<String, List<TemporaryBlocksManager.TemporaryBlock>> activeBlocks = getActiveTemporaryBlocks();

            for (Map.Entry<String, List<TemporaryBlocksManager.TemporaryBlock>> entry : activeBlocks.entrySet()) {
                for (TemporaryBlocksManager.TemporaryBlock block : entry.getValue()) {
                    TemporaryBlocksManager.getInstance().cancelBlockRemoval(block.getLocation());
                    block.getLocation().getBlock().setType(Material.AIR);
                    cleared++;
                }
            }

            logInternalDebug("Limpiados " + cleared + " bloques temporales activos");
            return cleared;
        });
    }

// ===== LIMPIEZA =====

    public void cleanup() {
        stopCleanupTask();
        positionTracker.stop();


        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }

        // Cerrar sistemas auxiliares
        TemporaryBlocksManager.getInstance().shutdown();
        RegionRegenerationManager.getInstance().shutdown();
        PlayerBlockTracker.getInstance().shutdown();

        regions.clear();
        playerRegions.clear();
        RegionCloner.getInstance().cleanup();

        logInternalDebug("RegionManager optimizado limpiado completamente");
    }

// ===== CLASE AUXILIAR =====

    /**
     * Representa un movimiento pendiente para procesamiento en batch
     */
    private static class PendingMovement {
        final Player player;
        final Location from;
        final Location to;
        final long timestamp;

        PendingMovement(Player player, Location from, Location to, long timestamp) {
            this.player = player;
            this.from = from.clone();
            this.to = to.clone();
            this.timestamp = timestamp;
        }
    }

    public CompletableFuture<Region> cloneRegion(String sourceRegionId, Location targetCenter) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return RegionCloner.getInstance().cloneRegion(sourceRegion.get(), targetCenter);
    }

    public CompletableFuture<Region> cloneRegion(String sourceRegionId, Location targetCenter, String customName) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return RegionCloner.getInstance().cloneRegion(sourceRegion.get(), targetCenter, customName);
    }

    public CompletableFuture<Region> cloneRegion(String sourceRegionId, Location targetCenter, World targetWorld) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return RegionCloner.getInstance().cloneRegion(sourceRegion.get(), targetCenter, targetWorld);
    }

    public CompletableFuture<Boolean> cloneMultipleRegions(String[] sourceRegionIds, Location[] targetCenters) {
        if (sourceRegionIds.length != targetCenters.length) {
            return CompletableFuture.completedFuture(false);
        }

        Region[] sourceRegions = new Region[sourceRegionIds.length];
        for (int i = 0; i < sourceRegionIds.length; i++) {
            Optional<Region> region = getRegion(sourceRegionIds[i]);
            if (region.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            sourceRegions[i] = region.get();
        }

        return RegionCloner.getInstance().cloneMultipleRegions(sourceRegions, targetCenters);
    }

    public boolean isRegionBeingCloned(String regionId) {
        return RegionCloner.getInstance().isCloneInProgress(regionId);
    }

    public int getActiveCloneOperationsCount() {
        return RegionCloner.getInstance().getActiveCloneCount();
    }

    public RegionCloner.CloneOperation getCloneOperation(String operationId) {
        return RegionCloner.getInstance().getCloneOperation(operationId);
    }

    public CompletableFuture<Boolean> copyRegionStructure(String sourceRegionId, Location targetCenter) {
        Optional<Region> sourceRegion = getRegion(sourceRegionId);
        if (sourceRegion.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        return RegionRegenerationManager.getInstance().copyRegionStructure(sourceRegion.get(), targetCenter);
    }

    public Collection<Region> findRegionsByPattern(String pattern) {
        return regions.values().stream()
                .filter(region -> region.getId().contains(pattern) ||
                        region.getDisplayName().contains(pattern))
                .collect(Collectors.toList());
    }

    public Collection<Region> getRegionsInWorld(World world) {
        return regions.values().stream()
                .filter(region -> region.getWorld().equals(world))
                .collect(Collectors.toList());
    }

    public Optional<Region> findCloneSource(String cloneRegionId) {
        if (!cloneRegionId.contains("_clone_")) {
            return Optional.empty();
        }

        String sourceId = cloneRegionId.substring(0, cloneRegionId.indexOf("_clone_"));
        return getRegion(sourceId);
    }

    public Collection<Region> findAllClonesOf(String sourceRegionId) {
        String clonePrefix = sourceRegionId + "_clone_";
        return regions.values().stream()
                .filter(region -> region.getId().startsWith(clonePrefix))
                .collect(Collectors.toList());
    }
}
