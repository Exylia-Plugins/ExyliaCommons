package net.exylia.commons.v2.action.pipeline;

import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;

public interface Middleware {
    void execute(Action action, ActionContext context) throws ActionException;

    default int getPriority() {
        return 100;
    }

    default boolean isEnabled() {
        return true;
    }
}
