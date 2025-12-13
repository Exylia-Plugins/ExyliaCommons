package net.exylia.commons.v2.action.model;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class ChainedAction implements Action {
    private final ActionMetadata metadata;
    private final List<Action> actions;

    @Override
    public CompletableFuture<ActionResult> execute(ActionContext context) {
        CompletableFuture<ActionResult> chain = CompletableFuture.completedFuture(ActionResult.success());

        for (Action action : actions) {
            chain = chain.thenCompose(result -> {
                if (!result.isSuccess()) {
                    return CompletableFuture.completedFuture(result);
                }
                return action.execute(context);
            });
        }

        return chain;
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
