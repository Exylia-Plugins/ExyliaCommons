package net.exylia.commons.region.flags;

import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.region.model.RegionFlagType;
import net.exylia.commons.utils.DebugUtils;

import java.util.List;

@Deprecated
public class FlagRepair {

    public static void repairBuildingFlags() {
        try {
            RegionManager regionManager = RegionManager.getInstance();
            List<Region> allRegions = new java.util.ArrayList<>(regionManager.getAllRegions());

            if (allRegions.isEmpty()) {
                DebugUtils.logInternalDebug("No regions to repair");
                return;
            }

            int repaired = 0;
            for (Region region : allRegions) {
                if (repairRegionBuildingFlags(region)) {
                    repaired++;
                }
            }

            DebugUtils.logInternalInfo("Flag repair complete: " + repaired + " regions fixed");

        } catch (Exception e) {
            DebugUtils.logInternalError("Error repairing flags: " + e.getMessage());
        }
    }

    private static boolean repairRegionBuildingFlags(Region region) {
        boolean changed = false;

        RegionFlagType buildType = region.getFlagType(RegionFlag.BUILD);
        RegionFlagType breakType = region.getFlagType(RegionFlag.BREAK);
        RegionFlagType interactType = region.getFlagType(RegionFlag.INTERACT);

        if (buildType == RegionFlagType.DENY || !region.getFlagValue(RegionFlag.BUILD)) {
            region.setFlag(RegionFlag.BUILD, RegionFlagType.DEFAULT);
            DebugUtils.logInternalDebug("Repaired BUILD flag for region: " + region.getId());
            changed = true;
        }

        if (breakType == RegionFlagType.DENY || !region.getFlagValue(RegionFlag.BREAK)) {
            region.setFlag(RegionFlag.BREAK, RegionFlagType.DEFAULT);
            DebugUtils.logInternalDebug("Repaired BREAK flag for region: " + region.getId());
            changed = true;
        }

        if (interactType == RegionFlagType.DENY || !region.getFlagValue(RegionFlag.INTERACT)) {
            region.setFlag(RegionFlag.INTERACT, RegionFlagType.DEFAULT);
            DebugUtils.logInternalDebug("Repaired INTERACT flag for region: " + region.getId());
            changed = true;
        }

        return changed;
    }

    public static boolean enableBuildingFlagsForRegion(Region region) {
        try {
            region.setFlag(RegionFlag.BUILD, RegionFlagType.ALLOW);
            region.setFlag(RegionFlag.BREAK, RegionFlagType.ALLOW);
            region.setFlag(RegionFlag.INTERACT, RegionFlagType.ALLOW);

            DebugUtils.logInternalDebug("Enabled building flags for region: " + region.getId());
            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error enabling flags for region: " + e.getMessage());
            return false;
        }
    }
}
