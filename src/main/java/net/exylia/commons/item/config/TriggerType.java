package net.exylia.commons.item.config;

public enum TriggerType {
    IMMEDIATE,         // Actions execute immediately on interact
    AFTER_CONSUME,     // Actions execute after consuming the item
    ON_HIT_PLAYER,     // Actions execute when hitting a player
    ON_PROJECTILE_LAUNCH, // Actions execute when launching a projectile
    ON_PROJECTILE_HIT, // Actions execute when projectile hits something
    RADIUS;            // Actions execute on all entities within radius

    public static TriggerType fromString(String value) {
        if (value == null) return IMMEDIATE;
        try {
            return TriggerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IMMEDIATE;
        }
    }

    /**
     * Checks if this trigger type supports radius-based actions
     */
    public boolean supportsRadius() {
        return this == RADIUS || this == ON_HIT_PLAYER || this == ON_PROJECTILE_HIT || this == IMMEDIATE || this == AFTER_CONSUME || this == ON_PROJECTILE_LAUNCH;
    }

    /**
     * Checks if this trigger type requires a target player
     */
    public boolean requiresTargetPlayer() {
        return this == ON_HIT_PLAYER || this == ON_PROJECTILE_HIT;
    }

    /**
     * Checks if this trigger type can affect multiple entities
     */
    public boolean canAffectMultipleEntities() {
        return this == RADIUS || this == IMMEDIATE || this == AFTER_CONSUME || this == ON_PROJECTILE_LAUNCH;
    }

    /**
     * Checks if this trigger type should affect self by default when no radius is configured
     */
    public boolean shouldAffectSelfByDefault() {
        return this == IMMEDIATE || this == AFTER_CONSUME || this == ON_PROJECTILE_LAUNCH;
    }
}