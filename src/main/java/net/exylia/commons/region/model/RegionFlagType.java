package net.exylia.commons.region.model;

import lombok.Getter;

@Getter
public enum RegionFlagType {
    ALLOW("allow", "Permite la acción", true),
    DENY("deny", "Deniega la acción", false),
    DEFAULT("default", "Usa el valor por defecto", null);

    private final String key;
    private final String description;
    private final Boolean value;  

    RegionFlagType(String key, String description, Boolean value) {
        this.key = key;
        this.description = description;
        this.value = value;
    }

    public boolean getEffectiveValue(RegionFlag flag) {
        if (value != null) {
            return value;
        }
        return flag.isDefaultValue();
    }

    public static RegionFlagType fromKey(String key) {
        for (RegionFlagType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return DEFAULT;
    }

    @Override
    public String toString() {
        return key;
    }
}
