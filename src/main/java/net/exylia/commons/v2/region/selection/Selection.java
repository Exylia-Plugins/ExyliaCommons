package net.exylia.commons.v2.region.selection;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.UUID;

@Getter
public class Selection {
    private final UUID playerId;
    private final String selectionId;
    private final long createdAt;

    @Setter
    private Location pos1;
    @Setter
    private Location pos2;

    public Selection(UUID playerId, String selectionId) {
        this.playerId = playerId;
        this.selectionId = selectionId;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public static Selection of(Location pos1, Location pos2) {
        Selection selection = new Selection(null, "region_" + System.currentTimeMillis());
        selection.setPos1(pos1);
        selection.setPos2(pos2);
        return selection;
    }

    public Location getCenter() {
        if (!isComplete()) return null;
        Location min = getMinimumPoint();
        Location max = getMaximumPoint();
        return new Location(
            pos1.getWorld(),
            (min.getX() + max.getX() + 1) / 2.0,
            (min.getY() + max.getY() + 1) / 2.0,
            (min.getZ() + max.getZ() + 1) / 2.0
        );
    }

    public boolean contains(Location location) {
        if (!isComplete() || location.getWorld() == null) return false;
        if (!location.getWorld().equals(pos1.getWorld())) return false;
        Location min = getMinimumPoint();
        Location max = getMaximumPoint();
        double x = location.getX(), y = location.getY(), z = location.getZ();
        return x >= min.getBlockX() && x <= max.getBlockX() + 1
            && y >= min.getBlockY() && y <= max.getBlockY() + 1
            && z >= min.getBlockZ() && z <= max.getBlockZ() + 1;
    }

    public Location getMinimumPoint() {
        if (!isComplete()) return null;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        return new Location(pos1.getWorld(), minX, minY, minZ);
    }

    public Location getMaximumPoint() {
        if (!isComplete()) return null;

        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return new Location(pos1.getWorld(), maxX, maxY, maxZ);
    }

    public long getVolume() {
        if (!isComplete()) return 0;

        int deltaX = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int deltaY = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        int deltaZ = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;

        return (long) deltaX * deltaY * deltaZ;
    }

    public void clear() {
        this.pos1 = null;
        this.pos2 = null;
    }

    @Override
    public String toString() {
        return String.format("SelectionV2{id='%s', complete=%b, volume=%d}",
                selectionId, isComplete(), getVolume());
    }
}
