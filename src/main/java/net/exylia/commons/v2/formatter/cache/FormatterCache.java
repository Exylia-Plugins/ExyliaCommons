package net.exylia.commons.v2.formatter.cache;

import lombok.Getter;
import net.exylia.commons.cache.CaffeineCache;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Getter
public class FormatterCache {
    private static volatile FormatterCache instance;

    private final CaffeineCache<String, Pattern> patternCache;
    private final CaffeineCache<String, DateTimeFormatter> dateTimeFormatterCache;
    private final CaffeineCache<DecimalFormatKey, DecimalFormat> decimalFormatCache;
    private final CaffeineCache<String, String> resultCache;
    private final CaffeineCache<String, Object> configCache;

    private FormatterCache() {
        this.patternCache = CaffeineCache.<String, Pattern>builder()
            .maximumSize(50)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

        this.dateTimeFormatterCache = CaffeineCache.<String, DateTimeFormatter>builder()
            .maximumSize(30)
            .recordStats()
            .build();

        this.decimalFormatCache = CaffeineCache.<DecimalFormatKey, DecimalFormat>builder()
            .maximumSize(20)
            .recordStats()
            .weakValues()
            .build();

        this.resultCache = CaffeineCache.<String, String>builder()
            .maximumSize(2000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats()
            .build();

        this.configCache = CaffeineCache.<String, Object>builder()
            .maximumSize(10)
            .recordStats()
            .build();
    }

    public static FormatterCache getInstance() {
        if (instance == null) {
            synchronized (FormatterCache.class) {
                if (instance == null) {
                    instance = new FormatterCache();
                }
            }
        }
        return instance;
    }

    public Pattern getPattern(String regex) {
        return patternCache.get(regex, Pattern::compile);
    }

    public DateTimeFormatter getDateTimeFormatter(String pattern) {
        return dateTimeFormatterCache.get(pattern, DateTimeFormatter::ofPattern);
    }

    public DecimalFormat getDecimalFormat(int precision, boolean forceZero) {
        return getDecimalFormat(precision, forceZero, ".", ",");
    }

    public DecimalFormat getDecimalFormat(int precision, boolean forceZero,
                                          String decimalSeparator, String thousandSeparator) {
        DecimalFormatKey key = new DecimalFormatKey(precision, forceZero, decimalSeparator, thousandSeparator);
        return decimalFormatCache.get(key, k -> {
            String pattern = forceZero
                ? "0." + "0".repeat(precision)
                : "0." + "#".repeat(precision);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator(decimalSeparator.charAt(0));
            symbols.setGroupingSeparator(thousandSeparator.charAt(0));

            return new DecimalFormat(pattern, symbols);
        });
    }

    public String getResult(String key, Supplier<String> loader) {
        return resultCache.get(key, k -> loader.get());
    }

    public String getResult(String key, Supplier<String> loader, long ttl, TimeUnit unit) {
        String cached = resultCache.get(key);
        if (cached != null) {
            return cached;
        }
        String result = loader.get();
        resultCache.put(key, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String key, Supplier<T> loader) {
        return (T) configCache.get(key, k -> loader.get());
    }

    public void invalidateResults() {
        resultCache.invalidateAll();
    }

    public void invalidateConfigs() {
        configCache.invalidateAll();
    }

    public void invalidateAll() {
        patternCache.invalidateAll();
        dateTimeFormatterCache.invalidateAll();
        decimalFormatCache.invalidateAll();
        resultCache.invalidateAll();
        configCache.invalidateAll();
    }

    public FormatterCacheStats getStats() {
        return new FormatterCacheStats(
            patternCache.getStats(),
            dateTimeFormatterCache.getStats(),
            decimalFormatCache.getStats(),
            resultCache.getStats(),
            configCache.getStats()
        );
    }
}
