package net.exylia.commons.v2.action.pipeline;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.permission.PermissionProvider;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

@RequiredArgsConstructor
public class PermissionMiddleware implements Middleware {
    private final PermissionProvider permissionProvider;

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        String permission = action.getMetadata().getPermission();

        if (permission != null && !permission.isEmpty()) {
            if (!permissionProvider.hasPermission(context.getPlayer(), permission)) {
                DebugAPI.logLibDebug(DebugCategory.ACTION, "Permission check failed for " + action.getMetadata().getFullId() +
                        " - player: " + context.getPlayer().getName() + ", required: " + permission);
                throw new ActionException.ActionPermissionException(permission);
            }
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Permission check passed for " + action.getMetadata().getFullId() +
                    " - player: " + context.getPlayer().getName());
        }
    }

    @Override
    public int getPriority() {
        return 40;
    }
}
