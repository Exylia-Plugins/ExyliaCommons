package net.exylia.commons.v2.action.pipeline;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.ratelimit.RateLimiter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.util.UUID;

@RequiredArgsConstructor
public class RateLimitMiddleware implements Middleware {
    private final RateLimiter rateLimiter;

    @Override
    public void execute(Action action, ActionContext context) throws ActionException {
        int rateLimit = action.getMetadata().getRateLimit();

        if (rateLimit > 0) {
            UUID playerId = context.getPlayer().getUniqueId();
            String actionId = action.getMetadata().getFullId();

            if (!rateLimiter.allowRequest(playerId, actionId, rateLimit)) {
                DebugAPI.logLibDebug(DebugCategory.ACTION, "Rate limit exceeded for " + actionId +
                        " - player: " + context.getPlayer().getName() + ", limit: " + rateLimit + "/s");
                throw new ActionException.ActionRateLimitException(actionId);
            }
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Rate limit check passed for " + actionId +
                    " - player: " + context.getPlayer().getName());
        }
    }

    @Override
    public int getPriority() {
        return 60;
    }
}
