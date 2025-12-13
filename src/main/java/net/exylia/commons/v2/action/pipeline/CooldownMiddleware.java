package net.exylia.commons.v2.action.pipeline;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.cooldown.CooldownManager;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;

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
                throw new ActionException.ActionCooldownException(actionId, remaining);
            }

            cooldownManager.setCooldown(playerId, actionId, cooldownMillis);
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
