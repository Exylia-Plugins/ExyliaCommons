package net.exylia.commons.v2.action.core;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.v2.action.audit.AuditLogger;
import net.exylia.commons.v2.action.cache.ActionCacheManager;
import net.exylia.commons.v2.action.cooldown.CooldownManager;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.parser.ArgumentParser;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import net.exylia.commons.v2.action.pipeline.ActionPipeline;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class ActionExecutor {
    private final ActionRegistry registry;
    private final ActionPipeline pipeline;
    private final ActionCacheManager cacheManager;
    private final CooldownManager cooldownManager;
    private final AuditLogger auditLogger;

    public CompletableFuture<ActionResult> executeAsync(String actionString, ActionContext context) {
        return AsyncExecutor.getInstance().supplyAsync(() -> {
            try {
                ParsedArguments parsed = ArgumentParser.parse(actionString);

                Action action = registry.resolve(parsed.getActionId(), context.getDefaultNamespace())
                        .orElseThrow(() -> new ActionException.ActionNotFoundException(parsed.getActionId()));

                ActionContext enrichedContext = context.toBuilder()
                        .arguments(parsed)
                        .action(action)
                        .build();

                ActionResult result = pipeline.execute(action, enrichedContext);

                if (enrichedContext.getExecutionId() != null) {
                    cacheManager.getExecutionCache().recordExecution(enrichedContext.getExecutionId(), result);
                }

                return result;

            } catch (Exception ex) {
                return ActionResult.failure(ex);
            }
        }, false);
    }

    public ActionResult executeSync(String actionString, ActionContext context) {
        try {
            return executeAsync(actionString, context).join();
        } catch (Exception ex) {
            return ActionResult.failure(ex);
        }
    }

    public CompletableFuture<Void> executeBatchAsync(List<String> actionStrings, ActionContext context) {
        return CompletableFuture.allOf(
                actionStrings.stream()
                        .map(actionString -> executeAsync(actionString, context))
                        .toArray(CompletableFuture[]::new)
        );
    }
}
