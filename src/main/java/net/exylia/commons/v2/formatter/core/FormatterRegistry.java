package net.exylia.commons.v2.formatter.core;

import net.exylia.commons.v2.config.FormattersConfig;
import net.exylia.commons.v2.formatter.cache.FormatterCache;
import net.exylia.commons.v2.formatter.date.DateFormatter;
import net.exylia.commons.v2.formatter.price.PriceFormatter;
import net.exylia.commons.v2.formatter.time.TimeFormatter;

public class FormatterRegistry {
    private static volatile TimeFormatter timeFormatter;
    private static volatile DateFormatter dateFormatter;
    private static volatile PriceFormatter priceFormatter;
    private static final Object LOCK = new Object();

    public static void initialize() {
        synchronized (LOCK) {
            FormatterCache cache = FormatterCache.getInstance();
            FormattersConfig formattersConfig = FormattersConfig.getInstance();

            timeFormatter = TimeFormatter.builder()
                .cache(cache)
                .config(formattersConfig.getTimeConfig())
                .build();

            dateFormatter = DateFormatter.builder()
                .cache(cache)
                .config(formattersConfig.getDateConfig())
                .build();

            priceFormatter = PriceFormatter.builder()
                .cache(cache)
                .config(formattersConfig.getPriceConfig())
                .build();
        }
    }

    public static void reload() {
        synchronized (LOCK) {
            FormattersConfig.reload();
            FormatterCache cache = FormatterCache.getInstance();
            cache.invalidateConfigs();
            cache.invalidateResults();
            initialize();
        }
    }

    public static TimeFormatter getTimeFormatter() {
        if (timeFormatter == null) {
            initialize();
        }
        return timeFormatter;
    }

    public static DateFormatter getDateFormatter() {
        if (dateFormatter == null) {
            initialize();
        }
        return dateFormatter;
    }

    public static PriceFormatter getPriceFormatter() {
        if (priceFormatter == null) {
            initialize();
        }
        return priceFormatter;
    }

    public static void shutdown() {
        synchronized (LOCK) {
            timeFormatter = null;
            dateFormatter = null;
            priceFormatter = null;
        }
    }
}
