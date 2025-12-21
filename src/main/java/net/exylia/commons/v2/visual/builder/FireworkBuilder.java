package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FireworkBuilder extends VisualBuilder<FireworkConfig, FireworkBuilder> {
    private FireworkEffect.Type type = FireworkEffect.Type.BALL;
    private final List<Color> colors = new ArrayList<>();
    private final List<Color> fadeColors = new ArrayList<>();
    private boolean flicker = false;
    private boolean trail = false;
    private int power = 1;
    private Location location;

    private FireworkBuilder() {
    }

    public static FireworkBuilder create() {
        return new FireworkBuilder();
    }

    public FireworkBuilder type(FireworkEffect.Type type) {
        this.type = type;
        return this;
    }

    public FireworkBuilder type(String typeName) {
        String normalized = typeName.toUpperCase()
                .replace("CIRCLE", "BALL")
                .replace("EXPLOSION", "BURST");
        try {
            this.type = FireworkEffect.Type.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid firework type: " + typeName);
        }
        return this;
    }

    public FireworkBuilder color(Color color) {
        this.colors.add(color);
        return this;
    }

    public FireworkBuilder color(int r, int g, int b) {
        return color(Color.fromRGB(r, g, b));
    }

    public FireworkBuilder colors(Color... colors) {
        this.colors.addAll(Arrays.asList(colors));
        return this;
    }

    public FireworkBuilder colors(List<Color> colors) {
        this.colors.addAll(colors);
        return this;
    }

    public FireworkBuilder fadeColor(Color color) {
        this.fadeColors.add(color);
        return this;
    }

    public FireworkBuilder fadeColor(int r, int g, int b) {
        return fadeColor(Color.fromRGB(r, g, b));
    }

    public FireworkBuilder fadeColors(Color... colors) {
        this.fadeColors.addAll(Arrays.asList(colors));
        return this;
    }

    public FireworkBuilder fadeColors(List<Color> colors) {
        this.fadeColors.addAll(colors);
        return this;
    }

    public FireworkBuilder flicker(boolean flicker) {
        this.flicker = flicker;
        return this;
    }

    public FireworkBuilder flicker() {
        return flicker(true);
    }

    public FireworkBuilder trail(boolean trail) {
        this.trail = trail;
        return this;
    }

    public FireworkBuilder trail() {
        return trail(true);
    }

    public FireworkBuilder power(int power) {
        this.power = Math.max(0, Math.min(3, power));
        return this;
    }

    public FireworkBuilder location(Location location) {
        this.location = location;
        return this;
    }

    private static Color parseColor(String colorStr) {
        colorStr = colorStr.trim().toUpperCase();

        return switch (colorStr) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "BLUE" -> Color.BLUE;
            case "YELLOW" -> Color.YELLOW;
            case "ORANGE" -> Color.ORANGE;
            case "PURPLE" -> Color.PURPLE;
            case "WHITE" -> Color.WHITE;
            case "BLACK" -> Color.BLACK;
            case "PINK" -> Color.fromRGB(255, 192, 203);
            case "LIME" -> Color.LIME;
            case "CYAN" -> Color.fromRGB(0, 255, 255);
            case "MAGENTA" -> Color.fromRGB(255, 0, 255);
            default -> {
                String[] rgb = colorStr.split(",");
                if (rgb.length == 3) {
                    try {
                        int r = Integer.parseInt(rgb[0].trim());
                        int g = Integer.parseInt(rgb[1].trim());
                        int b = Integer.parseInt(rgb[2].trim());
                        yield Color.fromRGB(r, g, b);
                    } catch (NumberFormatException e) {
                        yield Color.WHITE;
                    }
                }
                yield Color.WHITE;
            }
        };
    }

    public static FireworkConfig fromString(String fireworkString) {
        String[] parts = fireworkString.split("\\|");
        FireworkBuilder builder = create();

        if (parts.length >= 1) {
            builder.type(parts[0].trim());
        }
        if (parts.length >= 2) {
            String[] colorStrs = parts[1].trim().split(";");
            for (String colorStr : colorStrs) {
                builder.color(parseColor(colorStr));
            }
        }
        if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
            String[] fadeColorStrs = parts[2].trim().split(";");
            for (String colorStr : fadeColorStrs) {
                builder.fadeColor(parseColor(colorStr));
            }
        }
        if (parts.length >= 4) {
            String flickerStr = parts[3].trim().toLowerCase();
            builder.flicker("true".equals(flickerStr) || "yes".equals(flickerStr) || "1".equals(flickerStr));
        }
        if (parts.length >= 5) {
            String trailStr = parts[4].trim().toLowerCase();
            builder.trail("true".equals(trailStr) || "yes".equals(trailStr) || "1".equals(trailStr));
        }
        if (parts.length >= 6) {
            try {
                builder.power(Integer.parseInt(parts[5].trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return builder.build();
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (type == null) {
            errors.add("Firework type must be specified");
        }

        if (colors.isEmpty()) {
            errors.add("At least one color must be specified");
        }

        if (power < 0 || power > 3) {
            errors.add("Power must be between 0 and 3");
        }

        if (location == null) {
            errors.add("Location must be specified for fireworks");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected FireworkConfig buildInternal() {
        FireworkConfig config = FireworkConfig.builder()
                .type(type)
                .colors(colors)
                .fadeColors(fadeColors)
                .flicker(flicker)
                .trail(trail)
                .power(power)
                .location(location)
                .build();

        config.setEnabled(enabled);
        config.setUpdateInterval(updateInterval);
        config.setPermanent(permanent);

        return config;
    }
}
