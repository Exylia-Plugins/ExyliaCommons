package net.exylia.commons.v2.action.model;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.action.parser.ParsedArguments;

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

        SchedulerManager.getInstance().runSync(() -> {
            try {
                handler.accept(context, context.getArguments());
                long executionTime = System.currentTimeMillis() - startTime;
                future.complete(ActionResult.success("Action executed successfully", executionTime));
            } catch (Exception e) {
                future.complete(ActionResult.failure(e));
            }
        });

        return future;
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
