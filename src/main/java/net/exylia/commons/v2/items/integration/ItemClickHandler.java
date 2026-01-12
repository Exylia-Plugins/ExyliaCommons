package net.exylia.commons.v2.items.integration;

import lombok.experimental.UtilityClass;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class ItemClickHandler {

    public CompletableFuture<Void> handleClick(
            ProcessedItem item,
            Player player,
            ClickType clickType
    ) {
        return handleClick(item, player, clickType, PlaceholderContext.create());
    }

    public CompletableFuture<Void> handleClick(
            ProcessedItem item,
            Player player,
            ClickType clickType,
            PlaceholderContext placeholderContext
    ) {
        if (item == null) {
            return CompletableFuture.completedFuture(null);
        }

        boolean hasActions = item.hasActions();
        boolean hasCommands = item.hasCommands();

        if (!hasActions && !hasCommands) {
            return CompletableFuture.completedFuture(null);
        }

        DebugAPI.logLibDebug(DebugCategory.ITEMS,
            "Handling click " + clickType + " for player " + player.getName() +
            " (actions: " + hasActions + ", commands: " + hasCommands + ")");

        CompletableFuture<Void> actionsFuture = hasActions
            ? ActionIntegration.executeActionsForClick(item, player, clickType, placeholderContext)
            : CompletableFuture.completedFuture(null);

        CompletableFuture<Void> commandsFuture = hasCommands
            ? CommandIntegration.executeCommandsForClick(item, player, clickType, placeholderContext)
                .thenAccept(results -> {})
            : CompletableFuture.completedFuture(null);

        return CompletableFuture.allOf(actionsFuture, commandsFuture);
    }

    public CompletableFuture<Void> handleClickWithContext(
            ProcessedItem item,
            Player player,
            ClickType clickType,
            PlaceholderContext context
    ) {
        return handleClick(item, player, clickType, context);
    }
}
