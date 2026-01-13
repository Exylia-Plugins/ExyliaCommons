package net.exylia.commons.v2.formatter.date;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.formatter.FormattersDefaults;

@Getter
@Builder
public class DateFormatterConfig {
    private final String defaultPattern;
    private final String datePattern;
    private final String timePattern;
    private final boolean useIsoDefault;

    public static DateFormatterConfig fromConfig() {
        return DateFormatterConfig.builder()
            .defaultPattern(FormattersDefaults.Formatters.Date.DEFAULT_PATTERN)
            .datePattern(FormattersDefaults.Formatters.Date.DATE_PATTERN)
            .timePattern(FormattersDefaults.Formatters.Date.TIME_PATTERN)
            .useIsoDefault(FormattersDefaults.Formatters.Date.USE_ISO_DEFAULT)
            .build();
    }

    public static DateFormatterConfig defaults() {
        return DateFormatterConfig.builder()
            .defaultPattern("dd/MM/yyyy HH:mm:ss")
            .datePattern("dd/MM/yyyy")
            .timePattern("HH:mm:ss")
            .useIsoDefault(false)
            .build();
    }
}
