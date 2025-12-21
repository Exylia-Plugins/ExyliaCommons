package net.exylia.commons.v2.ui.model;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.model.ActionSource;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
@Builder
public class ClickAction {
    private final String actionId;
    private final String actionString;
    @Builder.Default
    private final Map<String, String> arguments = new HashMap<>();
    @Builder.Default
    private final boolean async = true;
    @Builder.Default
    private final long cooldown = 0L;
    private final String permission;
    @Builder.Default
    private final boolean closeMenu = false;
    @Builder.Default
    private final boolean refreshMenu = false;
    private final Consumer<ActionResult> resultHandler;

    public CompletableFuture<ActionResult> executeAsync(Player player, MenuContext menuContext) {
        if (actionString == null || actionString.isEmpty()) {
            return CompletableFuture.completedFuture(
                ActionResult.failure("No action string provided")
            );
        }

        Map<String, Object> contextData = new HashMap<>();

        if (menuContext != null && menuContext.getMetadata() != null) {
            contextData.putAll(menuContext.getMetadata());
        }

        ActionContext actionContext = ActionContext.builder()
            .player(player)
            .source(ActionSource.MENU)
            .data(contextData)
            .build();

        return ActionAPI.executeAsync(actionString, actionContext)
            .thenApply(result -> {
                if (resultHandler != null) {
                    resultHandler.accept(result);
                }

                if (result.isSuccess()) {
                    if (closeMenu && menuContext != null && menuContext.getPlayer() != null) {
                        Schedulers.sync(() -> menuContext.getPlayer().closeInventory());
                    }
                }

                return result;
            })
            .exceptionally(throwable -> {
                return ActionResult.failure("Action execution failed: " + throwable.getMessage());
            });
    }

    public ActionResult execute(Player player, MenuContext menuContext) {
        try {
            return executeAsync(player, menuContext).join();
        } catch (Exception e) {
            return ActionResult.failure("Action execution failed: " + e.getMessage());
        }
    }

    public static ClickAction fromString(String actionString) {
        return builder()
            .actionString(actionString)
            .async(true)
            .build();
    }
}
