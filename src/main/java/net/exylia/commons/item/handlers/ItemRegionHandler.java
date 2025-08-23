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
        double baseCooldown = config.getCooldownSeconds();
        double finalCooldown = baseCooldown;
        
        // Verificar cooldown específico de mundo
        boolean hasSpecificWorldConfig = config.hasWorldCooldowns() && config.getWorldCooldowns().containsKey(player.getWorld().getName());
        if (hasSpecificWorldConfig) {
            finalCooldown = config.getCooldownForWorld(player.getWorld());
        }

        // Si no hay WorldGuard o no hay configuración de regiones, usar mundo o base
        if (!config.hasRegionCooldowns() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return finalCooldown;
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);

        if (playerRegions.isEmpty()) {
            // Fuera de regiones - verificar si hay configuración __global__
            if (config.getRegionCooldowns().containsKey("__global__")) {
                finalCooldown = config.getRegionCooldowns().get("__global__");
            }
            return finalCooldown;
        }

        // Verificar si tiene configuración específica de región (OVERRIDE mundo y base)
        boolean hasSpecificRegionConfig = playerRegions.stream().anyMatch(region -> config.getRegionCooldowns().containsKey(region));
        if (hasSpecificRegionConfig) {
            finalCooldown = config.getHighestCooldownForRegions(playerRegions);
        }
        
        return finalCooldown;
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
     * Verifica si el jugador puede usar el item en su mundo actual
     * @param player Jugador
     * @param config Configuración del ítem
     * @return true si puede usarlo en el mundo actual
     */
    public static boolean canPlayerUseItemInCurrentWorld(Player player, ItemConfiguration config) {
        return config.canUseInWorld(player.getWorld());
    }

    /**
     * Obtiene el cooldown específico para el mundo del jugador
     * @param player Jugador
     * @param config Configuración del ítem
     * @return Cooldown en segundos para el mundo actual
     */
    public static double getCooldownForPlayerWorld(Player player, ItemConfiguration config) {
        return config.getCooldownForWorld(player.getWorld());
    }

    /**
     * Verifica si el jugador está en algún mundo específico
     * @param player Jugador
     * @param worldNames Nombres de mundos a verificar
     * @return true si está en alguno de los mundos especificados
     */
    public static boolean isPlayerInAnyWorld(Player player, List<String> worldNames) {
        if (worldNames.isEmpty()) {
            return false;
        }

        String currentWorld = player.getWorld().getName();
        return worldNames.stream().anyMatch(worldName -> worldName.equalsIgnoreCase(currentWorld));
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
        info.append("=== DEBUG REGIONES Y MUNDOS ===\n");
        info.append("Jugador: ").append(player.getName()).append("\n");
        info.append("Mundo: ").append(player.getWorld().getName()).append("\n");

        // Configuración de mundos
        if (config.hasWorldConfiguration()) {
            info.append("Tipo de filtro de mundo: ").append(config.getWorldType()).append("\n");
            info.append("Mundos configurados: ").append(config.getWorldNames()).append("\n");

            boolean canUseInWorld = config.canUseInWorld(player.getWorld());
            info.append("¿Puede usar el item en este mundo?: ").append(canUseInWorld).append("\n");

            if (config.hasWorldCooldowns()) {
                double worldCooldown = config.getCooldownForWorld(player.getWorld());
                info.append("Cooldown específico de mundo: ").append(worldCooldown).append(" segundos\n");
            }
        } else {
            info.append("Sin configuración de mundos\n");
        }

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

            // Cooldown específico (considera tanto mundo como región)
            if (config.hasRegionCooldowns() || config.hasWorldCooldowns()) {
                double cooldown = getCooldownForPlayerRegion(player, config);
                info.append("Cooldown final aplicable: ").append(cooldown).append(" segundos\n");
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