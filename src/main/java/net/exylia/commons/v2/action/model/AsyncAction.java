package net.exylia.commons.v2.action.model;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class AsyncAction implements Action {
    private final ActionMetadata metadata;
    private final BiConsumer<ActionContext, ParsedArguments> handler;

    @Override
    public CompletableFuture<ActionResult> execute(ActionContext context) {
        long startTime = System.currentTimeMillis();
        DebugAPI.logLibDebug(DebugCategory.ACTION, "AsyncAction.execute() called for " + metadata.getFullId());

        return Tasks.run(() -> {
            DebugAPI.logLibDebug(DebugCategory.ACTION, "AsyncAction callback executing for " + metadata.getFullId());
            try {
                DebugAPI.logLibDebug(DebugCategory.ACTION, "AsyncAction calling handler.accept() for " + metadata.getFullId());
                handler.accept(context, context.getArguments());
                DebugAPI.logLibDebug(DebugCategory.ACTION, "AsyncAction handler.accept() completed for " + metadata.getFullId());
                long executionTime = System.currentTimeMillis() - startTime;
                return ActionResult.success("Action executed successfully", executionTime);
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.ACTION, "AsyncAction handler threw exception for " + metadata.getFullId(), e);
                return ActionResult.failure(e);
            }
        }).thenApply(result -> result.getValue().orElse(ActionResult.failure(new RuntimeException("Task failed"))));
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
