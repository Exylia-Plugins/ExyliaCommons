package net.exylia.commons.config.base;

import net.exylia.commons.config.*;
import net.exylia.commons.placeholders.ExyliaContext;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

@Deprecated
@ConfigFile(value = "messages", required = true)
public class MessagesBase extends ConfigBase {

    @Override
    public ConfigurationSystem getSystem() {
        return ConfigManager.getSystem();
    }

    public FileConfiguration file() {
        return getActiveFileConfiguration();
    }

    public static String get(String path) {
        String result = getActiveInstance().getSystem().message(path).buildRawString();
        return result;
    }

    public static String get(String path, Object... replacements) {
        return getActiveInstance().getSystem().message(path).replace(replacements).buildRawString();
    }

    public static String getWithContext(String path, Object context) {
        return getActiveInstance().getSystem().message(path).withContext(ExyliaContext.of(context)).buildRawString();
    }

    public static String getWithContext(String path, ExyliaContext context) {
        return getActiveInstance().getSystem().message(path).withContext(context).buildRawString();
    }

    public static String forPlayer(String path, Player player) {
        return getActiveInstance().getSystem().message(path).forPlayer(player).buildRawString();
    }

    public static String forPlayerContext(String path, Player player, Object context) {
        return getActiveInstance().getSystem().message(path)
                .forPlayer(player)
                .withContext(ExyliaContext.of(context))
                .buildRawString();
    }

    public static Component getComponent(String path) {
        return getActiveInstance().getSystem().getMessage(path);
    }

    public static Component getComponent(String path, Object... replacements) {
        return getActiveInstance().getSystem().getMessage(path, replacements);
    }

    public static Component getComponentWithContext(String path, Object context) {
        return getActiveInstance().getSystem().message(path).withContext(ExyliaContext.of(context)).build();
    }

    public static Component getComponentWithContext(String path, ExyliaContext context) {
        return getActiveInstance().getSystem().message(path).withContext(context).build();
    }

    public static Component getComponentForPlayer(String path, Player player) {
        return getActiveInstance().getSystem().message(path).forPlayer(player).build();
    }

    public static Component getComponentForPlayerContext(String path, Player player, Object context) {
        return getActiveInstance().getSystem().message(path)
                .forPlayer(player)
                .withContext(ExyliaContext.of(context))
                .build();
    }

    public static ConfigurationSystem.MessageBuilder message(String path) {
        return getActiveInstance().getSystem().message(path);
    }

    public static String getRaw(String path) {
        return getActiveInstance().file().getString(path, path);
    }

    public static String getRaw(String path, String defaultValue) {
        return getActiveInstance().file().getString(path, defaultValue);
    }

    private static MessagesBase getActiveInstance() {
        try {
            for (Class<?> clazz : ConfigManager.getSystem().getConfigInstances().keySet()) {
                if (MessagesBase.class.isAssignableFrom(clazz) && !clazz.equals(MessagesBase.class)) {
                    return (MessagesBase) ConfigManager.getSystem().getConfigInstances().get(clazz);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            return ConfigManager.get(MessagesBase.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo obtener la instancia de MessagesBase");
        }
    }

    private FileConfiguration getActiveFileConfiguration() {
        try {
            MessagesBase activeInstance = getActiveInstance();
            if (activeInstance != this) {
                return activeInstance.getConfig();
            }
            return getConfig();
        } catch (Exception e) {
            return getConfig();
        }
    }
}
