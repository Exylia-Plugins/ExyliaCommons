package net.exylia.commons.v2.formatter.time;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClockFormat {
    AUTO("00:00"),
    HH_MM_SS("00:00:00"),
    MM_SS("00:00"),
    SS("00");

    private final String defaultZero;

    public static ClockFormat detect(long millis) {
        if (millis >= 3600000) {
            return HH_MM_SS;
        } else if (millis >= 60000) {
            return MM_SS;
        } else {
            return SS;
        }
    }
}
