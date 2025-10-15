package net.exylia.commons.item.config;

import org.bukkit.World;

import java.util.Objects;

public class RegionEntry {

    private final String regionName;
    private final String worldName;
    private final boolean worldSpecific;

    private RegionEntry(String regionName, String worldName, boolean worldSpecific) {
        this.regionName = regionName;
        this.worldName = worldName;
        this.worldSpecific = worldSpecific;
    }

    public static RegionEntry parse(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            throw new IllegalArgumentException("Region entry cannot be null or empty");
        }

        String trimmed = entry.trim();

        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|", 2);
            String regionName = parts[0].trim();
            String worldName = parts[1].trim();

            if (regionName.isEmpty()) {
                throw new IllegalArgumentException("Region name cannot be empty: " + entry);
            }
            if (worldName.isEmpty()) {
                throw new IllegalArgumentException("World name cannot be empty: " + entry);
            }

            return new RegionEntry(regionName, worldName, true);
        } else {
            return new RegionEntry(trimmed, null, false);
        }
    }

    public boolean matches(String regionName, World world) {
        if (!this.regionName.equalsIgnoreCase(regionName)) {
            return false;
        }

        if (!worldSpecific) {
            return true;
        }

        return world != null && this.worldName.equalsIgnoreCase(world.getName());
    }

    public boolean matchesRegionName(String regionName) {
        return this.regionName.equalsIgnoreCase(regionName);
    }

    public String getRegionName() {
        return regionName;
    }

    public String getWorldName() {
        return worldName;
    }

    public boolean isWorldSpecific() {
        return worldSpecific;
    }

    public String toConfigString() {
        if (worldSpecific) {
            return regionName + "|" + worldName;
        } else {
            return regionName;
        }
    }

    @Override
    public String toString() {
        return toConfigString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RegionEntry that = (RegionEntry) obj;
        return worldSpecific == that.worldSpecific &&
                Objects.equals(regionName, that.regionName) &&
                Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regionName, worldName, worldSpecific);
    }

    public static RegionEntry forAnyWorld(String regionName) {
        return new RegionEntry(regionName, null, false);
    }

    public static RegionEntry forWorld(String regionName, String worldName) {
        return new RegionEntry(regionName, worldName, true);
    }
}
