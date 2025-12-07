package net.exylia.commons.v2.formatter.date;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.config.Configs;

@Getter
@Builder
public class DateFormatterConfig {
    private final String defaultPattern;
    private final String datePattern;
    private final String timePattern;
    private final boolean useIsoDefault;

    public static DateFormatterConfig fromConfig() {
        return DateFormatterConfig.builder()
            .defaultPattern(Configs.string("formatters.date.default-pattern", "dd/MM/yyyy HH:mm:ss"))
            .datePattern(Configs.string("formatters.date.date-pattern", "dd/MM/yyyy"))
            .timePattern(Configs.string("formatters.date.time-pattern", "HH:mm:ss"))
            .useIsoDefault(Configs.bool("formatters.date.use-iso-default", false))
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
