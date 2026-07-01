package net.exylia.commons.v2.items.region;

public enum RegionFilterType {

    WHITELIST,
    BLACKLIST,
    NONE;

    public static RegionFilterType fromString(String value) {
        if (value == null) return NONE;
        return switch (value.toUpperCase()) {
            case "WHITELIST" -> WHITELIST;
            case "BLACKLIST" -> BLACKLIST;
            default -> NONE;
        };
    }
}
