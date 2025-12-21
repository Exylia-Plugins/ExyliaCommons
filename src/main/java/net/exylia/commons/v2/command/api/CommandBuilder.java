package net.exylia.commons.v2.command.api;

import net.exylia.commons.v2.command.model.Command;
import net.exylia.commons.v2.command.model.CommandType;
import net.exylia.commons.v2.command.processor.CommandParser;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandBuilder {
    private String command;
    private CommandType type = CommandType.PLAYER;
    private boolean async = true;
    private long delay = 0L;
    private String permission;
    private String condition;

    private CommandBuilder() {
    }

    public static CommandBuilder create() {
        return new CommandBuilder();
    }

    public CommandBuilder command(String command) {
        this.command = command;
        return this;
    }

    public CommandBuilder type(CommandType type) {
        this.type = type;
        return this;
    }

    public CommandBuilder asPlayer() {
        this.type = CommandType.PLAYER;
        return this;
    }

    public CommandBuilder asConsole() {
        this.type = CommandType.CONSOLE;
        return this;
    }

    public CommandBuilder asPlayerProxy() {
        this.type = CommandType.PLAYER_PROXY;
        return this;
    }

    public CommandBuilder asConsoleProxy() {
        this.type = CommandType.CONSOLE_PROXY;
        return this;
    }

    public CommandBuilder async(boolean async) {
        this.async = async;
        return this;
    }

    public CommandBuilder delay(long ticks) {
        this.delay = ticks;
        return this;
    }

    public CommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public CommandBuilder condition(String condition) {
        this.condition = condition;
        return this;
    }

    public Command build(Player player, PlaceholderContext context) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }

        String fullCommand = type.getPrefix() + command;
        return CommandParser.parse(fullCommand, player, context);
    }
}
