package net.exylia.commons.v2.action.pipeline;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.cooldown.CooldownManager;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.util.UUID;

@RequiredArgsConstructor
public class CooldownMiddleware implements Middleware {
    private final CooldownManager cooldownManager;

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        long cooldownMillis = action.getMetadata().getCooldownMillis();

        if (cooldownMillis > 0) {
            UUID playerId = context.getPlayer().getUniqueId();
            String actionId = action.getMetadata().getFullId();

            if (cooldownManager.isOnCooldown(playerId, actionId)) {
                long remaining = cooldownManager.getRemainingMillis(playerId, actionId);
                DebugAPI.logLibDebug(DebugCategory.ACTION, "Cooldown check failed for " + actionId +
                        " - player: " + context.getPlayer().getName() + ", remaining: " + remaining + "ms");
                throw new ActionException.ActionCooldownException(actionId, remaining);
            }

            cooldownManager.setCooldown(playerId, actionId, cooldownMillis);
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Cooldown set for " + actionId +
                    " - player: " + context.getPlayer().getName() + ", duration: " + cooldownMillis + "ms");
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
