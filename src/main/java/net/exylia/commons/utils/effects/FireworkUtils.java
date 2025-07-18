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

    /**
     * Lanza un fuego artificial desde una configuración string.
     * Format: TYPE|COLORS|FADE_COLORS|FLICKER|TRAIL|POWER
     * Example: BALL|255,0,0;0,255,0|255,255,255|true|true|1
     * Example simple: STAR|255,100,0|||false|false|2
     *
     * @param location La ubicación donde lanzar el fuego artificial
     * @param fireworkString La configuración del fuego artificial
     * @return true si se lanzó correctamente, false en caso contrario
     */
    public static boolean launchFirework(Location location, String fireworkString) {
        if (fireworkString == null || fireworkString.isEmpty() || location == null) return false;

        String[] parts = fireworkString.split("\\|");
        if (parts.length < 1) return false;

        try {
            // Parsear tipo de efecto
            String typeString = parts[0];
            FireworkEffect.Type type = parseFireworkType(typeString);
            if (type == null) return false;

            // Parsear colores principales
            List<Color> colors = new ArrayList<>();
            if (parts.length > 1 && !parts[1].isEmpty()) {
                colors = parseColors(parts[1]);
            }
            if (colors.isEmpty()) {
                colors.add(Color.RED); // Color por defecto
            }

            // Parsear colores de desvanecimiento
            List<Color> fadeColors = new ArrayList<>();
            if (parts.length > 2 && !parts[2].isEmpty()) {
                fadeColors = parseColors(parts[2]);
            }

            // Parsear efectos especiales
            boolean flicker = parts.length > 3 && parseBoolean(parts[3], false);
            boolean trail = parts.length > 4 && parseBoolean(parts[4], false);

            // Parsear poder del fuego artificial
            int power = parts.length > 5 ? parseInt(parts[5], 1) : 1;
            power = Math.max(0, Math.min(3, power)); // Limitar entre 0-3

            return launchFirework(location, type, colors, fadeColors, flicker, trail, power);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lanza un fuego artificial con parámetros específicos
     *
     * @param location La ubicación donde lanzar
     * @param type Tipo de efecto
     * @param colors Colores principales
     * @param fadeColors Colores de desvanecimiento
     * @param flicker Si debe parpadear
     * @param trail Si debe dejar rastro
     * @param power Poder del fuego artificial (0-3)
     * @return true si se lanzó correctamente
     */
    public static boolean launchFirework(Location location, FireworkEffect.Type type,
                                         List<Color> colors, List<Color> fadeColors,
                                         boolean flicker, boolean trail, int power) {
        if (location == null || location.getWorld() == null) return false;

        try {
            Firework firework = location.getWorld().spawn(location, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();

            // Crear el efecto
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
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lanza un fuego artificial simple con colores aleatorios
     *
     * @param location La ubicación donde lanzar
     * @return true si se lanzó correctamente
     */
    public static boolean launchRandomFirework(Location location) {
        if (location == null) return false;

        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type randomType = types[(int) (Math.random() * types.length)];

        List<Color> randomColors = List.of(
                Color.fromRGB((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256))
        );

        return launchFirework(location, randomType, randomColors, new ArrayList<>(),
                Math.random() > 0.5, Math.random() > 0.5, 1);
    }

    /**
     * Lanza un fuego artificial para un jugador específico (en su ubicación)
     *
     * @param player El jugador
     * @param fireworkString La configuración del fuego artificial
     * @return true si se lanzó correctamente
     */
    public static boolean launchFireworkForPlayer(Player player, String fireworkString) {
        if (player == null) return false;
        return launchFirework(player.getLocation().add(0, 2, 0), fireworkString);
    }

    /**
     * Lanza múltiples fuegos artificiales en secuencia
     *
     * @param location La ubicación base
     * @param fireworkString La configuración
     * @param count Cantidad de fuegos artificiales
     * @param delayTicks Delay entre cada uno (en ticks)
     */
    public static void launchMultipleFireworks(Location location, String fireworkString,
                                               int count, int delayTicks) {
        if (location == null || count <= 0) return;

        for (int i = 0; i < count; i++) {
            final int currentIndex = i;

            // Usar scheduler para el delay
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    getPlugin(),
                    () -> {
                        // Pequeña variación en la posición
                        Location randomLoc = location.clone().add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 2,
                                (Math.random() - 0.5) * 2
                        );
                        launchFirework(randomLoc, fireworkString);
                    },
                    delayTicks * currentIndex
            );
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Convierte string a tipo de fuego artificial
     */
    private static FireworkEffect.Type parseFireworkType(String typeString) {
        if (typeString == null || typeString.isEmpty()) return null;

        try {
            return FireworkEffect.Type.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Aliases comunes
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

    /**
     * Parsea una lista de colores desde string
     * Format: "R,G,B;R,G,B;R,G,B" o "R,G,B"
     */
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

    /**
     * Parsea un color individual desde string "R,G,B"
     */
    private static Color parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) return null;

        // Colores predefinidos
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

        // Parsear RGB
        String[] rgb = colorString.split(",");
        if (rgb.length != 3) return null;

        try {
            int r = Integer.parseInt(rgb[0].trim());
            int g = Integer.parseInt(rgb[1].trim());
            int b = Integer.parseInt(rgb[2].trim());

            // Validar valores RGB
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

    private static org.bukkit.plugin.java.JavaPlugin getPlugin() {
        // Intentar obtener el plugin desde ItemManager o cualquier clase que tenga referencia
        try {
            return net.exylia.commons.item.ItemManager.getPlugin();
        } catch (Exception e) {
            // Fallback: obtener el plugin que proporciona esta clase
            return org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(FireworkUtils.class);
        }
    }

    // ===== MÉTODOS DE CONVENIENCIA =====

    /**
     * Lanza un fuego artificial simple de un color
     */
    public static boolean launchSimpleFirework(Location location, Color color) {
        return launchFirework(location, FireworkEffect.Type.BALL,
                List.of(color), new ArrayList<>(), false, false, 1);
    }

    /**
     * Lanza una celebración de fuegos artificiales
     */
    public static void launchCelebration(Location location, int count) {
        launchMultipleFireworks(location, "BALL|255,0,0;0,255,0;0,0,255|255,255,0|true|true|2",
                count, 10);
    }

    /**
     * Lanza fuegos artificiales de victoria
     */
    public static void launchVictoryFireworks(Player player) {
        Location loc = player.getLocation().add(0, 3, 0);
        launchMultipleFireworks(loc, "STAR|255,215,0;255,255,255|255,255,255|true|true|2", 3, 15);
    }
}