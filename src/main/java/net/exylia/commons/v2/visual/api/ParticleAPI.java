package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.renderer.ParticleRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class ParticleAPI {
    private ParticleAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<Void> spawn(Player player, Particle particle) {
        return spawn(player, particle, 1);
    }

    public static CompletableFuture<Void> spawn(Player player, Particle particle, int count) {
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .build();

        return ParticleRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> spawn(Location location, Particle particle) {
        return spawn(location, particle, 1);
    }

    public static CompletableFuture<Void> spawn(Location location, Particle particle, int count) {
        if (location.getWorld() == null || location.getWorld().getPlayers().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Player nearestPlayer = location.getWorld().getPlayers().get(0);
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .atLocation(location)
                .build();

        return ParticleRenderer.getInstance().renderAsync(nearestPlayer, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> spawn(Player player, String particleString) {
        ParticleConfig config = ParticleBuilder.fromString(particleString);
        return ParticleRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> spawn(Player player, ParticleConfig config) {
        return ParticleRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> spawnNearby(Player player, Particle particle, int count) {
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .nearby()
                .build();

        return ParticleRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> spawnToFiltered(Predicate<Player> filter, Particle particle, int count) {
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                ParticleRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> spawnInRadius(Location origin, double radius, Particle particle, int count) {
        if (origin == null) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return spawnToFiltered(filter, particle, count);
    }

    public static CompletableFuture<String> spawnContinuous(Player player, ParticleConfig config) {
        return VisualManager.getInstance().spawnParticleContinuous(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> spawnCountdown(Player player, ParticleConfig config, long durationTicks) {
        return VisualManager.getInstance().spawnParticleCountdown(player, config, PlaceholderContext.create(), durationTicks);
    }

    public static ParticleBuilder builder() {
        return ParticleBuilder.create();
    }
}
