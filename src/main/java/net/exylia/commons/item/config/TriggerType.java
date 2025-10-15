package net.exylia.commons.item.config;

public enum TriggerType {
    IMMEDIATE,          
    AFTER_CONSUME,      
    ON_HIT_PLAYER,      
    ON_MULTIPLE_HIT_PLAYER,  
    ON_PROJECTILE_LAUNCH,  
    ON_PROJECTILE_HIT,  
    RADIUS,             
    HOLD;               

    public static TriggerType fromString(String value) {
        if (value == null) return IMMEDIATE;
        try {
            return TriggerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IMMEDIATE;
        }
    }

    public boolean supportsRadius() {
        return this == RADIUS || this == ON_HIT_PLAYER || this == ON_MULTIPLE_HIT_PLAYER || this == ON_PROJECTILE_HIT || this == IMMEDIATE || this == AFTER_CONSUME || this == ON_PROJECTILE_LAUNCH || this == HOLD;
    }

    public boolean requiresTargetPlayer() {
        return this == ON_HIT_PLAYER || this == ON_MULTIPLE_HIT_PLAYER || this == ON_PROJECTILE_HIT;
    }

    public boolean canAffectMultipleEntities() {
        return this == RADIUS || this == IMMEDIATE || this == AFTER_CONSUME || this == ON_PROJECTILE_LAUNCH || this == HOLD;
    }

    public boolean shouldAffectSelfByDefault() {
        return this == IMMEDIATE || this == AFTER_CONSUME || this == ON_PROJECTILE_LAUNCH || this == HOLD;
    }
}
