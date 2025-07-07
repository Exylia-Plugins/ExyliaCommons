package net.exylia.commons.region.model;

import lombok.Getter;

/**
 * Prioridad de las regiones para resolver conflictos
 */
@Getter
public enum RegionPriority {
    LOWEST(0, "Más baja"),
    LOW(1, "Baja"),
    NORMAL(2, "Normal"),
    HIGH(3, "Alta"),
    HIGHEST(4, "Más alta"),
    CRITICAL(5, "Crítica");

    private final int level;
    private final String displayName;

    RegionPriority(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public boolean isHigherThan(RegionPriority other) {
        return this.level > other.level;
    }

    public boolean isLowerThan(RegionPriority other) {
        return this.level < other.level;
    }
}