package net.exylia.commons.v2.effect.model;

public enum EffectType {
    PARTICLE,
    SOUND,
    POTION,
    FIREWORK,
    TITLE,
    ACTIONBAR,
    MESSAGE,
    SEQUENCE;

    public static EffectType fromName(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
