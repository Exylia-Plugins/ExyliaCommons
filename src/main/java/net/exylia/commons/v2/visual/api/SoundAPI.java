package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.compat.SoundCompat;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.builder.SoundBuilder;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.renderer.SoundRenderer;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class SoundAPI {
    private SoundAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void play(Player player, Sound sound) {
        play(player, sound, 1.0f, 1.0f);
    }

    public static void play(Player player, Sound sound, float volume, float pitch) {
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .build();

        SoundRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void play(Player player, String soundString) {
        SoundConfig config = SoundBuilder.fromString(soundString);
        SoundRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void play(Player player, SoundConfig config) {
        SoundRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void playAt(Location location, Sound sound) {
        playAt(location, sound, 1.0f, 1.0f);
    }

    public static void playAt(Location location, Sound sound, float volume, float pitch) {
        if (location.getWorld() == null || location.getWorld().getPlayers().isEmpty()) {
            return;
        }

        Player nearestPlayer = location.getWorld().getPlayers().get(0);
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .atLocation(location)
                .build();

        SoundRenderer.getInstance().render(nearestPlayer, config, PlaceholderContext.create());
    }

    public static void playAt(Location location, String soundString) {
        SoundConfig config = SoundBuilder.fromString(soundString);
        if (location.getWorld() == null || location.getWorld().getPlayers().isEmpty()) return;
        Player nearestPlayer = location.getWorld().getPlayers().get(0);
        SoundRenderer.getInstance().render(nearestPlayer, SoundBuilder.create()
                .sound(config.getSound())
                .volume(config.getVolume())
                .pitch(config.getPitch())
                .atLocation(location)
                .build(), PlaceholderContext.create());
    }

    public static void playNearby(Player player, Sound sound) {
        playNearby(player, sound, 1.0f, 1.0f);
    }

    public static void playNearby(Player player, Sound sound, float volume, float pitch) {
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .nearby()
                .build();

        SoundRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void playNearby(Player player, String soundString) {
        SoundConfig config = SoundBuilder.fromString(soundString);
        SoundRenderer.getInstance().render(player, SoundBuilder.create()
                .sound(config.getSound())
                .volume(config.getVolume())
                .pitch(config.getPitch())
                .nearby()
                .build(), PlaceholderContext.create());
    }

    public static void playToFiltered(Predicate<Player> filter, Sound sound) {
        playToFiltered(filter, sound, 1.0f, 1.0f);
    }

    public static void playToFiltered(Predicate<Player> filter, Sound sound, float volume, float pitch) {
        SoundConfig config = SoundBuilder.create()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                SoundRenderer.getInstance().render(player, config, PlaceholderContext.create());
            }
        }
    }

    public static void playToFiltered(Predicate<Player> filter, String soundString) {
        SoundConfig config = SoundBuilder.fromString(soundString);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                SoundRenderer.getInstance().render(player, config, PlaceholderContext.create());
            }
        }
    }

    public static void playInRadius(Location origin, double radius, Sound sound) {
        playInRadius(origin, radius, sound, 1.0f, 1.0f);
    }

    public static void playInRadius(Location origin, double radius, Sound sound, float volume, float pitch) {
        if (origin == null) {
            return;
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        playToFiltered(filter, sound, volume, pitch);
    }

    public static void playInRadius(Location origin, double radius, String soundString) {
        if (origin == null) return;
        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;
        playToFiltered(filter, soundString);
    }

    public static CompletableFuture<String> playContinuous(Player player, SoundConfig config) {
        return VisualManager.getInstance().playSoundContinuous(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> playCountdown(Player player, SoundConfig config, long durationTicks) {
        return VisualManager.getInstance().playSoundCountdown(player, config, PlaceholderContext.create(), durationTicks);
    }

    public static void stop(Player player, Sound sound) {
        player.stopSound(sound);
    }

    public static void stop(Player player, Sound sound, SoundCategory category) {
        player.stopSound(sound, category);
    }

    public static void stop(Player player, String soundName) {
        Sound sound = SoundCompat.fromName(soundName);
        if (sound != null) {
            player.stopSound(sound);
        }
    }

    public static void stop(Player player, String soundName, SoundCategory category) {
        Sound sound = SoundCompat.fromName(soundName);
        if (sound != null) {
            player.stopSound(sound, category);
        }
    }

    public static net.kyori.adventure.sound.Sound resolveAdventure(String soundString) {
        if (soundString == null || soundString.isBlank()) return null;
        String[] parts = soundString.split("\\|");
        String name = parts[0].trim();
        float volume = parts.length >= 2 ? parseFloat(parts[1], 1f) : 1f;
        float pitch  = parts.length >= 3 ? parseFloat(parts[2], 1f) : 1f;
        String keyStr = SoundCompat.keyStringOf(name);
        if (keyStr == null) return null;
        return net.kyori.adventure.sound.Sound.sound(Key.key(keyStr), net.kyori.adventure.sound.Sound.Source.MASTER, volume, pitch);
    }

    private static float parseFloat(String s, float fallback) {
        try { return Float.parseFloat(s.trim()); } catch (Exception ignored) { return fallback; }
    }

    public static SoundBuilder builder() {
        return SoundBuilder.create();
    }
}
