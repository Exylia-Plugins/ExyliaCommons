package net.exylia.commons.v2.action.cooldown;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class Cooldown {
    private final UUID playerId;
    private final String actionId;
    private final long expiryTime;

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public long getRemainingMillis() {
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
