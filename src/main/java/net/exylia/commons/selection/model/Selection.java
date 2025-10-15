package net.exylia.commons.selection.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class Selection {
    private UUID playerId;
    private String selectionId;
    @Setter
    private Location pos1;
    @Setter
    private Location pos2;
    private long createdAt;
    @Setter
    private SelectionType type;

    public Selection(UUID playerId, String selectionId, SelectionType type) {
        this.playerId = playerId;
        this.selectionId = selectionId;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public Selection(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld());
    }

    public long getVolume() {
        if (!isComplete()) return 0;

        int deltaX = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int deltaY = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        int deltaZ = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;

        return (long) deltaX * deltaY * deltaZ;
    }

    public Location getMinimumPoint() {
        if (!isComplete()) return null;

        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        return new Location(world, minX, minY, minZ);
    }

    public Location getMaximumPoint() {
        if (!isComplete()) return null;

        World world = pos1.getWorld();
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return new Location(world, maxX, maxY, maxZ);
    }

    public Location getCenter() {
        if (!isComplete()) return null;

        World world = pos1.getWorld();

        double centerX = (pos1.getBlockX() + pos2.getBlockX()) / 2.0;
        double centerY = (pos1.getBlockY() + pos2.getBlockY()) / 2.0;
        double centerZ = (pos1.getBlockZ() + pos2.getBlockZ()) / 2.0;

        return new Location(world, centerX, centerY, centerZ);
    }

    public boolean contains(Location location) {
        if (!isComplete() || !location.getWorld().equals(pos1.getWorld())) {
            return false;
        }

        Location min = getMinimumPoint();
        Location max = getMaximumPoint();

        return location.getBlockX() >= min.getBlockX() && location.getBlockX() <= max.getBlockX() &&
                location.getBlockY() >= min.getBlockY() && location.getBlockY() <= max.getBlockY() &&
                location.getBlockZ() >= min.getBlockZ() && location.getBlockZ() <= max.getBlockZ();
    }

}
