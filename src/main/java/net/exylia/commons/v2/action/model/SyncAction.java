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

        scheduleOnCorrectThread(context, () -> {
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
    public ActionResult executeDirect(ActionContext context) {
        long startTime = System.currentTimeMillis();
        DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction.executeDirect() called for " + metadata.getFullId());
        if (!isOnCorrectThread(context)) {
            DebugAPI.logLibDebug(DebugCategory.ACTION, "SyncAction.executeDirect() dispatching to correct thread for " + metadata.getFullId());
            CompletableFuture<ActionResult> future = new CompletableFuture<>();
            scheduleOnCorrectThread(context, () -> {
                long executionTime = System.currentTimeMillis() - startTime;
                try {
                    handler.accept(context, context.getArguments());
                    future.complete(ActionResult.success("Action executed successfully", executionTime));
                } catch (Exception e) {
                    DebugAPI.logLibError(DebugCategory.ACTION, "SyncAction handler threw exception for " + metadata.getFullId(), e);
                    future.complete(ActionResult.failure(e));
                }
            });
            return future.join();
        }
        try {
            handler.accept(context, context.getArguments());
            long executionTime = System.currentTimeMillis() - startTime;
            return ActionResult.success("Action executed successfully", executionTime);
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.ACTION, "SyncAction handler threw exception for " + metadata.getFullId(), e);
            return ActionResult.failure(e);
        }
    }

    private boolean isOnCorrectThread(ActionContext context) {
        if (TaskAPI.isFolia()) {
            if (context.getPlayer() != null) {
                return TaskAPI.isEntityThread(context.getPlayer());
            }
        }
        return TaskAPI.isMainThread();
    }

    private void scheduleOnCorrectThread(ActionContext context, Runnable task) {
        if (TaskAPI.isFolia() && context.getPlayer() != null) {
            TaskAPI.at(context.getPlayer(), task);
        } else {
            TaskAPI.runSync(task);
        }
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
