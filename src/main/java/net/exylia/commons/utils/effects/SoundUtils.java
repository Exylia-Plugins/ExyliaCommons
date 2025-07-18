package net.exylia.commons.utils.effects;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {

    /**
     * Plays a sound to a player from a formatted string.
     * Format: SOUND_NAME|VOLUME|PITCH
     * Example: BLOCK_NOTE_BLOCK_PLING|0.5|1.0
     *
     * @param player The player to play the sound to
     * @param soundString The formatted sound string
     * @return true if the sound was played, false otherwise
     */
    public static boolean playSound(Player player, String soundString) {
        if (soundString == null || soundString.isEmpty()) return false;

        String[] parts = soundString.split("\\|");
        if (parts.length < 1) return false;

        String soundName = parts[0];
        float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Plays a sound to a player.
     *
     * @param player The player to play the sound to
     * @param sound The sound to play
     * @param volume The volume (0.0 to 1.0)
     * @param pitch The pitch (0.5 to 2.0)
     */
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}