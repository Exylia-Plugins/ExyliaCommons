package net.exylia.commons.v2.ui.refresh;

public enum RefreshMode {
    DISABLED,
    FULL,
    SMART,
    SLOT_ONLY;

    public static RefreshMode fromString(String mode) {
        if (mode == null) {
            return DISABLED;
        }

        try {
            return RefreshMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DISABLED;
        }
    }
}
