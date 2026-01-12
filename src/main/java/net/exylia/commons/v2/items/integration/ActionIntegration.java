package net.exylia.commons.v2.items.integration;

import lombok.experimental.UtilityClass;
import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.model.ActionSource;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class ActionIntegration {

    public CompletableFuture<Void> executeActionsForClick(
            ProcessedItem item,
            Player player,
            ClickType clickType,
            PlaceholderContext placeholderContext
    ) {
        if (!item.hasActions()) {
            return CompletableFuture.completedFuture(null);
        }

        if (!ActionManager.isInitialized()) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS,
                "ActionManager not initialized, cannot execute item actions");
            return CompletableFuture.completedFuture(null);
        }

        List<String> actions = item.getActionsForClick(clickType);

        if (actions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        DebugAPI.logLibDebug(DebugCategory.ITEMS,
            "Executing " + actions.size() + " actions for click type " + clickType + " by player " + player.getName());

        ActionContext context = ActionContext.builder()
            .player(player)
            .source(ActionSource.INVENTORY_CLICK)
            .build()
            .withData("placeholderContext", placeholderContext);

        List<CompletableFuture<ActionResult>> futures = actions.stream()
            .map(actionString -> ActionAPI.executeAsync(actionString, context))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                long successful = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(ActionResult::isSuccess)
                    .count();

                DebugAPI.logLibDebug(DebugCategory.ITEMS,
                    "Actions executed: " + successful + "/" + actions.size() + " successful");
            });
    }

    public CompletableFuture<ActionResult> executeSingleAction(
            String actionString,
            Player player,
            PlaceholderContext placeholderContext
    ) {
        if (!ActionManager.isInitialized()) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS,
                "ActionManager not initialized, cannot execute action");
            return CompletableFuture.completedFuture(ActionResult.failure("ActionManager not initialized"));
        }

        ActionContext context = ActionContext.builder()
            .player(player)
            .source(ActionSource.INVENTORY_CLICK)
            .build()
            .withData("placeholderContext", placeholderContext);

        return ActionAPI.executeAsync(actionString, context);
    }
}
