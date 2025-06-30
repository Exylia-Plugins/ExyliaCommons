package net.exylia.commons.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Interface que proporciona automáticamente todos los métodos de mensajería
 * a cualquier configuración que la implemente
 */
public interface MessageConfig {

    // Métodos que debe implementar la clase
    ConfigurationSystem getSystem();
    FileConfiguration file();

    // ===== MÉTODOS DEFAULT AUTOMÁTICOS =====

    default Component get(String path) {
        return getSystem().getMessage(path);
    }

    default Component get(String path, Object... replacements) {
        return getSystem().getMessage(path, replacements);
    }

    default ConfigurationSystem.MessageBuilder message(String path) {
        return getSystem().message(path);
    }

    default Component getWithContext(String path, Object context) {
        return getSystem().message(path).withContext(context).build();
    }

    default Component getForPlayer(String path, Player player) {
        return getSystem().message(path).forPlayer(player).build();
    }

    default Component getComplete(String path, Object context, Player player, Object... replacements) {
        return getSystem().message(path)
                .withContext(context)
                .forPlayer(player)
                .replace(replacements)
                .build();
    }

    default String getRaw(String path) {
        return file().getString(path, path);
    }

    default String getRaw(String path, String defaultValue) {
        return file().getString(path, defaultValue);
    }

    default String getString(String path) {
        return getSystem().message(path).buildString();
    }

    default String getString(String path, Object... replacements) {
        return getSystem().message(path).replace(replacements).buildString();
    }
}