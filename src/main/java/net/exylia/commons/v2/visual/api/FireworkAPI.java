package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.builder.FireworkBuilder;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.renderer.FireworkRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class FireworkAPI {
    private FireworkAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void launch(Location location, FireworkEffect.Type type, Color... colors) {
        launch(location, type, List.of(colors));
    }

    public static void launch(Location location, FireworkEffect.Type type, List<Color> colors) {
        if (location.getWorld() == null || location.getWorld().getPlayers().isEmpty()) {
            return;
        }

        Player nearestPlayer = location.getWorld().getPlayers().get(0);
        FireworkConfig config = FireworkBuilder.create()
                .type(type)
                .colors(colors)
                .location(location)
                .build();

        FireworkRenderer.getInstance().render(nearestPlayer, config, PlaceholderContext.create());
    }

    public static void launch(Player player, FireworkEffect.Type type, Color... colors) {
        launch(player.getLocation(), type, colors);
    }

    public static void launch(Player player, String fireworkString) {
        FireworkConfig config = FireworkBuilder.fromString(fireworkString);
        if (config.getLocation() == null) {
            config = FireworkBuilder.create()
                    .type(config.getType())
                    .colors(config.getColors())
                    .fadeColors(config.getFadeColors())
                    .flicker(config.isFlicker())
                    .trail(config.isTrail())
                    .power(config.getPower())
                    .location(player.getLocation())
                    .build();
        }
        FireworkRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void launch(Player player, FireworkConfig config) {
        FireworkRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void launchToFiltered(Predicate<Player> filter, FireworkEffect.Type type, List<Color> colors) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                launch(player, type, colors.toArray(new Color[0]));
            }
        }
    }

    public static void launchInRadius(Location origin, double radius, FireworkEffect.Type type, List<Color> colors) {
        if (origin == null) {
            return;
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        launchToFiltered(filter, type, colors);
    }

    public static CompletableFuture<String> launchContinuous(Player player, FireworkConfig config) {
        return VisualManager.getInstance().launchFireworkContinuous(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> launchCountdown(Player player, FireworkConfig config, long durationTicks) {
        return VisualManager.getInstance().launchFireworkCountdown(player, config, PlaceholderContext.create(), durationTicks);
    }

    public static FireworkBuilder builder() {
        return FireworkBuilder.create();
    }
}
