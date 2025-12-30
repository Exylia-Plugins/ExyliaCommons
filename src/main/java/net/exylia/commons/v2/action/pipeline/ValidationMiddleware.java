package net.exylia.commons.v2.action.pipeline;

import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

public class ValidationMiddleware implements Middleware {

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        if (action == null) {
            DebugAPI.logLibError(DebugCategory.ACTION, "Validation failed: Action is null");
            throw new ActionException.ActionValidationException("Action cannot be null");
        }

        if (context == null) {
            DebugAPI.logLibError(DebugCategory.ACTION, "Validation failed: ActionContext is null");
            throw new ActionException.ActionValidationException("ActionContext cannot be null");
        }

        if (context.getPlayer() == null) {
            DebugAPI.logLibError(DebugCategory.ACTION, "Validation failed: Player is null");
            throw new ActionException.ActionValidationException("Player cannot be null");
        }

        if (action.getMetadata() == null) {
            DebugAPI.logLibError(DebugCategory.ACTION, "Validation failed: ActionMetadata is null for action");
            throw new ActionException.ActionValidationException("ActionMetadata cannot be null");
        }

        DebugAPI.logLibDebug(DebugCategory.ACTION, "Validation passed for action: " + action.getMetadata().getFullId());
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
