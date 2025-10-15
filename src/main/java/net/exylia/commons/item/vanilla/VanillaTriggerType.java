package net.exylia.commons.item.vanilla;

public enum VanillaTriggerType {

    INTERACT,

    AFTER_CONSUME,

    AFTER_PROJECTILE,

    AUTO_DETECT;

    public static VanillaTriggerType fromString(String triggerString) {
        if (triggerString == null || triggerString.trim().isEmpty()) {
            return AUTO_DETECT;
        }

        String normalized = triggerString.toUpperCase().replace("-", "_");

        try {
            return VanillaTriggerType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
             
            return switch (normalized) {
                case "IMMEDIATE", "CLICK", "USE" -> INTERACT;
                case "CONSUME", "EAT", "DRINK" -> AFTER_CONSUME;
                case "PROJECTILE", "LAUNCH", "THROW", "SHOOT" -> AFTER_PROJECTILE;
                default -> AUTO_DETECT;
            };
        }
    }

    public boolean shouldCancelInteract() {
        return this != AUTO_DETECT;  
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }
}
