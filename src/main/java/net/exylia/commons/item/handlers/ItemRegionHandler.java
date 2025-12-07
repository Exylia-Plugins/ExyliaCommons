package net.exylia.commons.item.handlers;

import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.RegionFilterType;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class ItemRegionHandler {

    public static boolean canPlayerUseItemInCurrentRegion(Player player, ItemConfiguration config) {
        DebugUtils.logInternalDebug("Item PVP Check - disable-in-non-pvp-regions: " + config.isDisableInNonPvpRegions());

        if (config.isDisableInNonPvpRegions()) {
            boolean isPvP = PvPRegionValidator.isPlayerInPvPRegion(player);
            DebugUtils.logInternalDebug("Item PVP Check - Player in PvP region: " + isPvP);

            if (!isPvP) {
                DebugUtils.logInternalDebug("Item PVP Check - DENIED: Player is in non-PvP region");
                return false;
            }
        }

        if (!config.hasRegionConfiguration() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return true;
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);
        String highestPriorityRegion = WorldGuardUtils.getHighestPriorityRegion(player);

        return config.canUseWithChecker(playerRegions, player.getWorld(), highestPriorityRegion);
    }

    public static boolean canPlayerUseItemInLocation(org.bukkit.Location location, ItemConfiguration config) {
        if (config.isDisableInNonPvpRegions() && !PvPRegionValidator.isLocationInPvPRegion(location)) {
            return false;
        }

        if (!config.hasRegionConfiguration() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return true;
        }

        List<String> locationRegions = WorldGuardUtils.getRegionsAtLocation(location);
        String highestPriorityRegion = WorldGuardUtils.getHighestPriorityRegion(location);

        return config.canUseWithChecker(locationRegions, location.getWorld(), highestPriorityRegion);
    }

    public static double getCooldownForPlayerRegion(Player player, ItemConfiguration config) {
        double baseCooldown = config.getCooldownSeconds();
        double finalCooldown = baseCooldown;
        
        boolean hasSpecificWorldConfig = config.hasWorldCooldowns() && config.getWorldCooldowns().containsKey(player.getWorld().getName());
        if (hasSpecificWorldConfig) {
            finalCooldown = config.getCooldownForWorld(player.getWorld());
        }

        if (!config.hasRegionCooldowns() || !WorldGuardUtils.isWorldGuardAvailable()) {
            return finalCooldown;
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);

        if (playerRegions.isEmpty()) {
             
            if (config.getRegionCooldowns().containsKey("__global__")) {
                finalCooldown = config.getRegionCooldowns().get("__global__");
            }
            return finalCooldown;
        }

        boolean hasSpecificRegionConfig = playerRegions.stream().anyMatch(region -> config.getRegionCooldowns().containsKey(region));
        if (hasSpecificRegionConfig) {
            finalCooldown = config.getHighestCooldownForRegions(playerRegions);
        }
        
        return finalCooldown;
    }

    public static List<String> getPlayerRegions(Player player) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return List.of();
        }
        return WorldGuardUtils.getRegionsAtPlayer(player);
    }

    public static String getPlayerHighestPriorityRegion(Player player) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return null;
        }
        return WorldGuardUtils.getHighestPriorityRegion(player);
    }

    public static boolean isPlayerInAnyRegion(Player player, List<String> regionNames) {
        if (!WorldGuardUtils.isWorldGuardAvailable() || regionNames.isEmpty()) {
            return false;
        }

        List<String> playerRegions = WorldGuardUtils.getRegionsAtPlayer(player);
        return playerRegions.stream().anyMatch(regionNames::contains);
    }

    public static boolean canPlayerUseItemInCurrentWorld(Player player, ItemConfiguration config) {
        return config.canUseInWorld(player.getWorld());
    }

    public static double getCooldownForPlayerWorld(Player player, ItemConfiguration config) {
        return config.getCooldownForWorld(player.getWorld());
    }

    public static boolean isPlayerInAnyWorld(Player player, List<String> worldNames) {
        if (worldNames.isEmpty()) {
            return false;
        }

        String currentWorld = player.getWorld().getName();
        return worldNames.stream().anyMatch(worldName -> worldName.equalsIgnoreCase(currentWorld));
    }

    public static String getRegionDebugInfo(Player player, ItemConfiguration config) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return "WorldGuard no disponible";
        }

        StringBuilder info = new StringBuilder();

        info.append("=== DEBUG REGIONES Y MUNDOS ===\n");
        info.append("Jugador: ").append(player.getName()).append("\n");
        info.append("Mundo: ").append(player.getWorld().getName()).append("\n");

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

        List<String> playerRegions = getPlayerRegions(player);
        info.append("Regiones del jugador: ").append(playerRegions).append("\n");

        String highestPriorityRegion = getPlayerHighestPriorityRegion(player);
        info.append("Región de mayor prioridad: ").append(highestPriorityRegion).append("\n");

        if (config.hasRegionConfiguration()) {
            info.append("Tipo de filtro: ").append(config.getRegionType()).append("\n");
            info.append("Tipo de verificador: ").append(config.getRegionChecker()).append("\n");
            info.append("Entradas de región configuradas: ").append(config.getRegionEntries()).append("\n");

            boolean canUse = config.canUseWithChecker(playerRegions, player.getWorld(), highestPriorityRegion);
            info.append("¿Puede usar el item?: ").append(canUse).append("\n");

            if (config.hasRegionCooldowns() || config.hasWorldCooldowns()) {
                double cooldown = getCooldownForPlayerRegion(player, config);
                info.append("Cooldown final aplicable: ").append(cooldown).append(" segundos\n");
            }
        } else {
            info.append("Sin configuración de regiones\n");
        }

        return info.toString();
    }

    public static boolean testRegionConfiguration(Player player,
                                                  net.exylia.commons.item.config.RegionFilterType regionType,
                                                  net.exylia.commons.item.config.RegionCheckerType regionChecker,
                                                  List<net.exylia.commons.item.config.RegionEntry> regionEntries) {

        if (!WorldGuardUtils.isWorldGuardAvailable() || regionEntries.isEmpty()) {
            return true;
        }

        List<String> playerRegions = getPlayerRegions(player);
        String highestPriorityRegion = getPlayerHighestPriorityRegion(player);

        var testConfig = net.exylia.commons.item.config.ItemConfiguration.builder()
                .regionType(regionType)
                .regionChecker(regionChecker)
                .regionEntries(regionEntries)
                .build();

        return testConfig.canUseWithChecker(playerRegions, player.getWorld(), highestPriorityRegion);
    }
}
