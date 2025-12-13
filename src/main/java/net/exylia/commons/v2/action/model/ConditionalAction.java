package net.exylia.commons.v2.action.model;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class ConditionalAction implements Action {
    private final ActionMetadata metadata;
    private final Predicate<ActionContext> condition;
    private final Action action;

    @Override
    public CompletableFuture<ActionResult> execute(ActionContext context) {
        if (condition.test(context)) {
            return action.execute(context);
        }
        return CompletableFuture.completedFuture(
            ActionResult.cancelled("Condition not met for action: " + metadata.getFullId())
        );
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean canExecute(ActionContext context) {
        return condition.test(context);
    }
}
