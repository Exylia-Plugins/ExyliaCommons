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

    public static boolean isWorldGuardAvailable() {
        return worldGuardAvailable && Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static List<String> getRegionsAtPlayer(Player player) {
        return getRegionsAtLocation(player.getLocation());
    }

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
             
            return new ArrayList<>();
        }
    }

    public static boolean isPlayerInRegion(Player player, String regionName) {
        return isLocationInRegion(player.getLocation(), regionName);
    }

    public static boolean isLocationInRegion(Location location, String regionName) {
        List<String> regions = getRegionsAtLocation(location);
        return regions.contains(regionName);
    }

    public static boolean isPlayerInAnyRegion(Player player, List<String> regionNames) {
        return isLocationInAnyRegion(player.getLocation(), regionNames);
    }

    public static boolean isLocationInAnyRegion(Location location, List<String> regionNames) {
        if (regionNames.isEmpty()) {
            return false;
        }

        List<String> playerRegions = getRegionsAtLocation(location);
        return playerRegions.stream().anyMatch(regionNames::contains);
    }

    public static String getHighestPriorityRegion(Player player) {
        return getHighestPriorityRegion(player.getLocation());
    }

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

    public static boolean isPvPAllowed(String regionName) {
        if (!isWorldGuardAvailable() || regionName == null) {
            return true;
        }

        try {
            for (World world : Bukkit.getWorlds()) {
                RegionManager regionManager = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer()
                        .get(BukkitAdapter.adapt(world));

                if (regionManager == null) {
                    continue;
                }

                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region != null) {
                    boolean isPvPDenied = isRegionPvPDenied(region);
                    return !isPvPDenied;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[PVP Check] Error checking PVP for region " + regionName + ": " + e.getMessage());
            e.printStackTrace();
            return true;
        }

        return true;
    }

    public static boolean isPvPAllowed(Location location) {
        if (!isWorldGuardAvailable() || location.getWorld() == null) {
            return true;
        }

        String highestPriorityRegion = getHighestPriorityRegion(location);
        if (highestPriorityRegion == null) {
            return true;
        }

        return isPvPAllowed(highestPriorityRegion);
    }

    private static boolean isRegionPvPDenied(ProtectedRegion region) {
        try {
            com.sk89q.worldguard.protection.flags.Flag<?> pvpFlag = WorldGuard.getInstance().getFlagRegistry().get("pvp");

            if (pvpFlag != null) {
                Object flagValue = region.getFlag(pvpFlag);

                if (flagValue != null) {
                    String flagStr = flagValue.toString().toLowerCase();

                    if (flagStr.equals("deny")) {
                        return true;
                    }
                    if (flagStr.equals("allow") || flagStr.equals("true")) {
                        return false;
                    }
                    if (flagValue instanceof Boolean) {
                        boolean denied = !(Boolean) flagValue;
                        return denied;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

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
