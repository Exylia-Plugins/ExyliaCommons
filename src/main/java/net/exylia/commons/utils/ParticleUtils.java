package net.exylia.commons.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ParticleUtils {

    /**
     * Spawns a particle effect from a formatted string.
     * Format: PARTICLE_NAME|COUNT|OFFSET_X|OFFSET_Y|OFFSET_Z|EXTRA|DATA
     * Example: FLAME|10|0.5|0.5|0.5|0.1
     * Example with color: REDSTONE|1|0|0|0|0|255,0,0
     *
     * @param location The location to spawn particles at
     * @param particleString The formatted particle string
     * @return true if the particles were spawned, false otherwise
     */
    public static boolean spawnParticles(Location location, String particleString) {
        if (particleString == null || particleString.isEmpty() || location == null) return false;

        String[] parts = particleString.split("\\|");
        if (parts.length < 1) return false;

        String particleName = parts[0];
        int count = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        double offsetX = parts.length > 2 ? parseDouble(parts[2], 0.0) : 0.0;
        double offsetY = parts.length > 3 ? parseDouble(parts[3], 0.0) : 0.0;
        double offsetZ = parts.length > 4 ? parseDouble(parts[4], 0.0) : 0.0;
        double extra = parts.length > 5 ? parseDouble(parts[5], 0.0) : 0.0;

        try {
            Particle particle = Particle.valueOf(particleName);

            // Handle special particles that require data
            if (parts.length > 6 && (particle == Particle.REDSTONE || particle == Particle.SPELL_MOB)) {
                Color color = parseColor(parts[6]);
                if (color != null) {
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
                    location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, dustOptions);
                } else {
                    location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
                }
            } else {
                location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Spawns particles for a specific player from a formatted string.
     *
     * @param player The player to show particles to
     * @param location The location to spawn particles at
     * @param particleString The formatted particle string
     * @return true if the particles were spawned, false otherwise
     */
    public static boolean spawnParticlesForPlayer(Player player, Location location, String particleString) {
        if (particleString == null || particleString.isEmpty() || location == null || player == null) return false;

        String[] parts = particleString.split("\\|");
        if (parts.length < 1) return false;

        String particleName = parts[0];
        int count = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        double offsetX = parts.length > 2 ? parseDouble(parts[2], 0.0) : 0.0;
        double offsetY = parts.length > 3 ? parseDouble(parts[3], 0.0) : 0.0;
        double offsetZ = parts.length > 4 ? parseDouble(parts[4], 0.0) : 0.0;
        double extra = parts.length > 5 ? parseDouble(parts[5], 0.0) : 0.0;

        try {
            Particle particle = Particle.valueOf(particleName);

            if (parts.length > 6 && (particle == Particle.REDSTONE || particle == Particle.SPELL_MOB)) {
                Color color = parseColor(parts[6]);
                if (color != null) {
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
                    player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, dustOptions);
                } else {
                    player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
                }
            } else {
                player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Spawns particles at a location.
     *
     * @param location The location to spawn particles at
     * @param particle The particle type
     * @param count The number of particles
     * @param offsetX X offset
     * @param offsetY Y offset
     * @param offsetZ Z offset
     * @param extra Extra data (speed for most particles)
     */
    public static void spawnParticles(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (location == null || location.getWorld() == null) return;
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    /**
     * Spawns colored particles (REDSTONE) at a location.
     *
     * @param location The location to spawn particles at
     * @param color The color of the particles
     * @param count The number of particles
     * @param offsetX X offset
     * @param offsetY Y offset
     * @param offsetZ Z offset
     */
    public static void spawnColoredParticles(Location location, Color color, int count, double offsetX, double offsetY, double offsetZ) {
        if (location == null || location.getWorld() == null || color == null) return;
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
        location.getWorld().spawnParticle(Particle.REDSTONE, location, count, offsetX, offsetY, offsetZ, 0, dustOptions);
    }

    /**
     * Creates a circle of particles around a location.
     *
     * @param center The center location
     * @param particle The particle type
     * @param radius The radius of the circle
     * @param points The number of points in the circle
     */
    public static void spawnParticleCircle(Location center, Particle particle, double radius, int points) {
        if (center == null || center.getWorld() == null) return;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);
            center.getWorld().spawnParticle(particle, particleLocation, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates a line of particles between two locations.
     *
     * @param start The starting location
     * @param end The ending location
     * @param particle The particle type
     * @param density The density of particles (distance between each particle)
     */
    public static void spawnParticleLine(Location start, Location end, Particle particle, double density) {
        if (start == null || end == null || start.getWorld() == null || !start.getWorld().equals(end.getWorld())) return;

        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double i = 0; i < distance; i += density) {
            Location particleLocation = start.clone().add(direction.clone().multiply(i));
            start.getWorld().spawnParticle(particle, particleLocation, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates a sphere of particles around a location.
     *
     * @param center The center location
     * @param particle The particle type
     * @param radius The radius of the sphere
     * @param density The density of particles
     */
    public static void spawnParticleSphere(Location center, Particle particle, double radius, double density) {
        if (center == null || center.getWorld() == null) return;

        for (double phi = 0; phi <= Math.PI; phi += density) {
            for (double theta = 0; theta <= 2 * Math.PI; theta += density) {
                double x = center.getX() + radius * Math.sin(phi) * Math.cos(theta);
                double y = center.getY() + radius * Math.cos(phi);
                double z = center.getZ() + radius * Math.sin(phi) * Math.sin(theta);
                Location particleLocation = new Location(center.getWorld(), x, y, z);
                center.getWorld().spawnParticle(particle, particleLocation, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Parses a color from a string in format "R,G,B"
     *
     * @param colorString The color string
     * @return The Color object, or null if invalid
     */
    private static Color parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) return null;

        String[] rgb = colorString.split(",");
        if (rgb.length != 3) return null;

        try {
            int r = Integer.parseInt(rgb[0].trim());
            int g = Integer.parseInt(rgb[1].trim());
            int b = Integer.parseInt(rgb[2].trim());

            // Validate RGB values
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) return null;

            return Color.fromRGB(r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}