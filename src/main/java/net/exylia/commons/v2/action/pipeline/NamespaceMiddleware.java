package net.exylia.commons.v2.action.pipeline;

import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.namespace.NamespaceValidator;

public class NamespaceMiddleware implements Middleware {

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        String fullId = action.getMetadata().getFullId();

        if (!NamespaceValidator.isValidFullId(fullId)) {
            throw new ActionException.ActionValidationException(
                    "Invalid action ID format: " + fullId + ". Must be lowercase alphanumeric with underscores only."
            );
        }
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
