package net.exylia.commons.v2.teleport.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@Getter
@AllArgsConstructor
public final class ExyliaLocation {

    private static final String LOCAL_MARKER = "-";

    private final String server;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public boolean isLocal() {
        return server == null;
    }

    public boolean isCrossServer() {
        return server != null;
    }

    public boolean isSameServer(String currentServer) {
        return server == null || server.equals(currentServer);
    }

    public Location toBukkitLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public static ExyliaLocation of(Location location) {
        return new ExyliaLocation(
                null,
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()
        );
    }

    public static ExyliaLocation of(String server, Location location) {
        return new ExyliaLocation(
                server,
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()
        );
    }

    public static ExyliaLocation fromString(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            throw new IllegalArgumentException("Invalid ExyliaLocation: empty string");
        }
        String[] parts = serialized.split(",");
        try {
            if (parts.length == 6) {
                return new ExyliaLocation(
                        null,
                        parts[0],
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]),
                        Float.parseFloat(parts[4]),
                        Float.parseFloat(parts[5])
                );
            }
            if (parts.length == 7) {
                return new ExyliaLocation(
                        LOCAL_MARKER.equals(parts[0]) ? null : parts[0],
                        parts[1],
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4]),
                        Float.parseFloat(parts[5]),
                        Float.parseFloat(parts[6])
                );
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ExyliaLocation: " + serialized, e);
        }
        throw new IllegalArgumentException("Invalid ExyliaLocation: " + serialized);
    }

    @Override
    public String toString() {
        return (server != null ? server : LOCAL_MARKER)
                + "," + world
                + "," + x
                + "," + y
                + "," + z
                + "," + yaw
                + "," + pitch;
    }
}
