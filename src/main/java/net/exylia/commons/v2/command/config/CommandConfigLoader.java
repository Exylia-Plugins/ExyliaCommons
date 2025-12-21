package net.exylia.commons.v2.command.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class CommandConfigLoader {

    public List<String> loadCommands(ConfigurationSection section) {
        return loadCommandsFromKey(section, "commands");
    }

    public List<String> loadCommandsFromKey(
            ConfigurationSection section,
            String key
    ) {
        List<String> commands = new ArrayList<>();

        if (section == null) {
            return commands;
        }

        if (section.isList(key)) {
            List<?> rawList = section.getList(key);
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (obj instanceof String) {
                        commands.add((String) obj);
                    } else if (obj instanceof ConfigurationSection) {
                        ConfigurationSection cmdSection = (ConfigurationSection) obj;
                        String command = cmdSection.getString("command");
                        if (command != null) {
                            commands.add(command);
                        }
                    }
                }
            }
        } else if (section.isConfigurationSection(key)) {
            ConfigurationSection cmdSection = section.getConfigurationSection(key);
            if (cmdSection != null) {
                for (String subKey : cmdSection.getKeys(false)) {
                    String command = cmdSection.getString(subKey);
                    if (command != null) {
                        commands.add(command);
                    }
                }
            }
        } else if (section.isString(key)) {
            String command = section.getString(key);
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }

    public CommandConfig loadCommandConfig(ConfigurationSection section) {
        return CommandConfig.builder()
                .command(section.getString("command"))
                .type(section.getString("type", "player"))
                .delay(section.getLong("delay", 0))
                .permission(section.getString("permission"))
                .condition(section.getString("condition"))
                .async(section.getBoolean("async", true))
                .build();
    }
}
