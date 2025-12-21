package net.exylia.commons.v2.ui.model;

import lombok.Getter;

@Getter
public enum RefreshMode {
    DISABLED("disabled", "No refresh"),
    SLOT_ONLY("slot_only", "Only refreshes the clicked slot"),
    SMART("smart", "Refreshes clicked slot and dynamic items"),
    FULL("full", "Refreshes entire menu");

    private final String configName;
    private final String description;

    RefreshMode(String configName, String description) {
        this.configName = configName;
        this.description = description;
    }

    public static RefreshMode fromString(String name) {
        for (RefreshMode mode : values()) {
            if (mode.configName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return SMART;
    }
}
