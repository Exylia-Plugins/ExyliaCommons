package net.exylia.commons.v2.action.model;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.v2.action.parser.ParsedArguments;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class AsyncAction implements Action {
    private final ActionMetadata metadata;
    private final BiConsumer<ActionContext, ParsedArguments> handler;

    @Override
    public CompletableFuture<ActionResult> execute(ActionContext context) {
        long startTime = System.currentTimeMillis();

        return AsyncExecutor.getInstance().supplyAsync(() -> {
            try {
                handler.accept(context, context.getArguments());
                long executionTime = System.currentTimeMillis() - startTime;
                return ActionResult.success("Action executed successfully", executionTime);
            } catch (Exception e) {
                return ActionResult.failure(e);
            }
        }, false);
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
