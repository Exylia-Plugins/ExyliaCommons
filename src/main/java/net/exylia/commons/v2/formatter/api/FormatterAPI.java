package net.exylia.commons.v2.formatter.api;

import net.exylia.commons.v2.formatter.cache.FormatterCache;
import net.exylia.commons.v2.formatter.cache.FormatterCacheStats;
import net.exylia.commons.v2.formatter.core.FormatterRegistry;
import net.exylia.commons.v2.formatter.date.DateFormatter;
import net.exylia.commons.v2.formatter.percent.PercentFormatter;
import net.exylia.commons.v2.formatter.price.PriceFormatter;
import net.exylia.commons.v2.formatter.time.ClockFormat;
import net.exylia.commons.v2.formatter.time.TimeComponents;
import net.exylia.commons.v2.formatter.time.TimeFormatter;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class FormatterAPI {

    private FormatterAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String formatTime(Object input) {
        return FormatterRegistry.getTimeFormatter().format(input);
    }

    public static String formatTimeClock(Object input) {
        return FormatterRegistry.getTimeFormatter().formatClock(input);
    }

    public static String formatTimeClock(Object input, ClockFormat format) {
        return FormatterRegistry.getTimeFormatter().formatClock(input, format);
    }

    public static String formatTimeCompact(Object input) {
        return FormatterRegistry.getTimeFormatter().formatCompact(input);
    }

    public static String formatTimeVerbal(Object input) {
        return FormatterRegistry.getTimeFormatter().formatVerbal(input);
    }

    public static String formatTimeLargest(Object input) {
        return FormatterRegistry.getTimeFormatter().formatLargestUnit(input);
    }

    public static String formatTimeApproximate(Object input) {
        return FormatterRegistry.getTimeFormatter().formatApproximate(input);
    }

    public static String formatTimeWithPrecision(Object input, int precision) {
        return FormatterRegistry.getTimeFormatter().formatWithPrecision(input, precision);
    }

    public static TimeComponents getTimeComponents(Object input) {
        return FormatterRegistry.getTimeFormatter().getComponents(input);
    }

    public static String formatDate(Object input) {
        return FormatterRegistry.getDateFormatter().format(input);
    }

    public static String formatDate(Object input, String pattern) {
        return FormatterRegistry.getDateFormatter().format(input, pattern);
    }

    public static String formatDateOnly(Object input) {
        return FormatterRegistry.getDateFormatter().formatDate(input);
    }

    public static String formatTimeOnly(Object input) {
        return FormatterRegistry.getDateFormatter().formatTime(input);
    }

    public static String formatDateISO(Object input) {
        return FormatterRegistry.getDateFormatter().formatISO(input);
    }

    public static String formatDateRelative(Object input) {
        return FormatterRegistry.getDateFormatter().formatRelative(input);
    }

    public static String formatDateRelativeFrom(Object input, Object fromDate) {
        return FormatterRegistry.getDateFormatter().formatRelativeFrom(input, fromDate);
    }

    public static String formatDateRelativeCompact(Object input) {
        return FormatterRegistry.getDateFormatter().formatRelativeCompact(input);
    }

    public static String formatDateRelativeVerbose(Object input) {
        return FormatterRegistry.getDateFormatter().formatRelativeVerbose(input);
    }

    public static long getDateDifference(Object input1, Object input2, ChronoUnit unit) {
        return FormatterRegistry.getDateFormatter().getDifference(input1, input2, unit);
    }

    public static String getDateDifferenceFormatted(Object input1, Object input2) {
        return FormatterRegistry.getDateFormatter().getDifferenceFormatted(input1, input2);
    }

    public static boolean isDatePast(Object input) {
        return FormatterRegistry.getDateFormatter().isPast(input);
    }

    public static boolean isDateFuture(Object input) {
        return FormatterRegistry.getDateFormatter().isFuture(input);
    }

    public static boolean isDateToday(Object input) {
        return FormatterRegistry.getDateFormatter().isToday(input);
    }

    public static boolean isDateWithin(Object input, long millis) {
        return FormatterRegistry.getDateFormatter().isWithin(input, millis);
    }

    public static String formatPrice(Object input) {
        return FormatterRegistry.getPriceFormatter().format(input);
    }

    public static String formatPriceCompact(Object input) {
        return FormatterRegistry.getPriceFormatter().formatCompact(input);
    }

    public static String formatPriceNoSymbol(Object input) {
        return FormatterRegistry.getPriceFormatter().formatNoSymbol(input);
    }

    public static String formatPriceWithSymbol(Object input, String symbol) {
        return FormatterRegistry.getPriceFormatter().formatWithSymbol(input, symbol);
    }

    public static String formatPercent(Object input) {
        return FormatterRegistry.getPercentFormatter().format(input);
    }

    public static String formatPercentWithDecimals(Object input, int decimals) {
        return FormatterRegistry.getPercentFormatter().formatWithDecimals(input, decimals);
    }

    public static String formatPercentNoSuffix(Object input) {
        return FormatterRegistry.getPercentFormatter().formatNoSuffix(input);
    }

    public static String formatPercentWithSuffix(Object input, String suffix) {
        return FormatterRegistry.getPercentFormatter().formatWithSuffix(input, suffix);
    }

    public static String formatPercentRatio(Object numerator, Object denominator) {
        return FormatterRegistry.getPercentFormatter().formatRatio(numerator, denominator);
    }

    public static CompletableFuture<List<String>> formatTimeBatchAsync(List<Object> inputs) {
        return AsyncFormatterAPI.getInstance().formatTimeBatch(inputs);
    }

    public static CompletableFuture<List<String>> formatDateBatchAsync(List<Object> inputs) {
        return AsyncFormatterAPI.getInstance().formatDateBatch(inputs);
    }

    public static CompletableFuture<List<String>> formatPriceBatchAsync(List<Object> inputs) {
        return AsyncFormatterAPI.getInstance().formatPriceBatch(inputs);
    }

    public static void reload() {
        FormatterRegistry.reload();
    }

    public static void invalidateCaches() {
        FormatterCache.getInstance().invalidateResults();
    }

    public static void invalidateAllCaches() {
        FormatterCache.getInstance().invalidateAll();
    }

    public static FormatterCacheStats getCacheStats() {
        return FormatterCache.getInstance().getStats();
    }

    public static GlobalFormatterStats getGlobalStats() {
        TimeFormatter timeFormatter = FormatterRegistry.getTimeFormatter();
        DateFormatter dateFormatter = FormatterRegistry.getDateFormatter();
        PriceFormatter priceFormatter = FormatterRegistry.getPriceFormatter();
        PercentFormatter percentFormatter = FormatterRegistry.getPercentFormatter();

        return new GlobalFormatterStats(
            FormatterCache.getInstance().getStats(),
            timeFormatter.getStats(),
            dateFormatter.getStats(),
            priceFormatter.getStats(),
            percentFormatter.getStats()
        );
    }

    public static class GlobalFormatterStats {
        private final FormatterCacheStats cacheStats;
        private final FormatterStats timeStats;
        private final FormatterStats dateStats;
        private final FormatterStats priceStats;
        private final FormatterStats percentStats;

        public GlobalFormatterStats(FormatterCacheStats cacheStats, FormatterStats timeStats,
                                   FormatterStats dateStats, FormatterStats priceStats,
                                   FormatterStats percentStats) {
            this.cacheStats = cacheStats;
            this.timeStats = timeStats;
            this.dateStats = dateStats;
            this.priceStats = priceStats;
            this.percentStats = percentStats;
        }

        public FormatterCacheStats getCacheStats() {
            return cacheStats;
        }

        public FormatterStats getTimeStats() {
            return timeStats;
        }

        public FormatterStats getDateStats() {
            return dateStats;
        }

        public FormatterStats getPriceStats() {
            return priceStats;
        }

        public FormatterStats getPercentStats() {
            return percentStats;
        }

        public double getOverallCacheHitRate() {
            return cacheStats.getOverallHitRate();
        }

        public long getTotalFormatCount() {
            return timeStats.getFormatCount().get() +
                   dateStats.getFormatCount().get() +
                   priceStats.getFormatCount().get() +
                   percentStats.getFormatCount().get();
        }

        public double getAverageTimeMillis() {
            long totalCount = getTotalFormatCount();
            if (totalCount == 0) return 0.0;

            long totalNanos = timeStats.getTotalTimeNanos().get() +
                            dateStats.getTotalTimeNanos().get() +
                            priceStats.getTotalTimeNanos().get() +
                            percentStats.getTotalTimeNanos().get();

            return (double) totalNanos / totalCount / 1_000_000;
        }
    }
}
