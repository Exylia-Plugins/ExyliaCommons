package net.exylia.commons.utils.effects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class FullUtils {

    public static void playFullEffects(Location location, ConfigurationSection section) {
        if (section == null) return;

        if (section.getBoolean("particles.enabled", false)) {
            for (String particleString : section.getStringList("particles.particles")) {
                ParticleUtils.spawnParticles(location, particleString);
            }
        }

        if (section.getBoolean("sounds.enabled", false)) {
            for (String soundString : section.getStringList("sounds.sounds")) {
                SoundUtils.playSound(location, soundString);
            }
        }

        if (section.getBoolean("fireworks.enabled", false)) {
            for (String fireworkString : section.getStringList("fireworks.fireworks")) {
                FireworkUtils.launchFirework(location, fireworkString);
            }
        }
    }

    public static void playFullEffects(Player player, ConfigurationSection section) {
        if (section == null) return;

        if (section.getBoolean("particles.enabled", false)) {
            for (String particleString : section.getStringList("particles.particles")) {
                ParticleUtils.spawnParticles(player, player.getLocation(), particleString);
            }
        }

        if (section.getBoolean("sounds.enabled", false)) {
            for (String soundString : section.getStringList("sounds.sounds")) {
                SoundUtils.playSound(player, soundString);
            }
        }

        if (section.getBoolean("fireworks.enabled", false)) {
            for (String fireworkString : section.getStringList("fireworks.fireworks")) {
                FireworkUtils.launchFireworkForPlayer(player, fireworkString);
            }
        }
    }
}
