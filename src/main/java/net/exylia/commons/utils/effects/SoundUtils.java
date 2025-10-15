package net.exylia.commons.utils.effects;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {

    public static boolean playSound(Player player, String soundString) {
        SoundData soundData = parseSound(soundString);
        if (soundData == null) return false;

        Location loc = player.getLocation();

        Runnable soundTask = () -> {
            switch (soundData.getScope()) {
                case PLAYER -> player.playSound(loc, soundData.getSound(), soundData.getVolume(), soundData.getPitch());
                case NEARBY -> loc.getWorld().playSound(loc, soundData.getSound(), soundData.getVolume(), soundData.getPitch());
            }
        };

        if (Bukkit.isPrimaryThread()) {
            soundTask.run();
        } else {
            Bukkit.getScheduler().runTask(ExyliaPlugin.getInstance(), soundTask);
        }

        return true;
    }

    public static boolean playSound(Location location, String soundString) {
        SoundData soundData = parseSound(soundString);
        if (soundData == null) return false;

        Runnable soundTask = () -> location.getWorld().playSound(location, soundData.getSound(), soundData.getVolume(), soundData.getPitch());

        if (Bukkit.isPrimaryThread()) {
            soundTask.run();
        } else {
            Bukkit.getScheduler().runTask(ExyliaPlugin.getInstance(), soundTask);
        }

        return true;
    }

    private static SoundData parseSound(String soundString) {
        if (soundString == null || soundString.isEmpty()) return null;

        String[] parts = soundString.split("\\|");
        if (parts.length < 1) return null;

        SoundScope scope = SoundScope.PLAYER;
        String soundName = parts[0];

        if (soundName.startsWith("@")) {
            int spaceIndex = soundName.indexOf(' ');
            if (spaceIndex != -1) {
                String scopePart = soundName.substring(0, spaceIndex);
                soundName = soundName.substring(spaceIndex + 1);

                String scopeType = scopePart.substring(1).toLowerCase();
                scope = switch (scopeType) {
                    case "n", "nearby" -> SoundScope.NEARBY;
                    default -> SoundScope.PLAYER;
                };
            }
        }

        float volume = parts.length > 1 ? parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? parseFloat(parts[2]) : 1.0f;

        try {
            Sound sound = Sound.valueOf(soundName);
            return new SoundData(sound, volume, pitch, scope);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    public enum SoundScope {
        PLAYER, NEARBY
    }

    @Data
    @AllArgsConstructor
    static class SoundData {
        private Sound sound;
        private float volume;
        private float pitch;
        private SoundScope scope;
    }
}
