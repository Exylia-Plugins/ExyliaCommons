package net.exylia.commons.v2.formatter.date;

import net.exylia.commons.v2.formatter.cache.FormatterCache;

public class DateFormatterBuilder {
    private FormatterCache cache;
    private DateFormatterConfig config;
    private String defaultPattern;
    private String datePattern;
    private String timePattern;
    private Boolean useIsoDefault;

    public DateFormatterBuilder cache(FormatterCache cache) {
        this.cache = cache;
        return this;
    }

    public DateFormatterBuilder config(DateFormatterConfig config) {
        this.config = config;
        return this;
    }

    public DateFormatterBuilder defaultPattern(String defaultPattern) {
        this.defaultPattern = defaultPattern;
        return this;
    }

    public DateFormatterBuilder datePattern(String datePattern) {
        this.datePattern = datePattern;
        return this;
    }

    public DateFormatterBuilder timePattern(String timePattern) {
        this.timePattern = timePattern;
        return this;
    }

    public DateFormatterBuilder useIsoDefault(boolean useIsoDefault) {
        this.useIsoDefault = useIsoDefault;
        return this;
    }

    public DateFormatter build() {
        if (cache == null) {
            cache = FormatterCache.getInstance();
        }

        if (config == null) {
            config = DateFormatterConfig.builder()
                .defaultPattern(defaultPattern != null ? defaultPattern : "dd/MM/yyyy HH:mm:ss")
                .datePattern(datePattern != null ? datePattern : "dd/MM/yyyy")
                .timePattern(timePattern != null ? timePattern : "HH:mm:ss")
                .useIsoDefault(useIsoDefault != null ? useIsoDefault : false)
                .build();
        }

        return new DateFormatter(cache, config);
    }
}
