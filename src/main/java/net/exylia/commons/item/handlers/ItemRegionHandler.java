package net.exylia.commons.item.handlers;

import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.RegionFilterType;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manejador de lógica de regiones para items
 * ACTUALIZADO: Soporte para el nuevo sistema de regiones con verificación por prioridad y mundos específicos
 */
public class ItemRegionHandler {

    /**
     * Verifica si un jugador puede usar un ítem en su ubicación actual usando el nuevo sistema
     * @param player Jugador
     * @param config Configuración del ítem
     * @return true si puede usarlo en la región actual
     */
    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        if (!config.hasRegionConfiguration() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return true; // Sin configuración de regiones o sin WorldGuard = permitir
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);
        String highestPriorityRegion = WorldGuardUtils.getHighestPriorityRegion(player);

        // Usar el nuevo sistema de verificación con checker
        return config.canUseWithChecker(playerRegions, player.getWorld(), highestPriorityRegion);
    }


    /**
     * Obtiene el cooldown apropiado para la ubicación actual del jugador
     * @param player Jugador
     * @param config Configuración del ítem
     * @return Cooldown en segundos (double)
     */
    public static double getCooldownForPlayerRegion(Player player, ItemConfiguration config) {
        if (!config.hasRegionCooldowns() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return config.getCooldownSeconds(); // Sin configuración de regiones = cooldown por defecto
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);

        if (playerRegions.isEmpty()) {
            return config.getCooldownSeconds(); // Fuera de regiones = cooldown por defecto
        }

        return config.getHighestCooldownForRegions(playerRegions);
    }

    /**
     * Obtiene información detallada sobre las regiones del jugador
     * @param player Jugador
     * @return Lista de nombres de regiones donde está el jugador
     */
    public static List<String> getPlayerRegions(Player player) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return List.of();
        }
        return WorldGuardUtils.getRegionsAtPlayer(player);
    }

    /**
     * Obtiene la región de mayor prioridad del jugador
     * @param player Jugador
     * @return Nombre de la región de mayor prioridad, o null si no está en ninguna región
     */
    public static String getPlayerHighestPriorityRegion(Player player) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return null;
        }
        return WorldGuardUtils.getHighestPriorityRegion(player);
    }

    /**
     * Verifica si el jugador está en alguna región específica
     * @param player Jugador
     * @param regionNames Nombres de regiones a verificar
     * @return true si está en alguna de las regiones especificadas
     */
    public static boolean isPlayerInAnyRegion(Player player, List<String> regionNames) {
        if (!WorldGuardUtils.isWorldGuardAvailable() || regionNames.isEmpty()) {
            return false;
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);
        return playerRegions.stream().anyMatch(regionNames::contains);
    }

    /**
     * Obtiene información detallada sobre la verificación de regiones para debug
     * @param player Jugador
     * @param config Configuración del ítem
     * @return String con información detallada para debug
     */
    public static String getRegionDebugInfo(Player player, ItemConfiguration config) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return "WorldGuard no disponible";
        }

        StringBuilder info = new StringBuilder();

        // Información básica del jugador
        info.append("=== DEBUG REGIONES ===\n");
        info.append("Jugador: ").append(player.getName()).append("\n");
        info.append("Mundo: ").append(player.getWorld().getName()).append("\n");

        // Regiones donde está el jugador
        List<String> playerRegions = getPlayerRegions(player);
        info.append("Regiones del jugador: ").append(playerRegions).append("\n");

        // Región de mayor prioridad
        String highestPriorityRegion = getPlayerHighestPriorityRegion(player);
        info.append("Región de mayor prioridad: ").append(highestPriorityRegion).append("\n");

        // Configuración del item
        if (config.hasRegionConfiguration()) {
            info.append("Tipo de filtro: ").append(config.getRegionType()).append("\n");
            info.append("Tipo de verificador: ").append(config.getRegionChecker()).append("\n");
            info.append("Entradas de región configuradas: ").append(config.getRegionEntries()).append("\n");

            // Resultado de la verificación
            boolean canUse = config.canUseWithChecker(playerRegions, player.getWorld(), highestPriorityRegion);
            info.append("¿Puede usar el item?: ").append(canUse).append("\n");

            // Cooldown específico
            if (config.hasRegionCooldowns()) {
                double cooldown = getCooldownForPlayerRegion(player, config);
                info.append("Cooldown aplicable: ").append(cooldown).append(" segundos\n");
            }
        } else {
            info.append("Sin configuración de regiones\n");
        }

        return info.toString();
    }

    /**
     * Para verificar una configuración específica contra las regiones del jugador
     * @param player Jugador
     * @param regionType Tipo de filtro (WHITELIST/BLACKLIST)
     * @param regionChecker Tipo de verificador (CONTAINS/PRIORITY)
     * @param regionEntries Lista de entradas de región
     * @return true si puede usar según la configuración dada
     */
    public static boolean testRegionConfiguration(Player player,
                                                  net.exylia.commons.item.config.RegionFilterType regionType,
                                                  net.exylia.commons.item.config.RegionCheckerType regionChecker,
                                                  List<net.exylia.commons.item.config.RegionEntry> regionEntries) {

        if (!WorldGuardUtils.isWorldGuardAvailable() || regionEntries.isEmpty()) {
            return true;
        }

        List<String> playerRegions = getPlayerRegions(player);
        String highestPriorityRegion = getPlayerHighestPriorityRegion(player);

        // Crear configuración temporal para la prueba
        var testConfig = net.exylia.commons.item.config.ItemConfiguration.builder()
                .regionType(regionType)
                .regionChecker(regionChecker)
                .regionEntries(regionEntries)
                .build();

        return testConfig.canUseWithChecker(playerRegions, player.getWorld(), highestPriorityRegion);
    }
}