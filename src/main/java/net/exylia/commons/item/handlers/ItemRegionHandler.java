package net.exylia.commons.item.handlers;

import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.RegionFilterType;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manejador de lógica de regiones para items
 */
public class ItemRegionHandler {

    /**
     * Verifica si un jugador puede usar un ítem en su ubicación actual
     * @param player Jugador
     * @param config Configuración del ítem
     * @return true si puede usarlo en la región actual
     */
    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        if (!config.hasRegionConfiguration() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return true; // Sin configuración de regiones o sin WorldGuard = permitir
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);

        // Si no está en ninguna región, depende del tipo de filtro
        if (playerRegions.isEmpty()) {
            return config.getRegionType() == RegionFilterType.BLACKLIST; // En blacklist se permite fuera de regiones
        }

        return config.canUseInAnyRegion(playerRegions);
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
}
