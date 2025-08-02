package net.exylia.commons.item.config;

public enum TriggerType {
    IMMEDIATE,         // Actions execute immediately on interact
    AFTER_CONSUME,     // Actions execute after consuming the item
    ON_HIT_PLAYER,     // Actions execute when hitting a player
    ON_PROJECTILE_LAUNCH, // Actions execute when launching a projectile
    ON_PROJECTILE_HIT; // Actions execute when projectile hits something

    public static TriggerType fromString(String value) {
        if (value == null) return IMMEDIATE;
        try {
            return TriggerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IMMEDIATE;
        }
    }
}