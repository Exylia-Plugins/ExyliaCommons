package net.exylia.commons.item.exceptions;

import java.util.UUID;

/**
 * Excepción para errores relacionados con cooldowns
 */
public class ItemCooldownException extends ItemException {

    private final UUID playerId;
    private final String itemId;
    private final double cooldownSeconds;

    public ItemCooldownException(UUID playerId, String itemId, String message) {
        super(String.format("Cooldown error for player %s with item '%s': %s", playerId, itemId, message));
        this.playerId = playerId;
        this.itemId = itemId;
        this.cooldownSeconds = 0.0;
    }

    public ItemCooldownException(UUID playerId, String itemId, double cooldownSeconds, String message) {
        super(String.format("Cooldown error for player %s with item '%s' (%.1fs): %s", playerId, itemId, cooldownSeconds, message));
        this.playerId = playerId;
        this.itemId = itemId;
        this.cooldownSeconds = cooldownSeconds;
    }

    public ItemCooldownException(UUID playerId, String itemId, String message, Throwable cause) {
        super(String.format("Cooldown error for player %s with item '%s': %s", playerId, itemId, message), cause);
        this.playerId = playerId;
        this.itemId = itemId;
        this.cooldownSeconds = 0.0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getItemId() {
        return itemId;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }
}