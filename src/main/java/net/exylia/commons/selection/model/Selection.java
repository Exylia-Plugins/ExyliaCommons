package net.exylia.commons.selection.model;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

@Getter
public class Selection {
    private UUID playerId;
    private String selectionId;
    private Location pos1;
    private Location pos2;
    private long createdAt;
    private SelectionType type;

    private int minX, minY, minZ, maxX, maxY, maxZ;
    private boolean boundsComputed;

    public Selection(UUID playerId, String selectionId, SelectionType type) {
        this.playerId = playerId;
        this.selectionId = selectionId;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public Selection(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        recomputeBounds();
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
        recomputeBounds();
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
        recomputeBounds();
    }

    public void setType(SelectionType type) {
        this.type = type;
    }

    private void recomputeBounds() {
        if (pos1 != null && pos2 != null && pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld())) {
            this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            this.boundsComputed = true;
        } else {
            this.boundsComputed = false;
        }
    }

    public boolean isComplete() {
        return boundsComputed;
    }

    public long getVolume() {
        if (!boundsComputed) return 0;
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public Location getMinimumPoint() {
        if (!boundsComputed) return null;
        return new Location(pos1.getWorld(), minX, minY, minZ);
    }

    public Location getMaximumPoint() {
        if (!boundsComputed) return null;
        return new Location(pos1.getWorld(), maxX, maxY, maxZ);
    }

    public Location getCenter() {
        if (!boundsComputed) return null;
        return new Location(pos1.getWorld(), (minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
    }

    public boolean contains(Location location) {
        if (!boundsComputed || !location.getWorld().equals(pos1.getWorld())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
