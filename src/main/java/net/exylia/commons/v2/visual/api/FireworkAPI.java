package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.FireworkBuilder;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.renderer.FireworkRenderer;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FireworkAPI {
    private FireworkAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void launch(Location location, FireworkEffect.Type type, Color... colors) {
        launch(location, type, List.of(colors));
    }

    public static void launch(Location location, FireworkEffect.Type type, List<Color> colors) {
        if (location.getWorld() == null) {
            return;
        }
        FireworkConfig config = FireworkBuilder.create()
                .type(type)
                .colors(colors)
                .location(location)
                .build();
        FireworkRenderer.getInstance().render(location, config);
    }

    public static void launch(Location location, FireworkConfig config) {
        if (location.getWorld() == null) {
            return;
        }
        FireworkRenderer.getInstance().render(location, config);
    }

    public static void launch(Location location, List<FireworkConfig> configs) {
        for (FireworkConfig config : configs) {
            launch(location, config);
        }
    }

    public static void launch(Location location, String fireworkString) {
        launch(location, FireworkBuilder.fromString(fireworkString));
    }

    public static void launchInRadius(Location origin, double radius, FireworkEffect.Type type, List<Color> colors) {
        launch(randomInRadius(origin, radius), type, colors);
    }

    public static void launchInRadius(Location origin, double radius, FireworkConfig config) {
        launch(randomInRadius(origin, radius), config);
    }

    public static void launchInRadius(Location origin, double radius, List<FireworkConfig> configs) {
        launch(randomInRadius(origin, radius), configs);
    }

    public static void launchInRadius(Location origin, double radius, String fireworkString) {
        launch(randomInRadius(origin, radius), fireworkString);
    }

    public static FireworkBuilder builder() {
        return FireworkBuilder.create();
    }

    private static Location randomInRadius(Location origin, double radius) {
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = ThreadLocalRandom.current().nextDouble() * radius;
        return origin.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }
}
