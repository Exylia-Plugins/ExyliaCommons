package net.exylia.commons.v2.items.integration;

import lombok.experimental.UtilityClass;
import net.exylia.commons.v2.command.api.CommandAPI;
import net.exylia.commons.v2.command.core.CommandManager;
import net.exylia.commons.v2.command.model.CommandContext;
import net.exylia.commons.v2.command.model.CommandResult;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@UtilityClass
public class CommandIntegration {

    public CompletableFuture<List<CommandResult>> executeCommandsForClick(
            ProcessedItem item,
            Player player,
            ClickType clickType,
            PlaceholderContext placeholderContext
    ) {
        if (!item.hasCommands()) {
            return CompletableFuture.completedFuture(List.of());
        }

        CommandManager manager = CommandManager.getInstance();

        List<String> commands = item.getCommandsForClick(clickType);

        if (commands.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        DebugAPI.logLibDebug(DebugCategory.ITEMS,
            "Executing " + commands.size() + " commands for click type " + clickType + " by player " + player.getName());

        return manager.executeBatch(player, commands, placeholderContext);
    }

    public CompletableFuture<CommandResult> executeSingleCommand(
            String commandString,
            Player player,
            PlaceholderContext placeholderContext
    ) {
        CommandContext context = CommandContext.builder()
            .player(player)
            .placeholderContext(placeholderContext)
            .build();

        return CommandAPI.executeAsync(commandString, context);
    }
}
