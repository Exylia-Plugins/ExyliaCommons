package net.exylia.commons.v2.region.model;

import lombok.Getter;

@Getter
public enum RegionFlagState {
    ALLOW(true),
    DENY(false),
    DEFAULT(null);

    private final Boolean value;

    RegionFlagState(Boolean value) {
        this.value = value;
    }

    public boolean getEffectiveValue(RegionFlag flag) {
        if (this == DEFAULT) {
            return flag.isDefaultValue();
        }
        return value;
    }

    public static RegionFlagState fromBoolean(boolean value) {
        return value ? ALLOW : DENY;
    }
}
