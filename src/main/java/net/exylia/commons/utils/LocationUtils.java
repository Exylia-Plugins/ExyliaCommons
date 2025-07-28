package net.exylia.commons.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

public class LocationUtils {

    private static final String SEPARATOR = "|";
    private static final Random RANDOM = new Random();
    private static final int MAX_ATTEMPTS = 50;

    public static Location deserialize(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = locationString.split("\\" + SEPARATOR);

            if (parts.length != 6) {
                throw new IllegalArgumentException("El formato debe ser: world|x|y|z|pitch|yaw");
            }

            String worldName = parts[0].trim();
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                throw new IllegalArgumentException("El mundo '" + worldName + "' no existe");
            }

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
        if (world == null) return false;

        Block feet = world.getBlockAt(loc);
        Block head = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block ground = world.getBlockAt(loc.clone().add(0, -1, 0));

        return isAirOrPassable(feet) &&
                isAirOrPassable(head) &&
                ground.getType().isSolid() &&
                !isDangerousBlock(ground);
    }

    public static void preventBoundaryCrossing(Player player, Location from, Location to) {
        if (from == null || to == null || player == null || !player.isOnline()) {
            return;
        }

        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        Location safeLocation = from.clone().subtract(direction.multiply(2.0));

        safeLocation.setYaw(to.getYaw());
        safeLocation.setPitch(to.getPitch());

        safeLocation = findSafeLocation(safeLocation, 10);

        if (safeLocation != null) {
            player.teleport(safeLocation);
        }
    }

    public static boolean preventBoundaryCrossing(Player player, Location from, Location to,
                                                  Location minBounds, Location maxBounds) {
        if (from == null || to == null || player == null || !player.isOnline() ||
                minBounds == null || maxBounds == null) {
            return false;
        }

        if (isOutsideBounds(to, minBounds, maxBounds)) {
            Vector direction = from.toVector().subtract(to.toVector()).normalize();
            Location safeLocation = from.clone().add(direction.multiply(1.0));
            safeLocation = clampToBounds(safeLocation, minBounds, maxBounds);
            safeLocation.setYaw(to.getYaw());
            safeLocation.setPitch(to.getPitch());
            safeLocation = findSafeLocation(safeLocation, 10);

            if (safeLocation != null) {
                player.teleport(safeLocation);
                return true;
            }
        }

        return false;
    }

    public static boolean teleportToSafeHeightUp(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        Location playerLoc = player.getLocation();
        Location safeLocation = findSafeLocationUp(playerLoc);

        if (safeLocation != null) {
            player.teleport(safeLocation);
            return true;
        }

        return false;
    }

    public static Location findSafeLocationUp(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int maxHeight = world.getMaxHeight() - 2;

        for (int y = location.getBlockY(); y <= maxHeight; y++) {
            Location checkLocation = new Location(world, x + 0.5, y, z + 0.5);

            if (hasTwoBlocksOfAir(checkLocation)) {
                checkLocation.setYaw(location.getYaw());
                checkLocation.setPitch(location.getPitch());
                return checkLocation;
            }
        }

        return null;
    }

    public static void randomTeleport(Location centerLocation, Collection<Player> players, int radius) {
        if (centerLocation == null || centerLocation.getWorld() == null || players == null || players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            Location safeLoc = findSafeLocationInRadius(centerLocation, radius);
            if (safeLoc != null) {
                safeLoc.setPitch(player.getLocation().getPitch());
                safeLoc.setYaw(player.getLocation().getYaw());
                player.teleport(safeLoc);
            }
        }
    }

    public static Location getRandomSafeLocation(Location centerLocation, int radius) {
        if (centerLocation == null || centerLocation.getWorld() == null) {
            return null;
        }

        return findSafeLocationInRadius(centerLocation, radius);
    }

    private static boolean hasTwoBlocksOfAir(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);

        return feet.getType().isAir() && head.getType().isAir();
    }

    private static boolean isOutsideBounds(Location location, Location min, Location max) {
        return location.getBlockX() < min.getBlockX() || location.getBlockX() > max.getBlockX() ||
                location.getBlockY() < min.getBlockY() || location.getBlockY() > max.getBlockY() ||
                location.getBlockZ() < min.getBlockZ() || location.getBlockZ() > max.getBlockZ();
    }

    private static Location clampToBounds(Location location, Location min, Location max) {
        Location clamped = location.clone();

        if (clamped.getBlockX() < min.getBlockX()) {
            clamped.setX(min.getBlockX() + 0.5);
        } else if (clamped.getBlockX() > max.getBlockX()) {
            clamped.setX(max.getBlockX() - 0.5);
        }

        if (clamped.getBlockY() < min.getBlockY()) {
            clamped.setY(min.getBlockY() + 1);
        } else if (clamped.getBlockY() > max.getBlockY()) {
            clamped.setY(max.getBlockY() - 1);
        }

        if (clamped.getBlockZ() < min.getBlockZ()) {
            clamped.setZ(min.getBlockZ() + 0.5);
        } else if (clamped.getBlockZ() > max.getBlockZ()) {
            clamped.setZ(max.getBlockZ() - 0.5);
        }

        return clamped;
    }

    private static Location findSafeLocationInRadius(Location centerLocation, int radius) {
        World world = centerLocation.getWorld();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double randomX = centerLocation.getX() + (RANDOM.nextDouble() * 2 - 1) * radius;
            double randomZ = centerLocation.getZ() + (RANDOM.nextDouble() * 2 - 1) * radius;

            Location safeLoc = findSafeLocationAtCoordinates(world, randomX, randomZ, centerLocation.getY());
            if (safeLoc != null) {
                return safeLoc;
            }
        }

        return findSafeLocationAtCoordinates(world, centerLocation.getX(), centerLocation.getZ(), centerLocation.getY());
    }

    private static Location findSafeLocationAtCoordinates(World world, double x, double z, double startY) {
        int blockY = (int) Math.floor(startY);

        for (int y = blockY; y > Math.max(world.getMinHeight(), blockY - 50); y--) {
            Location testLoc = new Location(world, x, y + 1.0, z);
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }

        for (int y = blockY + 1; y < Math.min(world.getMaxHeight() - 2, blockY + 50); y++) {
            Location testLoc = new Location(world, x, y + 1.0, z);
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }

        return null;
    }

    private static boolean isAirOrPassable(Block block) {
        Material type = block.getType();
        return type == Material.AIR ||
                type == Material.CAVE_AIR ||
                type == Material.VOID_AIR ||
                !type.isSolid();
    }

    private static boolean isDangerousBlock(Block block) {
        Material type = block.getType();
        return type == Material.LAVA ||
                type == Material.FIRE ||
                type == Material.SOUL_FIRE ||
                type == Material.MAGMA_BLOCK ||
                type == Material.CACTUS ||
                type == Material.SWEET_BERRY_BUSH ||
                type.name().contains("PRESSURE_PLATE");
    }
}