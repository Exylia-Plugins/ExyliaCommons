package net.exylia.commons.v2.action.pipeline;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.cooldown.CooldownManager;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.util.UUID;

@RequiredArgsConstructor
public class CooldownApplyMiddleware implements Middleware {
    private final CooldownManager cooldownManager;

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        long cooldownMillis = action.getMetadata().getCooldownMillis();
        if (cooldownMillis <= 0) return;

        ActionResult result = ActionPipeline.getResultFromContext(context);
        if (result == null || !result.isSuccess()) {
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Cooldown not applied for " + action.getMetadata().getFullId() +
                    " - action failed or no result");
            return;
        }

        UUID playerId = context.getPlayer().getUniqueId();
        String actionId = action.getMetadata().getFullId();

        cooldownManager.setCooldown(playerId, actionId, cooldownMillis);
        DebugAPI.logLibDebug(DebugCategory.ACTION, "Cooldown applied for " + actionId +
                " - player: " + context.getPlayer().getName() + ", duration: " + cooldownMillis + "ms");
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
