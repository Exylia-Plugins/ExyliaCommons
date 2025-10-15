package net.exylia.commons.item.config;

public enum RegionFilterType {

    NONE,

    WHITELIST,

    BLACKLIST;

    public static RegionFilterType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }

        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    public boolean requiresRegionList() {
        return this == WHITELIST || this == BLACKLIST;
    }

    public String getDescription() {
        return switch (this) {
            case NONE -> "Sin restricciones de región";
            case WHITELIST -> "Solo permitido en regiones específicas";
            case BLACKLIST -> "Prohibido en regiones específicas";
        };
    }
}
