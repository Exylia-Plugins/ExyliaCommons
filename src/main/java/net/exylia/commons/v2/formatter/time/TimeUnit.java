package net.exylia.commons.v2.formatter.time;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimeUnit {
    MILLISECOND(1L, "ms", "millisecond", "milliseconds"),
    SECOND(1000L, "s", "second", "seconds"),
    MINUTE(60000L, "m", "minute", "minutes"),
    HOUR(3600000L, "h", "hour", "hours"),
    DAY(86400000L, "d", "day", "days"),
    WEEK(604800000L, "w", "week", "weeks"),
    MONTH(2592000000L, "mo", "month", "months"),
    YEAR(31536000000L, "y", "year", "years");

    private final long millis;
    private final String symbol;
    private final String singularName;
    private final String pluralName;

    public String getName(long value) {
        return value == 1 ? singularName : pluralName;
    }

    public static TimeUnit fromSymbol(String symbol) {
        for (TimeUnit unit : values()) {
            if (unit.symbol.equalsIgnoreCase(symbol)) {
                return unit;
            }
        }
        return null;
    }
}
