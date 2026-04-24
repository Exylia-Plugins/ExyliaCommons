package net.exylia.commons.v2.items.region;

public enum RegionFilterChecker {

    CONTAINS,
    PRIORITY;

    public static RegionFilterChecker fromString(String value) {
        if (value == null) return CONTAINS;
        return switch (value.toUpperCase()) {
            case "PRIORITY" -> PRIORITY;
            default -> CONTAINS;
        };
    }
}
