package net.exylia.commons.v2.formatter.core;

import net.exylia.commons.v2.config.FormattersConfig;
import net.exylia.commons.v2.formatter.cache.FormatterCache;
import net.exylia.commons.v2.formatter.date.DateFormatterV2;
import net.exylia.commons.v2.formatter.price.PriceFormatterV2;
import net.exylia.commons.v2.formatter.time.TimeFormatterV2;

public class FormatterRegistry {
    private static volatile TimeFormatterV2 timeFormatter;
    private static volatile DateFormatterV2 dateFormatter;
    private static volatile PriceFormatterV2 priceFormatter;
    private static final Object LOCK = new Object();

    public static void initialize() {
        synchronized (LOCK) {
            FormattersConfig.ensureDefaults();
            FormatterCache cache = FormatterCache.getInstance();
            FormattersConfig formattersConfig = FormattersConfig.getInstance();

            timeFormatter = TimeFormatterV2.builder()
                .cache(cache)
                .config(formattersConfig.getTimeConfig())
                .build();

            dateFormatter = DateFormatterV2.builder()
                .cache(cache)
                .config(formattersConfig.getDateConfig())
                .build();

            priceFormatter = PriceFormatterV2.builder()
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

    public static TimeFormatterV2 getTimeFormatter() {
        if (timeFormatter == null) {
            initialize();
        }
        return timeFormatter;
    }

    public static DateFormatterV2 getDateFormatter() {
        if (dateFormatter == null) {
            initialize();
        }
        return dateFormatter;
    }

    public static PriceFormatterV2 getPriceFormatter() {
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
