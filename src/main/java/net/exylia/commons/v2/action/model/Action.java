package net.exylia.commons.v2.action.model;

import java.util.concurrent.CompletableFuture;

public interface Action {
    ActionMetadata getMetadata();

    CompletableFuture<ActionResult> execute(ActionContext context);

    default ActionPriority getPriority() {
        return getMetadata().getPriority();
    }

    default boolean canExecute(ActionContext context) {
        return true;
    }
}
