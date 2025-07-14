package net.exylia.commons.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LocationUtils {

    private static final String SEPARATOR = "|";

    public static Location deserialize(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = locationString.split("\\" + SEPARATOR);

            if (parts.length != 6) {
                throw new IllegalArgumentException("El formato debe ser: world|x|y|z|pitch|yaw");
            }

            // Obtener el mundo
            String worldName = parts[0].trim();
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                throw new IllegalArgumentException("El mundo '" + worldName + "' no existe");
            }

            // Parsear coordenadas
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float pitch = Float.parseFloat(parts[4].trim());
            float yaw = Float.parseFloat(parts[5].trim());

            return new Location(world, x, y, z, yaw, pitch);

        } catch (NumberFormatException e) {
            DebugUtils.logInternalError("Error al parsear números en la ubicación: " + locationString);
            return null;
        } catch (Exception e) {
            DebugUtils.logInternalError("Error al parsear la ubicación '" + locationString + "': " + e.getMessage());
            return null;
        }
    }

    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        return String.format("%s%s%.1f%s%.1f%s%.1f%s%.1f%s%.1f",
                location.getWorld().getName(),
                SEPARATOR,
                location.getX(),
                SEPARATOR,
                location.getY(),
                SEPARATOR,
                location.getZ(),
                SEPARATOR,
                location.getPitch(),
                SEPARATOR,
                location.getYaw()
        );
    }

    public static String serialize(Location location, int decimals) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        String format = String.format("%s%s%%.%df%s%%.%df%s%%.%df%s%%.%df%s%%.%df",
                location.getWorld().getName(),
                SEPARATOR, decimals,
                SEPARATOR, decimals,
                SEPARATOR, decimals,
                SEPARATOR, decimals,
                SEPARATOR, decimals
        );

        return String.format(format,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw()
        );
    }

    public static Location findSafeLocation(Location center, int searchRadius) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int y = centerY; y <= centerY + searchRadius && y < world.getMaxHeight() - 2; y++) {
            Location checkLoc = new Location(world, centerX + 0.5, y, centerZ + 0.5);
            if (isSafeLocation(checkLoc)) {
                return checkLoc;
            }
        }

        for (int y = centerY - 1; y >= centerY - searchRadius && y > world.getMinHeight(); y--) {
            Location checkLoc = new Location(world, centerX + 0.5, y, centerZ + 0.5);
            if (isSafeLocation(checkLoc)) {
                return checkLoc;
            }
        }

        return null;
    }

    public static boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        boolean feetSafe = feet.getType().isAir() || !feet.getType().isSolid();
        boolean headSafe = head.getType().isAir() || !head.getType().isSolid();

        boolean groundSafe = ground.getType().isSolid() &&
                ground.getType() != Material.LAVA &&
                !ground.getType().name().contains("PRESSURE_PLATE");

        return feetSafe && headSafe && groundSafe;
    }
}