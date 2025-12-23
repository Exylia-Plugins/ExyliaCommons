package net.exylia.commons.v2.debug.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebugLevel {
    DISABLED(0, "Disabled"),
    PLUGIN_ONLY(1, "Plugin Only"),
    LIBRARY_ONLY(2, "Library Only"),
    ALL(3, "All");

    private final int level;
    private final String displayName;

    public static DebugLevel fromLevel(int level) {
        for (DebugLevel debugLevel : values()) {
            if (debugLevel.level == level) {
                return debugLevel;
            }
        }
        return DISABLED;
    }
}
