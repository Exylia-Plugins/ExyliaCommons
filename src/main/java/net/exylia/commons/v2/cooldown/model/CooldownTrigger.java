package net.exylia.commons.v2.cooldown.model;

public enum CooldownTrigger {
    USE,
    USE_ON_ELYTRA,
    ELYTRA_BOOST,
    CONSUME,
    LAUNCH,
    RESURRECT;

    public static CooldownTrigger fromString(String value) {
        if (value == null) return USE;
        try {
            return valueOf(value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return USE;
        }
    }
}
