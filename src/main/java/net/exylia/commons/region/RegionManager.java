package net.exylia.commons.region;

import lombok.Getter;
import net.exylia.commons.region.events.*;
import net.exylia.commons.region.listener.RegionMovementListener;
import net.exylia.commons.region.model.*;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manager principal para el sistema de regiones
 */
public class RegionManager {
    private static RegionManager instance;

    private final JavaPlugin plugin;
    private final Map<String, Map<String, Region>> pluginRegions; // plugin -> regionId -> Region
    private final Map<World, Set<Region>> worldRegions; // Optimización por mundo
    private final Map<UUID, Set<Region>> playerRegions; // Jugador -> Regiones donde está

    @Getter
    private final RegionMovementListener movementListener;

    // Configuración del sistema
    @Getter
    private boolean asyncMovementChecking = true;
    @Getter
    private int movementCheckInterval = 1; // ticks
    @Getter
    private boolean enableDetailedLogging = false;

    private BukkitRunnable movementTask;

    private RegionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pluginRegions = new ConcurrentHashMap<>();
        this.worldRegions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();
        this.movementListener = new RegionMovementListener(this, plugin);

        // Registrar listener
        plugin.getServer().getPluginManager().registerEvents(movementListener, plugin);

        // Habilitar logging detallado por defecto para debugging
        this.enableDetailedLogging = false;

        // Iniciar tarea de movimiento asíncrono
        if (asyncMovementChecking) {
            startMovementTask();
        }

        plugin.getLogger().info("RegionManager inicializado correctamente");
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

    /**
     * Registra una nueva región
     */
    public boolean registerRegion(String pluginName, Region region) {
        if (!region.isValid()) {
            return false;
        }

        // Disparar evento de creación
        RegionCreateEvent createEvent = new RegionCreateEvent(region);
        Bukkit.getPluginManager().callEvent(createEvent);

        if (createEvent.isCancelled()) {
            return false;
        }

        // Registrar región
        pluginRegions.computeIfAbsent(pluginName, k -> new ConcurrentHashMap<>())
                .put(region.getId(), region);

        // Optimización por mundo
        worldRegions.computeIfAbsent(region.getWorld(), k -> ConcurrentHashMap.newKeySet())
                .add(region);

        if (enableDetailedLogging) {
            plugin.getLogger().info("Región registrada: " + region.getInfo());
        }

        return true;
    }

    /**
     * Crea y registra una región desde una selección
     */
    public Region createRegion(String pluginName, String regionId, Selection selection) {
        Region region = new Region(regionId, pluginName, selection);

        if (registerRegion(pluginName, region)) {
            return region;
        }

        return null;
    }

    /**
     * Remueve una región
     */
    public boolean unregisterRegion(String pluginName, String regionId) {
        Map<String, Region> regions = pluginRegions.get(pluginName);
        if (regions == null) {
            return false;
        }

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

        if (enableDetailedLogging) {
            plugin.getLogger().info("Región eliminada: " + region.getInfo());
        }

        return true;
    }

    /**
     * Remueve todas las regiones de un plugin
     */
    public void unregisterAllRegions(String pluginName) {
        Map<String, Region> regions = pluginRegions.remove(pluginName);
        if (regions == null) {
            return;
        }

        for (Region region : regions.values()) {
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

        if (enableDetailedLogging) {
            plugin.getLogger().info("Todas las regiones del plugin " + pluginName + " han sido eliminadas");
        }
    }

    // ===== BÚSQUEDA DE REGIONES =====

    /**
     * Obtiene una región específica
     */
    public Optional<Region> getRegion(String pluginName, String regionId) {
        Map<String, Region> regions = pluginRegions.get(pluginName);
        if (regions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(regions.get(regionId));
    }

    /**
     * Obtiene todas las regiones de un plugin
     */
    public Collection<Region> getRegions(String pluginName) {
        Map<String, Region> regions = pluginRegions.get(pluginName);
        return regions != null ? new ArrayList<>(regions.values()) : Collections.emptyList();
    }

    /**
     * Obtiene todas las regiones
     */
    public Collection<Region> getAllRegions() {
        return pluginRegions.values().stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
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
        return getAllRegions().stream()
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

    /**
     * Verifica si un jugador está en alguna región de un plugin
     */
    public boolean isPlayerInAnyRegion(Player player, String pluginName) {
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        if (regions == null) {
            return false;
        }

        return regions.stream().anyMatch(region -> region.getPluginName().equals(pluginName));
    }

    // ===== MANEJO DE EVENTOS =====

    /**
     * Procesa el movimiento de un jugador
     */
    public void processPlayerMovement(Player player, Location from, Location to) {
        // Obtener regiones actuales (donde el jugador está registrado)
        Set<Region> currentRegions = playerRegions.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Obtener regiones nuevas (donde está físicamente ahora)
        List<Region> toRegions = getRegionsAt(to);
        Set<Region> newRegions = new HashSet<>(toRegions);

        // Detectar salidas (estaba registrado pero ya no está físicamente)
        Set<Region> exitRegions = new HashSet<>(currentRegions);
        exitRegions.removeAll(newRegions);

        // Detectar entradas (no estaba registrado pero ahora está físicamente)
        Set<Region> enterRegions = new HashSet<>(newRegions);
        enterRegions.removeAll(currentRegions);

        if (enableDetailedLogging) {
            if (!exitRegions.isEmpty() || !enterRegions.isEmpty()) {
                plugin.getLogger().info(String.format(
                        "Player %s movement: entering %d regions, exiting %d regions",
                        player.getName(), enterRegions.size(), exitRegions.size()));
            }
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
    }

    /**
     * Maneja la entrada de un jugador a una región
     */
    private void handlePlayerEnter(Player player, Region region) {
        if (enableDetailedLogging) {
            plugin.getLogger().info(String.format(
                    "Processing ENTER for player %s to region %s (%s)",
                    player.getName(), region.getId(), region.getPluginName()));
        }

        // Agregar jugador a la región
        region.addPlayer(player);
        playerRegions.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(region);

        // Disparar evento
        RegionEnterEvent enterEvent = new RegionEnterEvent(player, region);
        Bukkit.getPluginManager().callEvent(enterEvent);

        if (enableDetailedLogging) {
            plugin.getLogger().info(String.format(
                    "ENTER event fired for player %s in region %s",
                    player.getName(), region.getId()));
        }

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
        if (enableDetailedLogging) {
            plugin.getLogger().info(String.format(
                    "Processing EXIT for player %s from region %s (%s)",
                    player.getName(), region.getId(), region.getPluginName()));
        }

        // Remover jugador de la región
        region.removePlayer(player);
        Set<Region> regions = playerRegions.get(player.getUniqueId());
        if (regions != null) {
            regions.remove(region);
        }

        // Disparar evento
        RegionExitEvent exitEvent = new RegionExitEvent(player, region);
        Bukkit.getPluginManager().callEvent(exitEvent);

        if (enableDetailedLogging) {
            plugin.getLogger().info(String.format(
                    "EXIT event fired for player %s from region %s",
                    player.getName(), region.getId()));
        }

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
                region.removePlayer(player);

                // Disparar evento de salida
                RegionExitEvent exitEvent = new RegionExitEvent(player, region);
                Bukkit.getPluginManager().callEvent(exitEvent);
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

    public void setDetailedLogging(boolean enabled) {
        this.enableDetailedLogging = enabled;
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
                    getAllRegions().forEach(Region::cleanupOfflinePlayers);
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

    // ===== LIMPIEZA =====

    /**
     * Limpia todos los recursos
     */
    public void cleanup() {
        stopMovementTask();

        // Limpiar todos los jugadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }

        pluginRegions.clear();
        worldRegions.clear();
        playerRegions.clear();

        if (enableDetailedLogging) {
            plugin.getLogger().info("RegionManager limpiado completamente");
        }
    }

    // ===== ESTADÍSTICAS =====

    /**
     * Obtiene estadísticas del sistema
     */
    public RegionStats getStats() {
        int totalRegions = getAllRegions().size();
        int totalPlugins = pluginRegions.size();
        int totalPlayersInRegions = playerRegions.values().stream()
                .mapToInt(Set::size)
                .sum();

        return new RegionStats(totalRegions, totalPlugins, totalPlayersInRegions);
    }

    /**
     * Clase para estadísticas
     */
    @Getter
    public static class RegionStats {
        private final int totalRegions;
        private final int totalPlugins;
        private final int totalPlayersInRegions;

        public RegionStats(int totalRegions, int totalPlugins, int totalPlayersInRegions) {
            this.totalRegions = totalRegions;
            this.totalPlugins = totalPlugins;
            this.totalPlayersInRegions = totalPlayersInRegions;
        }

        @Override
        public String toString() {
            return String.format("RegionStats{regions=%d, plugins=%d, players=%d}",
                    totalRegions, totalPlugins, totalPlayersInRegions);
        }
    }
}