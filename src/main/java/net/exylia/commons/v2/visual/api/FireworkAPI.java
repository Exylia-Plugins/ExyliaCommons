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

    public static CompletableFuture<Void> launch(Location location, FireworkEffect.Type type, Color... colors) {
        return launch(location, type, List.of(colors));
    }

    public static CompletableFuture<Void> launch(Location location, FireworkEffect.Type type, List<Color> colors) {
        if (location.getWorld() == null || location.getWorld().getPlayers().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Player nearestPlayer = location.getWorld().getPlayers().get(0);
        FireworkConfig config = FireworkBuilder.create()
                .type(type)
                .colors(colors)
                .location(location)
                .build();

        return FireworkRenderer.getInstance().renderAsync(nearestPlayer, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> launch(Player player, FireworkEffect.Type type, Color... colors) {
        return launch(player.getLocation(), type, colors);
    }

    public static CompletableFuture<Void> launch(Player player, String fireworkString) {
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
        return FireworkRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> launch(Player player, FireworkConfig config) {
        return FireworkRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> launchToFiltered(Predicate<Player> filter, FireworkEffect.Type type, List<Color> colors) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                launch(player, type, colors.toArray(new Color[0]));
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> launchInRadius(Location origin, double radius, FireworkEffect.Type type, List<Color> colors) {
        if (origin == null) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return launchToFiltered(filter, type, colors);
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
