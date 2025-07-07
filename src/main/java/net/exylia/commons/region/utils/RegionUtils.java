package net.exylia.commons.region.utils;

import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilidades para trabajar con regiones
 */
public class RegionUtils {

    /**
     * Verifica si un jugador puede realizar una acción en su ubicación actual
     */
    public static boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        List<Region> regions = RegionManager.getInstance().getRegionsAt(player.getLocation());

        if (regions.isEmpty()) {
            // No hay regiones, usar valor por defecto de la flag
            return flag.isDefaultValue();
        }

        // Obtener la región de mayor prioridad
        Region highestPriorityRegion = regions.get(0); // Ya están ordenadas por prioridad
        return highestPriorityRegion.hasFlag(flag);
    }

    /**
     * Verifica si un jugador puede realizar una acción en una ubicación específica
     */
    public static boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        List<Region> regions = RegionManager.getInstance().getRegionsAt(location);

        if (regions.isEmpty()) {
            return flag.isDefaultValue();
        }

        Region highestPriorityRegion = regions.get(0);
        return highestPriorityRegion.hasFlag(flag);
    }

    /**
     * Obtiene todas las regiones que se superponen con otra región
     */
    public static List<Region> getOverlappingRegions(Region targetRegion) {
        return RegionManager.getInstance().getAllRegions().stream()
                .filter(region -> !region.equals(targetRegion))
                .filter(region -> regionsOverlap(targetRegion, region))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si dos regiones se superponen
     */
    public static boolean regionsOverlap(Region region1, Region region2) {
        if (!region1.getWorld().equals(region2.getWorld())) {
            return false;
        }

        Location min1 = region1.getMinimumPoint();
        Location max1 = region1.getMaximumPoint();
        Location min2 = region2.getMinimumPoint();
        Location max2 = region2.getMaximumPoint();

        return min1.getBlockX() <= max2.getBlockX() && max1.getBlockX() >= min2.getBlockX() &&
                min1.getBlockY() <= max2.getBlockY() && max1.getBlockY() >= min2.getBlockY() &&
                min1.getBlockZ() <= max2.getBlockZ() && max1.getBlockZ() >= min2.getBlockZ();
    }

    /**
     * Calcula el volumen de intersección entre dos regiones
     */
    public static long getIntersectionVolume(Region region1, Region region2) {
        if (!regionsOverlap(region1, region2)) {
            return 0;
        }

        Location min1 = region1.getMinimumPoint();
        Location max1 = region1.getMaximumPoint();
        Location min2 = region2.getMinimumPoint();
        Location max2 = region2.getMaximumPoint();

        int minX = Math.max(min1.getBlockX(), min2.getBlockX());
        int maxX = Math.min(max1.getBlockX(), max2.getBlockX());
        int minY = Math.max(min1.getBlockY(), min2.getBlockY());
        int maxY = Math.min(max1.getBlockY(), max2.getBlockY());
        int minZ = Math.max(min1.getBlockZ(), min2.getBlockZ());
        int maxZ = Math.min(max1.getBlockZ(), max2.getBlockZ());

        return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /**
     * Encuentra la región más cercana a una ubicación
     */
    public static Optional<Region> getNearestRegion(Location location, String pluginName) {
        Collection<Region> regions = RegionManager.getInstance().getRegions(pluginName);

        return regions.stream()
                .min((r1, r2) -> {
                    double dist1 = getDistanceToRegion(location, r1);
                    double dist2 = getDistanceToRegion(location, r2);
                    return Double.compare(dist1, dist2);
                });
    }

    /**
     * Calcula la distancia desde una ubicación al borde más cercano de una región
     */
    public static double getDistanceToRegion(Location location, Region region) {
        if (region.contains(location)) {
            return 0.0; // Ya está dentro
        }

        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        double dx = Math.max(0, Math.max(min.getX() - location.getX(), location.getX() - max.getX()));
        double dy = Math.max(0, Math.max(min.getY() - location.getY(), location.getY() - max.getY()));
        double dz = Math.max(0, Math.max(min.getZ() - location.getZ(), location.getZ() - max.getZ()));

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Obtiene el centro de una región
     */
    public static Location getRegionCenter(Region region) {
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        double centerX = (min.getX() + max.getX()) / 2.0;
        double centerY = (min.getY() + max.getY()) / 2.0;
        double centerZ = (min.getZ() + max.getZ()) / 2.0;

        return new Location(region.getWorld(), centerX, centerY, centerZ);
    }

    /**
     * Expande una región en todas las direcciones
     */
    public static Region expandRegion(Region region, int amount) {
        Selection selection = region.getSelection();
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        Location newMin = min.clone().add(-amount, -amount, -amount);
        Location newMax = max.clone().add(amount, amount, amount);

        selection.setPos1(newMin);
        selection.setPos2(newMax);

        return region;
    }

    /**
     * Contrae una región en todas las direcciones
     */
    public static Region contractRegion(Region region, int amount) {
        Selection selection = region.getSelection();
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        Location newMin = min.clone().add(amount, amount, amount);
        Location newMax = max.clone().add(-amount, -amount, -amount);

        // Verificar que la región sigue siendo válida
        if (newMin.getBlockX() > newMax.getBlockX() ||
                newMin.getBlockY() > newMax.getBlockY() ||
                newMin.getBlockZ() > newMax.getBlockZ()) {
            return region; // No aplicar cambios si se vuelve inválida
        }

        selection.setPos1(newMin);
        selection.setPos2(newMax);

        return region;
    }

    /**
     * Crea una región temporal para pruebas
     */
    public static Region createTemporaryRegion(String id, Location pos1, Location pos2) {
        Selection tempSelection = new Selection(UUID.randomUUID(), "temp-" + id,
                net.exylia.commons.selection.model.SelectionType.CUBOID);
        tempSelection.setPos1(pos1);
        tempSelection.setPos2(pos2);

        return new Region(id, "temporary", tempSelection);
    }

    /**
     * Valida que una región no cause problemas
     */
    public static boolean validateRegion(Region region) {
        if (!region.isValid()) {
            return false;
        }

        // Verificar volumen razonable
        long volume = region.getVolume();
        if (volume <= 0 || volume > 100_000_000) { // Máximo 100M bloques
            return false;
        }

        // Verificar que esté en un mundo válido
        return region.getWorld() != null;
    }

    /**
     * Obtiene información detallada de una región
     */
    public static String getDetailedRegionInfo(Region region) {
        StringBuilder info = new StringBuilder();
        info.append("=== Información de Región ===\n");
        info.append("ID: ").append(region.getId()).append("\n");
        info.append("Plugin: ").append(region.getPluginName()).append("\n");
        info.append("Nombre: ").append(region.getDisplayName()).append("\n");
        info.append("Descripción: ").append(region.getDescription()).append("\n");
        info.append("Mundo: ").append(region.getWorld().getName()).append("\n");
        info.append("Prioridad: ").append(region.getPriority().getDisplayName()).append("\n");
        info.append("Volumen: ").append(region.getVolume()).append(" bloques\n");
        info.append("Jugadores dentro: ").append(region.getPlayersInside().size()).append("\n");

        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();
        info.append("Coordenadas: (").append(min.getBlockX()).append(",")
                .append(min.getBlockY()).append(",").append(min.getBlockZ())
                .append(") a (").append(max.getBlockX()).append(",")
                .append(max.getBlockY()).append(",").append(max.getBlockZ()).append(")\n");

        info.append("Flags activas: ");
        if (region.getFlags().isEmpty()) {
            info.append("Ninguna");
        } else {
            info.append(region.getFlags().stream()
                    .map(RegionFlag::getKey)
                    .collect(Collectors.joining(", ")));
        }
        info.append("\n");

        if (!region.getMetadata().isEmpty()) {
            info.append("Metadata: ").append(region.getMetadata().size()).append(" elementos\n");
        }

        return info.toString();
    }

    /**
     * Busca regiones por nombre o ID usando patrones
     */
    public static List<Region> searchRegions(String pattern, String pluginName) {
        Collection<Region> regions = pluginName != null ?
                RegionManager.getInstance().getRegions(pluginName) :
                RegionManager.getInstance().getAllRegions();

        String lowerPattern = pattern.toLowerCase();

        return regions.stream()
                .filter(region ->
                        region.getId().toLowerCase().contains(lowerPattern) ||
                                region.getDisplayName().toLowerCase().contains(lowerPattern) ||
                                region.getDescription().toLowerCase().contains(lowerPattern))
                .sorted((r1, r2) -> {
                    // Priorizar coincidencias exactas en ID
                    if (r1.getId().toLowerCase().equals(lowerPattern)) return -1;
                    if (r2.getId().toLowerCase().equals(lowerPattern)) return 1;

                    // Luego por prioridad de región
                    return r2.getPriority().getLevel() - r1.getPriority().getLevel();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de uso de flags
     */
    public static Map<RegionFlag, Integer> getFlagStatistics(String pluginName) {
        Collection<Region> regions = pluginName != null ?
                RegionManager.getInstance().getRegions(pluginName) :
                RegionManager.getInstance().getAllRegions();

        Map<RegionFlag, Integer> stats = new EnumMap<>(RegionFlag.class);

        for (Region region : regions) {
            for (RegionFlag flag : region.getFlags()) {
                stats.merge(flag, 1, Integer::sum);
            }
        }

        return stats;
    }

    /**
     * Verifica si un jugador tiene permisos para modificar una región
     */
    public static boolean canPlayerModifyRegion(Player player, Region region) {
        // Verificar permiso general
        if (player.hasPermission("exylia.region.admin")) {
            return true;
        }

        // Verificar permiso específico del plugin
        if (player.hasPermission(region.getPluginName() + ".region.admin")) {
            return true;
        }

        // Verificar permiso específico de la región
        return player.hasPermission(region.getPluginName() + ".region." + region.getId());
    }
}