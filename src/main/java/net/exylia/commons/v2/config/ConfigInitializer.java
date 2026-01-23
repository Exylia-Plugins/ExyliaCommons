package net.exylia.commons.v2.config;

import org.bukkit.plugin.java.JavaPlugin;

public class ConfigInitializer {

    public static void initConfigs(JavaPlugin plugin) {
        Configs.init(plugin);
    }

    public static void initMessages() {
        Config messagesConfig = Configs.get("messages");
        Messages.init(messagesConfig);
    }

    public static void init(JavaPlugin plugin) {
        Configs.init(plugin);

        Config messagesConfig = Configs.get("messages");
        Messages.init(messagesConfig);
    }

    public static void init(JavaPlugin plugin, String... preloadFiles) {
        Configs.init(plugin);

        for (String fileName : preloadFiles) {
            Configs.get(fileName);
        }

        Config messagesConfig = Configs.get("messages");
        Messages.init(messagesConfig);
    }
}
