package net.exylia.commons.utils.effects;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;

public class FireworkUtils {

    public static boolean launchFirework(Location location, String fireworkString) {
        if (fireworkString == null || fireworkString.isEmpty() || location == null) return false;

        String[] parts = fireworkString.split("\\|");
        if (parts.length < 1) return false;

        try {
            String typeString = parts[0];
            FireworkEffect.Type type = parseFireworkType(typeString);
            if (type == null) return false;

            List<Color> colors = new ArrayList<>();
            if (parts.length > 1 && !parts[1].isEmpty()) {
                colors = parseColors(parts[1]);
            }
            if (colors.isEmpty()) {
                colors.add(Color.RED);
            }

            List<Color> fadeColors = new ArrayList<>();
            if (parts.length > 2 && !parts[2].isEmpty()) {
                fadeColors = parseColors(parts[2]);
            }

            boolean flicker = parts.length > 3 && parseBoolean(parts[3], false);
            boolean trail = parts.length > 4 && parseBoolean(parts[4], false);

            int power = parts.length > 5 ? parseInt(parts[5], 1) : 1;
            power = Math.max(0, Math.min(3, power));

            return launchFirework(location, type, colors, fadeColors, flicker, trail, power);

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean launchFirework(Location location, FireworkEffect.Type type,
                                         List<Color> colors, List<Color> fadeColors,
                                         boolean flicker, boolean trail, int power) {
        if (location == null || location.getWorld() == null) return false;

        try {
            Runnable fireworkTask = () -> {
                Firework firework = location.getWorld().spawn(location, Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();

                FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                        .with(type)
                        .withColor(colors)
                        .flicker(flicker)
                        .trail(trail);

                if (!fadeColors.isEmpty()) {
                    effectBuilder.withFade(fadeColors);
                }

                FireworkEffect effect = effectBuilder.build();
                meta.addEffect(effect);
                meta.setPower(Math.max(0, Math.min(3, power)));

                firework.setFireworkMeta(meta);
            };

            if (org.bukkit.Bukkit.isPrimaryThread()) {
                fireworkTask.run();
            } else {
                org.bukkit.Bukkit.getScheduler().runTask(net.exylia.commons.ExyliaPlugin.getInstance(), fireworkTask);
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean launchFireworkForPlayer(Player player, String fireworkString) {
        if (player == null || fireworkString == null || fireworkString.isEmpty()) return false;

        String[] parts = fireworkString.split("\\|");
        if (parts.length < 1) return false;

        FireworkScope scope = FireworkScope.PLAYER;
        String typeString = parts[0];

        if (typeString.startsWith("@")) {
            int spaceIndex = typeString.indexOf(' ');
            if (spaceIndex != -1) {
                String scopePart = typeString.substring(0, spaceIndex);
                typeString = typeString.substring(spaceIndex + 1);

                String scopeType = scopePart.substring(1).toLowerCase();
                scope = switch (scopeType) {
                    case "n", "nearby" -> FireworkScope.NEARBY;
                    default -> FireworkScope.PLAYER;
                };
            }
        }

        try {
            FireworkEffect.Type type = parseFireworkType(typeString);
            if (type == null) return false;

            List<Color> colors = new ArrayList<>();
            if (parts.length > 1 && !parts[1].isEmpty()) {
                colors = parseColors(parts[1]);
            }
            if (colors.isEmpty()) {
                colors.add(Color.RED);
            }

            List<Color> fadeColors = new ArrayList<>();
            if (parts.length > 2 && !parts[2].isEmpty()) {
                fadeColors = parseColors(parts[2]);
            }

            boolean flicker = parts.length > 3 && parseBoolean(parts[3], false);
            boolean trail = parts.length > 4 && parseBoolean(parts[4], false);

            int power = parts.length > 5 ? parseInt(parts[5], 1) : 1;
            power = Math.max(0, Math.min(3, power));

            Location loc = player.getLocation().add(0, 2, 0);

            return switch (scope) {
                case PLAYER, NEARBY -> launchFirework(loc, type, colors, fadeColors, flicker, trail, power);
            };

        } catch (Exception e) {
            return false;
        }
    }

    public enum FireworkScope {
        PLAYER, NEARBY
    }

    private static FireworkEffect.Type parseFireworkType(String typeString) {
        if (typeString == null || typeString.isEmpty()) return null;

        try {
            return FireworkEffect.Type.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
             
            switch (typeString.toUpperCase()) {
                case "CIRCLE":
                case "ROUND":
                    return FireworkEffect.Type.BALL;
                case "LARGE_CIRCLE":
                case "BIG_BALL":
                    return FireworkEffect.Type.BALL_LARGE;
                case "EXPLOSION":
                case "BURST":
                    return FireworkEffect.Type.BURST;
                case "SMALL_BALL":
                    return FireworkEffect.Type.BALL;
                default:
                    return null;
            }
        }
    }

    private static List<Color> parseColors(String colorString) {
        List<Color> colors = new ArrayList<>();
        if (colorString == null || colorString.isEmpty()) return colors;

        String[] colorGroups = colorString.split(";");
        for (String colorGroup : colorGroups) {
            Color color = parseColor(colorGroup.trim());
            if (color != null) {
                colors.add(color);
            }
        }

        return colors;
    }

    private static Color parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) return null;

        switch (colorString.toUpperCase()) {
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "YELLOW": return Color.YELLOW;
            case "ORANGE": return Color.ORANGE;
            case "PURPLE": return Color.PURPLE;
            case "WHITE": return Color.WHITE;
            case "BLACK": return Color.BLACK;
            case "PINK": return Color.FUCHSIA;
            case "LIME": return Color.LIME;
            case "CYAN": return Color.AQUA;
            case "MAGENTA": return Color.FUCHSIA;
        }

        String[] rgb = colorString.split(",");
        if (rgb.length != 3) return null;

        try {
            int r = Integer.parseInt(rgb[0].trim());
            int g = Integer.parseInt(rgb[1].trim());
            int b = Integer.parseInt(rgb[2].trim());

            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) return null;

            return Color.fromRGB(r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        String lower = value.toLowerCase().trim();
        return "true".equals(lower) || "yes".equals(lower) || "1".equals(lower);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
