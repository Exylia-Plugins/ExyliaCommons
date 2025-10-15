package net.exylia.commons.item.config;

import org.bukkit.World;

import java.util.Objects;

public class WorldEntry {

    private final String worldName;

    private WorldEntry(String worldName) {
        this.worldName = worldName;
    }

    public static WorldEntry parse(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            throw new IllegalArgumentException("World entry cannot be null or empty");
        }

        String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("World name cannot be empty: " + entry);
        }

        return new WorldEntry(trimmed);
    }

    public boolean matches(World world) {
        return world != null && this.worldName.equalsIgnoreCase(world.getName());
    }

    public boolean matchesWorldName(String worldName) {
        return this.worldName.equalsIgnoreCase(worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public String toConfigString() {
        return worldName;
    }

    @Override
    public String toString() {
        return toConfigString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        WorldEntry that = (WorldEntry) obj;
        return Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName);
    }

    public static WorldEntry forWorld(String worldName) {
        return new WorldEntry(worldName);
    }
}
