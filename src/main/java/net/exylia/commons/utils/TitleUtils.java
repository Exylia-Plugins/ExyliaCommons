package net.exylia.commons.utils;

import net.exylia.commons.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;

public class TitleUtils {
    public static void sendTitle(Player player, ConfigurationSection section, String... replacements) {
        if (section == null) return;
        boolean enabled = section.getBoolean("enabled");
        if (!enabled) return;
        String title = section.getString("title");
        String subtitle = section.getString("subtitle");
        title = ConfigManager.applyColorPresets(title);
        subtitle = ConfigManager.applyColorPresets(subtitle);
        if (title != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                title = title.replace(replacements[i], replacements[i + 1]);
            }
        }
        if (subtitle != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                subtitle = subtitle.replace(replacements[i], replacements[i + 1]);
            }
        }
        int fadeIn = section.getInt("fadeIn");
        int stay = section.getInt("stay");
        int fadeOut = section.getInt("fadeOut");
        MessageUtils.sendTitleAsync(player, title, subtitle, fadeIn, stay, fadeOut);
    }
    public static void sendTitle(Collection<Player> players, ConfigurationSection section, String... replacements) {
        if (section == null) return;
        boolean enabled = section.getBoolean("enabled");
        if (!enabled) return;
        String title = section.getString("title");
        String subtitle = section.getString("subtitle");
        title = ConfigManager.applyColorPresets(title);
        subtitle = ConfigManager.applyColorPresets(subtitle);
        if (title != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                title = title.replace(replacements[i], replacements[i + 1]);
            }
        }
        if (subtitle != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                subtitle = subtitle.replace(replacements[i], replacements[i + 1]);
            }
        }
        int fadeIn = section.getInt("fadeIn");
        int stay = section.getInt("stay");
        int fadeOut = section.getInt("fadeOut");
        MessageUtils.sendTitleAsync(players, title, subtitle, fadeIn, stay, fadeOut);
    }
}
