package net.exylia.commons.v2.command.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommandType {
    PLAYER("player:", false),
    CONSOLE("console:", false),
    PLAYER_PROXY("player-proxy:", true),
    CONSOLE_PROXY("console-proxy:", true);

    private final String prefix;
    private final boolean requiresProxy;

    public static CommandType fromString(String commandString) {
        if (commandString == null || commandString.isEmpty()) {
            return CONSOLE;
        }

        String lower = commandString.toLowerCase().trim();

        for (CommandType type : values()) {
            if (lower.startsWith(type.getPrefix())) {
                return type;
            }
        }

        return CONSOLE;
    }

    public String stripPrefix(String commandString) {
        if (commandString == null || commandString.isEmpty()) {
            return commandString;
        }

        if (commandString.toLowerCase().startsWith(prefix)) {
            return commandString.substring(prefix.length()).trim();
        }

        return commandString;
    }
}
