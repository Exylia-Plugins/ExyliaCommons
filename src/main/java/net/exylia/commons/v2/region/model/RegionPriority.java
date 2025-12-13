package net.exylia.commons.v2.region.model;

import lombok.Getter;

@Getter
public enum RegionPriority {
    LOW(0),
    NORMAL(1),
    HIGH(2);

    private final int level;

    RegionPriority(int level) {
        this.level = level;
    }

    public boolean isHigherThan(RegionPriority other) {
        return this.level > other.level;
    }

    public boolean isLowerThan(RegionPriority other) {
        return this.level < other.level;
    }
}
