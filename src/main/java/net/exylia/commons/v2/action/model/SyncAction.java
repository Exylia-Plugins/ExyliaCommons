package net.exylia.commons.v2.action.model;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.tasks.api.TaskAPI;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class SyncAction implements Action {
    private final ActionMetadata metadata;
    private final BiConsumer<ActionContext, ParsedArguments> handler;

    @Override
    public CompletableFuture<ActionResult> execute(ActionContext context) {
        CompletableFuture<ActionResult> future = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();

        DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction.execute() called for " + metadata.getFullId());
        DebugAPI.logLibDebug(DebugCategory.ACTION, "Is main thread: " + TaskAPI.isMainThread());

        TaskAPI.runSync(() -> {
            DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction runSync callback executing for " + metadata.getFullId());
            try {
                DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction calling handler.accept() for " + metadata.getFullId());
                handler.accept(context, context.getArguments());
                DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction handler.accept() completed for " + metadata.getFullId());
                long executionTime = System.currentTimeMillis() - startTime;
                future.complete(ActionResult.success("Action executed successfully", executionTime));
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.ACTION, "SyncAction handler threw exception for " + metadata.getFullId(), e);
                future.complete(ActionResult.failure(e));
            }
        });

        DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction.execute() returning future for " + metadata.getFullId());
        return future;
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
