package net.exylia.commons.v2.ui.animation;

public enum AnimationType {
    NONE,
    SLIDE_LEFT,
    SLIDE_TOP,
    CASCADE,
    CENTER_OUT,
    RANDOM,
    SPIRAL,
    SPIRAL_OUT,
    CHECKERBOARD,
    WAVE_HORIZONTAL,
    WAVE_VERTICAL,
    CORNERS,
    SNAKE,
    ROWS_ALTERNATE,
    COLUMNS_ALTERNATE,
    EXPLOSION,
    TYPEWRITER;

    public static AnimationType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return NONE;
        }
        try {
            return valueOf(value.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
