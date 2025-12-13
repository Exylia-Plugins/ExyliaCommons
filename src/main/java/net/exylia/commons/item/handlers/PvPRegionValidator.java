package net.exylia.commons.item.handlers;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PvPRegionValidator {

    private PvPRegionValidator() {
    }

    public static boolean isPlayerInPvPRegion(Player player) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            DebugUtils.logInternalDebug("PVP Validator - WorldGuard not available, returning true");
            return true;
        }

        String highestPriorityRegion = WorldGuardUtils.getHighestPriorityRegion(player);
        DebugUtils.logInternalDebug("PVP Validator - Player region: " + highestPriorityRegion);

        boolean result = isPvPEnabledInRegion(highestPriorityRegion);
        DebugUtils.logInternalDebug("PVP Validator - Is PVP enabled: " + result);
        return result;
    }

    public static boolean isLocationInPvPRegion(Location location) {
        if (!WorldGuardUtils.isWorldGuardAvailable() || location == null) {
            return true;
        }

        String highestPriorityRegion = WorldGuardUtils.getHighestPriorityRegion(location);
        return isPvPEnabledInRegion(highestPriorityRegion);
    }

    public static boolean isPvPEnabledInRegion(String regionName) {
        if (!WorldGuardUtils.isWorldGuardAvailable() || regionName == null) {
            return true;
        }

        try {
            return WorldGuardUtils.isPvPAllowed(regionName);
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean canPlayerAttackInCurrentRegion(Player player) {
        return isPlayerInPvPRegion(player);
    }

    public static boolean canPlayerAttackAtLocation(Location location) {
        return isLocationInPvPRegion(location);
    }
}
