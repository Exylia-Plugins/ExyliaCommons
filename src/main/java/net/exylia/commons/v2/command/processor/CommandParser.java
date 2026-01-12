package net.exylia.commons.v2.command.processor;

import net.exylia.commons.v2.command.model.Command;
import net.exylia.commons.v2.command.model.CommandSource;
import net.exylia.commons.v2.command.model.CommandType;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandParser {

    public static CompletableFuture<Command> parseAsync(
            String commandString,
            Player player,
            PlaceholderContext context
    ) {
        return CompletableFuture.supplyAsync(() -> parse(commandString, player, context));
    }

    public static Command parse(String commandString, Player player, PlaceholderContext context) {
        if (commandString == null || commandString.isEmpty()) {
            throw new IllegalArgumentException("Command string cannot be null or empty");
        }

        CommandType type = CommandType.fromString(commandString);
        String stripped = type.stripPrefix(commandString);

        String processed;
        if (player != null && context != null) {
            processed = Placeholders.process(stripped, player, context);
        } else if (player != null) {
            processed = Placeholders.process(stripped, player, PlaceholderContext.create());
        } else {
            processed = stripped;
        }

        CommandSource source = type.isRequiresProxy() ? CommandSource.PROXY : CommandSource.LOCAL;

        return Command.builder()
                .id(UUID.randomUUID().toString())
                .rawCommand(commandString)
                .processedCommand(processed)
                .type(type)
                .source(source)
                .async(true)
                .build();
    }
}
