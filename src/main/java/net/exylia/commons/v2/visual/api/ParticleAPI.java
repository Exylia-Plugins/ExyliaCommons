package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.compat.ParticleCompat;
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

    public static void spawn(Player player, Particle particle) {
        spawn(player, particle, 1);
    }

    public static void spawn(Player player, Particle particle, int count) {
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .build();

        ParticleRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void spawn(Location location, Particle particle) {
        spawn(location, particle, 1);
    }

    public static void spawn(Location location, Particle particle, int count) {
        if (location.getWorld() == null || location.getWorld().getPlayers().isEmpty()) {
            return;
        }

        Player nearestPlayer = location.getWorld().getPlayers().get(0);
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .atLocation(location)
                .build();

        ParticleRenderer.getInstance().render(nearestPlayer, config, PlaceholderContext.create());
    }

    public static void spawn(Player player, String particleName) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawn(player, particle, 1);
    }

    public static void spawn(Player player, String particleName, int count) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawn(player, particle, count);
    }

    public static void spawn(Location location, String particleName) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawn(location, particle, 1);
    }

    public static void spawn(Location location, String particleName, int count) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawn(location, particle, count);
    }

    public static void spawn(Player player, ParticleConfig config) {
        ParticleRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void spawn(Location location, ParticleConfig config) {
        if (location == null || location.getWorld() == null) return;
        if (location.getWorld().getPlayers().isEmpty()) return;
        ParticleConfig located = ParticleConfig.builder()
                .particle(config.getParticle())
                .count(config.getCount())
                .offsetX(config.getOffsetX())
                .offsetY(config.getOffsetY())
                .offsetZ(config.getOffsetZ())
                .extra(config.getExtra())
                .color(config.getColor())
                .dustSize(config.getDustSize())
                .scope(ParticleConfig.ParticleScope.LOCATION)
                .location(location)
                .build();
        ParticleRenderer.getInstance().render(null, located, PlaceholderContext.create());
    }

    public static void spawnNearby(Player player, String particleName, int count) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawnNearby(player, particle, count);
    }

    public static void spawnNearby(Player player, Particle particle, int count) {
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .nearby()
                .build();

        ParticleRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void spawnToFiltered(Predicate<Player> filter, String particleName, int count) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawnToFiltered(filter, particle, count);
    }

    public static void spawnToFiltered(Predicate<Player> filter, Particle particle, int count) {
        ParticleConfig config = ParticleBuilder.create()
                .particle(particle)
                .count(count)
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                ParticleRenderer.getInstance().render(player, config, PlaceholderContext.create());
            }
        }
    }

    public static void spawnInRadius(Location origin, double radius, String particleName, int count) {
        Particle particle = ParticleCompat.fromName(particleName);
        if (particle != null) spawnInRadius(origin, radius, particle, count);
    }

    public static void spawnInRadius(Location origin, double radius, Particle particle, int count) {
        if (origin == null) {
            return;
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        spawnToFiltered(filter, particle, count);
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
