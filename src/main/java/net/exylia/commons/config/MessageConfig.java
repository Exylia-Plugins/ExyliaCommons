package net.exylia.commons.config;

import net.exylia.commons.placeholders.ExyliaContext;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

@Deprecated
public interface MessageConfig {

    ConfigurationSystem getSystem();
    FileConfiguration file();

    default Component get(String path) {
        return getSystem().getMessage(path);
    }

    default Component get(String path, Object... replacements) {
        return getSystem().getMessage(path, replacements);
    }

    default ConfigurationSystem.MessageBuilder message(String path) {
        return getSystem().message(path);
    }

    default Component getWithContext(String path, ExyliaContext context) {
        return getSystem().message(path).withContext(context).build();
    }

    default Component getForPlayer(String path, Player player) {
        return getSystem().message(path).forPlayer(player).build();
    }

    default Component getComplete(String path, ExyliaContext context, Player player, Object... replacements) {
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
