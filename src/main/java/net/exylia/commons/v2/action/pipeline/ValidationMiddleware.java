package net.exylia.commons.v2.action.pipeline;

import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;

public class ValidationMiddleware implements Middleware {

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        if (action == null) {
            throw new ActionException.ActionValidationException("Action cannot be null");
        }

        if (context == null) {
            throw new ActionException.ActionValidationException("ActionContext cannot be null");
        }

        if (context.getPlayer() == null) {
            throw new ActionException.ActionValidationException("Player cannot be null");
        }

        if (action.getMetadata() == null) {
            throw new ActionException.ActionValidationException("ActionMetadata cannot be null");
        }
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
