package net.exylia.commons.item;

import lombok.Getter;

@Getter
public enum ExpirationBehavior {
    KEEP("keep"),
    REMOVE("remove"),
    DISABLE("disable"),
    TRANSFORM("transform");
    
    private final String configName;
    
    ExpirationBehavior(String configName) {
        this.configName = configName;
    }

    public static ExpirationBehavior fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return KEEP;
        }
        
        String normalized = str.toLowerCase().trim();
        for (ExpirationBehavior behavior : values()) {
            if (behavior.configName.equals(normalized) || behavior.name().toLowerCase().equals(normalized)) {
                return behavior;
            }
        }
        
        return KEEP;
    }
    
    @Override
    public String toString() {
        return configName;
    }
}
