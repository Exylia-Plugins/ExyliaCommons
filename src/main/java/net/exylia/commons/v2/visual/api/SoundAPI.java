package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.builder.SoundBuilder;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.renderer.SoundRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class SoundAPI {
    private SoundAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<Void> play(Player player, Sound sound) {
        return play(player, sound, 1.0f, 1.0f);
    }

    public static CompletableFuture<Void> play(Player player, Sound sound, float volume, float pitch) {
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .build();

        return SoundRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> play(Player player, String soundString) {
        SoundConfig config = SoundBuilder.fromString(soundString);
        return SoundRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> play(Player player, SoundConfig config) {
        return SoundRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> playAt(Location location, Sound sound) {
        return playAt(location, sound, 1.0f, 1.0f);
    }

    public static CompletableFuture<Void> playAt(Location location, Sound sound, float volume, float pitch) {
        if (location.getWorld() == null || !location.getWorld().getPlayers().isEmpty()) {
            Player nearestPlayer = location.getWorld().getPlayers().get(0);
            SoundConfig config = SoundBuilder.create()
                    .sound(sound)
                    .volume(volume)
                    .pitch(pitch)
                    .atLocation(location)
                    .build();

            return SoundRenderer.getInstance().renderAsync(nearestPlayer, config, PlaceholderContext.create());
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> playNearby(Player player, Sound sound) {
        return playNearby(player, sound, 1.0f, 1.0f);
    }

    public static CompletableFuture<Void> playNearby(Player player, Sound sound, float volume, float pitch) {
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .nearby()
                .build();

        return SoundRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> playToFiltered(Predicate<Player> filter, Sound sound) {
        return playToFiltered(filter, sound, 1.0f, 1.0f);
    }

    public static CompletableFuture<Void> playToFiltered(Predicate<Player> filter, Sound sound, float volume, float pitch) {
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                SoundRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> playInRadius(Location origin, double radius, Sound sound) {
        return playInRadius(origin, radius, sound, 1.0f, 1.0f);
    }

    public static CompletableFuture<Void> playInRadius(Location origin, double radius, Sound sound, float volume, float pitch) {
        if (origin == null) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return playToFiltered(filter, sound, volume, pitch);
    }

    public static CompletableFuture<String> playContinuous(Player player, SoundConfig config) {
        return VisualManager.getInstance().playSoundContinuous(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> playCountdown(Player player, SoundConfig config, long durationTicks) {
        return VisualManager.getInstance().playSoundCountdown(player, config, PlaceholderContext.create(), durationTicks);
    }

    public static SoundBuilder builder() {
        return SoundBuilder.create();
    }
}
