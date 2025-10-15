package net.exylia.commons.item.config;

public enum WorldFilterType {
     
    NONE,

    WHITELIST,

    BLACKLIST;

    public static WorldFilterType fromString(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return NONE;
        }

        String normalized = typeString.trim().toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            switch (normalized) {
                case "WHITE":
                case "ALLOW":
                case "ALLOWED":
                    return WHITELIST;
                case "BLACK":
                case "DENY":
                case "DENIED":
                case "BLOCK":
                case "BLOCKED":
                    return BLACKLIST;
                case "OFF":
                case "DISABLED":
                case "DISABLE":
                    return NONE;
                default:
                    return NONE;
            }
        }
    }
}
