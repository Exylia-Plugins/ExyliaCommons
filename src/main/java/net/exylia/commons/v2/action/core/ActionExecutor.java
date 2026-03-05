package net.exylia.commons.v2.action.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.cache.ActionCacheManager;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.parser.ArgumentParser;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import net.exylia.commons.v2.action.pipeline.ActionPipeline;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.tasks.api.Tasks;

@RequiredArgsConstructor
public class ActionExecutor {

    private final ActionRegistry registry;
    private final ActionPipeline pipeline;
    private final ActionCacheManager cacheManager;

    public CompletableFuture<ActionResult> executeAsync(
        String actionString,
        ActionContext context
    ) {
        if (actionString == null || actionString.isBlank()) {
            return CompletableFuture.completedFuture(ActionResult.success());
        }

        return Tasks.run(() -> {
            long startTime = System.currentTimeMillis();
            try {
                DebugAPI.logLibDebug(
                    DebugCategory.ACTION,
                    "Executing action: " + actionString
                );

                ParsedArguments parsed = ArgumentParser.parse(actionString);

                Action action = registry
                    .resolve(
                        parsed.getActionId(),
                        context.getDefaultNamespace()
                    )
                    .orElseThrow(() ->
                        new ActionException.ActionNotFoundException(
                            parsed.getActionId()
                        )
                    );

                DebugAPI.logLibDebug(
                    DebugCategory.ACTION,
                    "Resolved action: " + action.getMetadata().getFullId()
                );

                ActionContext enrichedContext = context
                    .toBuilder()
                    .arguments(parsed)
                    .action(action)
                    .build();

                ActionResult result = pipeline.execute(action, enrichedContext);

                if (enrichedContext.getExecutionId() != null) {
                    cacheManager
                        .getExecutionCache()
                        .recordExecution(
                            enrichedContext.getExecutionId(),
                            result
                        );
                }

                long duration = System.currentTimeMillis() - startTime;
                DebugAPI.logLibDebug(
                    DebugCategory.ACTION,
                    "Action executed successfully in " +
                        duration +
                        "ms: " +
                        action.getMetadata().getFullId()
                );

                return result;
            } catch (ActionException ex) {
                long duration = System.currentTimeMillis() - startTime;
                DebugAPI.logLibError(
                    DebugCategory.ACTION,
                    "Action execution failed after " +
                        duration +
                        "ms: " +
                        ex.getMessage()
                );
                return ActionResult.failure(ex);
            } catch (Exception ex) {
                long duration = System.currentTimeMillis() - startTime;
                DebugAPI.logLibError(
                    DebugCategory.ACTION,
                    "Unexpected error during action execution after " +
                        duration +
                        "ms",
                    ex
                );
                return ActionResult.failure(ex);
            }
        }).thenApply(result ->
            result
                .getValue()
                .orElse(
                    ActionResult.failure(new RuntimeException("Task failed"))
                )
        );
    }

    public ActionResult executeSync(
        String actionString,
        ActionContext context
    ) {
        try {
            return executeAsync(actionString, context).join();
        } catch (Exception ex) {
            return ActionResult.failure(ex);
        }
    }

    public CompletableFuture<Void> executeBatchAsync(
        List<String> actionStrings,
        ActionContext context
    ) {
        DebugAPI.logLibDebug(
            DebugCategory.ACTION,
            "Executing batch of " + actionStrings.size() + " actions"
        );
        return CompletableFuture.allOf(
            actionStrings
                .stream()
                .map(actionString -> executeAsync(actionString, context))
                .toArray(CompletableFuture[]::new)
        );
    }
}
