package net.exylia.commons.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utilidades para integración con WorldGuard
 * Proporciona métodos para obtener regiones en ubicaciones específicas
 */
public class WorldGuardUtils {

    private static boolean worldGuardAvailable = false;

    static {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            worldGuardAvailable = true;
        } catch (ClassNotFoundException e) {
            worldGuardAvailable = false;
        }
    }

    /**
     * Verifica si WorldGuard está disponible
     * @return true si WorldGuard está cargado
     */
    public static boolean isWorldGuardAvailable() {
        return worldGuardAvailable && Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    /**
     * Obtiene todas las regiones en la ubicación del jugador
     * @param player Jugador
     * @return Lista de nombres de regiones (vacía si no hay WorldGuard o regiones)
     */
    public static List<String> getRegionsAtPlayer(Player player) {
        return getRegionsAtLocation(player.getLocation());
    }

    /**
     * Obtiene todas las regiones en una ubicación específica
     * @param location Ubicación
     * @return Lista de nombres de regiones (vacía si no hay WorldGuard o regiones)
     */
    public static List<String> getRegionsAtLocation(Location location) {
        if (!isWorldGuardAvailable() || location.getWorld() == null) {
            return new ArrayList<>();
        }

        try {
            World world = location.getWorld();
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return new ArrayList<>();
            }

            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                    BukkitAdapter.asBlockVector(location)
            );

            List<String> regionNames = new ArrayList<>();
            for (ProtectedRegion region : regions) {
                regionNames.add(region.getId());
            }

            return regionNames;

        } catch (Exception e) {
            // Si hay algún error con WorldGuard, devolver lista vacía
            return new ArrayList<>();
        }
    }

    /**
     * Verifica si el jugador está en una región específica
     * @param player Jugador
     * @param regionName Nombre de la región
     * @return true si está en la región
     */
    public static boolean isPlayerInRegion(Player player, String regionName) {
        return isLocationInRegion(player.getLocation(), regionName);
    }

    /**
     * Verifica si una ubicación está en una región específica
     * @param location Ubicación
     * @param regionName Nombre de la región
     * @return true si está en la región
     */
    public static boolean isLocationInRegion(Location location, String regionName) {
        List<String> regions = getRegionsAtLocation(location);
        return regions.contains(regionName);
    }

    /**
     * Verifica si el jugador está en cualquiera de las regiones dadas
     * @param player Jugador
     * @param regionNames Lista de nombres de regiones
     * @return true si está en al menos una de las regiones
     */
    public static boolean isPlayerInAnyRegion(Player player, List<String> regionNames) {
        return isLocationInAnyRegion(player.getLocation(), regionNames);
    }

    /**
     * Verifica si una ubicación está en cualquiera de las regiones dadas
     * @param location Ubicación
     * @param regionNames Lista de nombres de regiones
     * @return true si está en al menos una de las regiones
     */
    public static boolean isLocationInAnyRegion(Location location, List<String> regionNames) {
        if (regionNames.isEmpty()) {
            return false;
        }

        List<String> playerRegions = getRegionsAtLocation(location);
        return playerRegions.stream().anyMatch(regionNames::contains);
    }

    /**
     * Obtiene la región de mayor prioridad en la ubicación del jugador
     * @param player Jugador
     * @return Nombre de la región de mayor prioridad, o null si no hay regiones
     */
    public static String getHighestPriorityRegion(Player player) {
        return getHighestPriorityRegion(player.getLocation());
    }

    /**
     * Obtiene la región de mayor prioridad en una ubicación específica
     * @param location Ubicación
     * @return Nombre de la región de mayor prioridad, o null si no hay regiones
     */
    public static String getHighestPriorityRegion(Location location) {
        if (!isWorldGuardAvailable() || location.getWorld() == null) {
            return null;
        }

        try {
            World world = location.getWorld();
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return null;
            }

            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                    BukkitAdapter.asBlockVector(location)
            );

            ProtectedRegion highestPriorityRegion = null;
            int highestPriority = Integer.MIN_VALUE;

            for (ProtectedRegion region : regions) {
                if (region.getPriority() > highestPriority) {
                    highestPriority = region.getPriority();
                    highestPriorityRegion = region;
                }
            }

            return highestPriorityRegion != null ? highestPriorityRegion.getId() : null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica si existe una región específica en un mundo
     * @param world Mundo
     * @param regionName Nombre de la región
     * @return true si la región existe
     */
    public static boolean regionExists(World world, String regionName) {
        if (!isWorldGuardAvailable() || world == null) {
            return false;
        }

        try {
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return false;
            }

            return regionManager.hasRegion(regionName);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene todas las regiones definidas en un mundo
     * @param world Mundo
     * @return Lista de nombres de regiones (vacía si no hay WorldGuard)
     */
    public static List<String> getAllRegions(World world) {
        if (!isWorldGuardAvailable() || world == null) {
            return new ArrayList<>();
        }

        try {
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                return new ArrayList<>();
            }

            Set<String> regionNames = regionManager.getRegions().keySet();
            return new ArrayList<>(regionNames);

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Mensaje de información sobre el estado de WorldGuard
     * @return String con información del estado
     */
    public static String getWorldGuardStatus() {
        if (!worldGuardAvailable) {
            return "WorldGuard no está instalado";
        } else if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return "WorldGuard está instalado pero no habilitado";
        } else {
            return "WorldGuard está disponible y funcionando";
        }
    }
}