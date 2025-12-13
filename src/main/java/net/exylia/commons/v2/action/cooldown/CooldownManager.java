package net.exylia.commons.v2.action.cooldown;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.action.cache.CooldownCache;

import java.util.UUID;

@RequiredArgsConstructor
public class CooldownManager {
    private final CooldownCache cooldownCache;

    public void setCooldown(UUID playerId, String actionId, long durationMillis) {
        cooldownCache.setCooldown(playerId, actionId, durationMillis);
    }

    public boolean isOnCooldown(UUID playerId, String actionId) {
        return cooldownCache.isOnCooldown(playerId, actionId);
    }

    public long getRemainingMillis(UUID playerId, String actionId) {
        return cooldownCache.getRemainingMillis(playerId, actionId);
    }

    public void removeCooldown(UUID playerId, String actionId) {
        cooldownCache.invalidate(playerId, actionId);
    }

    public void clearAll() {
        cooldownCache.invalidateAll();
    }
}
