package net.exylia.commons.utils.effects;

import net.exylia.commons.ExyliaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ParticleUtils {

    public enum ParticleScope {
        PLAYER, NEARBY
    }

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

            Runnable particleTask = () -> {
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
            };

            if (Bukkit.isPrimaryThread()) {
                particleTask.run();
            } else {
                Bukkit.getScheduler().runTask(ExyliaPlugin.getInstance(), particleTask);
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean spawnParticles(Player player, Location location, String particleString) {
        if (particleString == null || particleString.isEmpty() || location == null || player == null) return false;

        String[] parts = particleString.split("\\|");
        if (parts.length < 1) return false;

        ParticleScope scope = ParticleScope.PLAYER;
        String particleName = parts[0];

        if (particleName.startsWith("@")) {
            int spaceIndex = particleName.indexOf(' ');
            if (spaceIndex != -1) {
                String scopePart = particleName.substring(0, spaceIndex);
                particleName = particleName.substring(spaceIndex + 1);

                String scopeType = scopePart.substring(1).toLowerCase();
                scope = switch (scopeType) {
                    case "n", "nearby" -> ParticleScope.NEARBY;
                    default -> ParticleScope.PLAYER;
                };
            }
        }

        int count = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        double offsetX = parts.length > 2 ? parseDouble(parts[2], 0.0) : 0.0;
        double offsetY = parts.length > 3 ? parseDouble(parts[3], 0.0) : 0.0;
        double offsetZ = parts.length > 4 ? parseDouble(parts[4], 0.0) : 0.0;
        double extra = parts.length > 5 ? parseDouble(parts[5], 0.0) : 0.0;

        try {
            Particle particle = Particle.valueOf(particleName);
            Color color = null;

            if (parts.length > 6 && (particle == Particle.REDSTONE || particle == Particle.SPELL_MOB)) {
                color = parseColor(parts[6]);
            }

            final Color finalColor = color;
            final ParticleScope finalScope = scope;

            Runnable particleTask = () -> {
                switch (finalScope) {
                    case PLAYER -> spawnParticleForPlayer(player, location, particle, count, offsetX, offsetY, offsetZ, extra, finalColor);
                    case NEARBY -> location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra,
                            finalColor != null ? new Particle.DustOptions(finalColor, 1.0f) : null);
                }
            };

            if (Bukkit.isPrimaryThread()) {
                particleTask.run();
            } else {
                Bukkit.getScheduler().runTask(ExyliaPlugin.getInstance(), particleTask);
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void spawnParticleForPlayer(Player player, Location location, Particle particle, int count,
                                              double offsetX, double offsetY, double offsetZ, double extra, Color color) {
        if (color != null) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, dustOptions);
        } else {
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
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