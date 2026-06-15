package net.exylia.commons.v2.cooldown.model;

import lombok.Getter;
import org.bukkit.Material;

import java.util.UUID;

@Getter
public final class ItemCooldown {

    private final String id;
    private final UUID playerId;
    private final long startTimeMs;
    private final long durationMs;
    private final Material material;

    public ItemCooldown(String id, UUID playerId, long durationMs, Material material) {
        this.id = id;
        this.playerId = playerId;
        this.startTimeMs = System.currentTimeMillis();
        this.durationMs = durationMs;
        this.material = material;
    }

    ItemCooldown(String id, UUID playerId, long startTimeMs, long durationMs, Material material) {
        this.id = id;
        this.playerId = playerId;
        this.startTimeMs = startTimeMs;
        this.durationMs = durationMs;
        this.material = material;
    }

    public static ItemCooldown fromStorage(String id, UUID playerId, long expiryTimeMs, long durationMs, Material material) {
        return new ItemCooldown(id, playerId, expiryTimeMs - durationMs, durationMs, material);
    }

    public long getExpiryTimeMs() {
        return startTimeMs + durationMs;
    }

    public long getRemainingMillis() {
        return Math.max(0, getExpiryTimeMs() - System.currentTimeMillis());
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= getExpiryTimeMs();
    }

    public double getProgressDecimal() {
        long remaining = getRemainingMillis();
        if (durationMs <= 0) return 0.0;
        return (double) remaining / durationMs;
    }

    public boolean hasMaterial() {
        return material != null;
    }
}
