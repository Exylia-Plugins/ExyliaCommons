package net.exylia.commons.region;

import lombok.Getter;
import net.exylia.commons.region.blocks.AllowedBlocksManager;
import net.exylia.commons.region.blocks.TemporaryBlocksManager;
import net.exylia.commons.region.events.*;
import net.exylia.commons.region.listener.UnifiedRegionListener;
import net.exylia.commons.region.model.*;
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

/**
 * Manager principal para el sistema de regiones - ACTUALIZADO con listener unificado
 */
public class RegionManager {
    private static RegionManager instance;

    private final JavaPlugin plugin;
    private final Map<String, Region> regions; // regionId -> Region
    private final Map<World, Set<Region>> worldRegions; // Optimización por mundo
    private final Map<UUID, Set<Region>> playerRegions; // Jugador -> Regiones donde está

    // Configuración del sistema
    @Getter
    private boolean asyncMovementChecking = true;
    @Getter
    private int movementCheckInterval = 1; // ticks

    @Getter
    private FlagManager flagManager;

    // LISTENER UNIFICADO
    private UnifiedRegionListener unifiedListener;

    private BukkitRunnable movementTask;

    private RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regions = new ConcurrentHashMap<>();
        this.worldRegions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();

        // Inicializar sistema de flags
        FlagManager.initialize(plugin);
        this.flagManager = FlagManager.getInstance();

        // Inicializar sistemas auxiliares
        RegionRegenerationManager.initialize(plugin);
        AllowedBlocksManager.initialize(plugin);
        TemporaryBlocksManager.initialize(plugin);
        PlayerBlockTracker.initialize(plugin);

        // USAR LISTENER UNIFICADO
        this.unifiedListener = new UnifiedRegionListener(plugin, this);

        if (asyncMovementChecking) {
            startMovementTask();
        }

        logInternalDebug(debug(), "RegionManager inicializado con listener unificado");
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

    // ===== GESTIÓN DE REGIONES =====

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

        // Optimización por mundo
        worldRegions.computeIfAbsent(region.getWorld(), k -> ConcurrentHashMap.newKeySet())
                .add(region);

        logInternalDebug(debug(), "Región registrada: " + region.getInfo());

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

        // Remover de optimización por mundo
        Set<Region> worldSet = worldRegions.get(region.getWorld());
        if (worldSet != null) {
            worldSet.remove(region);
        }

        // Remover jugadores de la región
        Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
        for (Player player : playersToRemove) {
            handlePlayerExit(player, region);
        }

        logInternalDebug(debug(), "Región eliminada: " + region.getInfo());

        return true;
    }

    public void unregisterAllRegions() {
        Collection<Region> allRegions = new ArrayList<>(regions.values());

        for (Region region : allRegions) {
            // Remover de optimización por mundo
            Set<Region> worldSet = worldRegions.get(region.getWorld());
            if (worldSet != null) {
                worldSet.remove(region);
            }

            // Remover jugadores de la región
            Set<Player> playersToRemove = new HashSet<>(region.getPlayersInside());
            for (Player player : playersToRemove) {
                handlePlayerExit(player, region);
            }
        }

        regions.clear();
        logInternalDebug(debug(), "Todas las regiones han sido eliminadas");
    }

    // ===== BÚSQUEDA DE REGIONES =====

    public Optional<Region> getRegion(String regionId) {
        return Optional.ofNullable(regions.get(regionId));
    }

    /**
     * Obtiene todas las regiones
     */
    public Collection<Region> getRegions() {
        return new ArrayList<>(regions.values());
    }

    /**
     * Obtiene todas las regiones (alias para compatibilidad)
     */
    public Collection<Region> getAllRegions() {
        return getRegions();
    }

    /**
     * Encuentra regiones que contienen una ubicación
     */
    public List<Region> getRegionsAt(Location location) {
        Set<Region> worldSet = worldRegions.get(location.getWorld());
        if (worldSet == null) {
            return Collections.emptyList();
        }

        return worldSet.stream()
                .filter(region -> region.contains(location))
                .sorted((r1, r2) -> r2.getPriority().getLevel() - r1.getPriority().getLevel())
                .collect(Collectors.toList());
    }

    /**
     * Encuentra la región de mayor prioridad en una ubicación
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

    // ===== GESTIÓN DE JUGADORES =====

    /**
     * Obtiene las regiones donde está un jugador
     */
    public Set<Region> getPlayerRegions(Player player) {
        return playerRegions.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    /**
     * Verifica si un jugador está en una región específica
     */
    public boolean isPlayerInRegion(Player player, Region region) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        return regions != null && regions.contains(region);
    }

    public boolean isPlayerInAnyRegion(Player player) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        return regions != null && !regions.isEmpty();
    }

    // ===== MANEJO DE EVENTOS =====

    /**
     * Procesa el movimiento de un jugador
     *
     * @param player El jugador que se mueve
     * @param from   Ubicación de origen
     * @param to     Ubicación de destino
     * @return true si el movimiento está permitido, false si debe ser cancelado
     */
    public boolean processPlayerMovement(Player player, Location from, Location to) {
        // Verificar primero si el movimiento está permitido por las flags
        boolean movementAllowed = flagManager.canPlayerPerformActionAt(player, to, RegionFlag.ENTRY);

        // Si el movimiento no está permitido por flags, cancelar inmediatamente
        if (!movementAllowed) {
            logInternalDebug(debug(), String.format(
                    "Movement blocked by flags for player %s to location %s",
                    player.getName(), to));
            return false;
        }

        // Obtener regiones actuales (donde el jugador está registrado)
        Set<Region> currentRegions = playerRegions.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Obtener regiones nuevas (donde está físicamente ahora)
        List<Region> toRegions = getRegionsAt(to);
        Set<Region> newRegions = new HashSet<>(toRegions);

        // Verificar restricciones de entrada para nuevas regiones
        for (Region region : newRegions) {
            if (!currentRegions.contains(region)) {
                // Es una región nueva, verificar si puede entrar
                if (!canPlayerEnterRegion(player, region, from, to)) {
                    logInternalDebug(debug(), String.format(
                            "Entry blocked for player %s to region %s",
                            player.getName(), region.getId()));
                    return false;
                }
            }
        }

        // Verificar restricciones de salida para regiones que abandona
        Set<Region> exitRegions = new HashSet<>(currentRegions);
        exitRegions.removeAll(newRegions);

        for (Region region : exitRegions) {
            if (!canPlayerExitRegion(player, region, from, to)) {
                logInternalDebug(debug(), String.format(
                        "Exit blocked for player %s from region %s",
                        player.getName(), region.getId()));
                return false;
            }
        }

        // Detectar entradas (no estaba registrado pero ahora está físicamente)
        Set<Region> enterRegions = new HashSet<>(newRegions);
        enterRegions.removeAll(currentRegions);

        if (!exitRegions.isEmpty() || !enterRegions.isEmpty()) {
            logInternalDebug(debug(), String.format(
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
        Set<Region> stayRegions = new HashSet<>(currentRegions);
        stayRegions.retainAll(newRegions);

        for (Region region : stayRegions) {
            handlePlayerMove(player, region, from, to);
        }

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
        logInternalDebug(debug(), String.format(
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
                plugin.getLogger().warning("Error executing onEnter callback: " + e.getMessage());
            }
        }
    }

    /**
     * Maneja la salida de un jugador de una región
     */
    private void handlePlayerExit(Player player, Region region) {
        logInternalDebug(debug(), String.format(
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

        logInternalDebug(debug(), String.format(
                "EXIT event fired for player %s from region %s with flags removed",
                player.getName(), region.getId()));

        // Ejecutar callback personalizado
        if (region.getOnExit() != null) {
            try {
                region.getOnExit().execute(player, region);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing onExit callback: " + e.getMessage());
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
     * Limpia un jugador desconectado
     */
    public void cleanupPlayer(Player player) {
        Set<Region> regions = playerRegions.remove(player.getUniqueId());
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

    // ===== API PARA VALIDACIONES =====

    /**
     * Verifica si un jugador puede realizar una acción específica
     */
    public boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        return flagManager.canPlayerPerformAction(player, flag);
    }

    /**
     * Verifica si un jugador puede realizar una acción en una ubicación específica
     */
    public boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        return flagManager.canPlayerPerformActionAt(player, location, flag);
    }

    /**
     * Verifica si el PvP está permitido entre dos jugadores
     */
    public boolean isPvpAllowed(Player attacker, Player target) {
        return flagManager.isPvpAllowed(attacker, target);
    }

    /**
     * Verifica si un jugador puede colocar un material específico en una ubicación
     */
    public boolean canPlayerPlaceMaterial(Player player, Location location, Material material) {
        // Verificar permisos básicos de construcción primero
        if (!canPlayerPerformActionAt(player, location, RegionFlag.BUILD)) {
            return false;
        }

        // Obtener región de mayor prioridad
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

    /**
     * Fuerza la re-evaluación de flags para un jugador (mejorado)
     */
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

    /**
     * Invalida cache y reaplica flags cuando una región cambia
     */
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

    // ===== CONFIGURACIÓN =====

    public void setAsyncMovementChecking(boolean enabled) {
        this.asyncMovementChecking = enabled;

        if (enabled && movementTask == null) {
            startMovementTask();
        } else if (!enabled && movementTask != null) {
            stopMovementTask();
        }
    }

    public void setMovementCheckInterval(int ticks) {
        this.movementCheckInterval = Math.max(1, ticks);

        if (movementTask != null) {
            stopMovementTask();
            startMovementTask();
        }
    }

    // ===== TAREAS ASÍNCRONAS =====

    private void startMovementTask() {
        if (movementTask != null) {
            return;
        }

        movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Limpiar regiones de jugadores desconectados
                    regions.values().forEach(Region::cleanupOfflinePlayers);
                }
            }
        };

        movementTask.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60); // Cada minuto
    }

    private void stopMovementTask() {
        if (movementTask != null) {
            movementTask.cancel();
            movementTask = null;
        }
    }

    // ===== ESTADÍSTICAS Y GESTIÓN =====

    /**
     * Obtiene estadísticas del sistema de regeneración
     */
    public RegionRegenerationManager.RegenerationStats getRegenerationStats() {
        return RegionRegenerationManager.getInstance().getStats();
    }

    /**
     * Limpia el cache de regeneración
     */
    public void clearRegenerationCache() {
        RegionRegenerationManager.getInstance().clearCache();
    }

    /**
     * Limpia los bloques de jugador de una región
     */
    public void clearRegionPlayerBlocks(String regionId) {
        PlayerBlockTracker.getInstance().clearRegionBlocks(regionId);
    }

    /**
     * Obtiene estadísticas del sistema de rastreo de bloques
     */
    public PlayerBlockTracker.BlockTrackerStats getBlockTrackerStats() {
        return PlayerBlockTracker.getInstance().getStats();
    }

    /**
     * Guarda los datos de bloques de jugador de forma asíncrona
     */
    public CompletableFuture<Void> savePlayerBlocksAsync() {
        return PlayerBlockTracker.getInstance().saveDataAsync();
    }

    /**
     * Obtiene estadísticas del sistema de bloques temporales
     */
    public TemporaryBlocksManager.TemporaryBlocksStats getTemporaryBlocksStats() {
        return TemporaryBlocksManager.getInstance().getStats();
    }

    /**
     * Obtiene todas las regiones que tienen bloques temporales habilitados
     */
    public List<Region> getRegionsWithTemporaryBlocks() {
        return regions.values().stream()
                .filter(Region::hasTemporaryBlocks)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene información de todos los bloques temporales activos en el servidor
     */
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

            logInternalDebug(debug(), "Limpiados " + cleared + " bloques temporales activos");
            return cleared;
        });
    }

    // ===== LIMPIEZA =====

    /**
     * Limpia todos los recursos
     */
    public void cleanup() {
        stopMovementTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }

        // Cerrar sistemas auxiliares
        TemporaryBlocksManager.getInstance().shutdown();
        RegionRegenerationManager.getInstance().shutdown();
        PlayerBlockTracker.getInstance().shutdown();

        regions.clear();
        worldRegions.clear();
        playerRegions.clear();

        logInternalDebug(debug(), "RegionManager limpiado completamente");
    }
}