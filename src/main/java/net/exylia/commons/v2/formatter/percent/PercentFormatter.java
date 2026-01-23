package net.exylia.commons.v2.formatter.percent;

import lombok.Getter;
import net.exylia.commons.v2.formatter.cache.FormatterCache;
import net.exylia.commons.v2.formatter.core.AbstractFormatter;
import net.exylia.commons.v2.formatter.core.FormatterException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Getter
public class PercentFormatter extends AbstractFormatter<Object, String> {
    private final PercentFormatterConfig config;
    private final ThreadLocal<DecimalFormat> formatters;

    PercentFormatter(FormatterCache cache, PercentFormatterConfig config) {
        super(cache);
        this.config = config;
        this.formatters = ThreadLocal.withInitial(() -> createDecimalFormat(config));
    }

    public static PercentFormatterBuilder builder() {
        return new PercentFormatterBuilder();
    }

    @Override
    public String format(Object input) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        String formatted = formatters.get().format(amount);
        String result = buildResult(formatted, amount.doubleValue());

        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    @Override
    public String format(Object input, String customPattern) {
        return format(input);
    }

    public String formatWithDecimals(Object input, int decimals) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        DecimalFormat df = createDecimalFormat(decimals);
        String formatted = df.format(amount);
        String result = buildResult(formatted, amount.doubleValue());

        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatNoSuffix(Object input) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        String result = formatters.get().format(amount);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatWithSuffix(Object input, String suffix) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        String formatted = formatters.get().format(amount);
        String result = config.isShowPlusSign() && amount.doubleValue() > 0
            ? "+" + formatted + suffix
            : formatted + suffix;

        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatRatio(Object numerator, Object denominator) {
        BigDecimal num = parseInput(numerator);
        BigDecimal denom = parseInput(denominator);

        if (denom.compareTo(BigDecimal.ZERO) == 0) {
            return format(0);
        }

        BigDecimal percent = num.divide(denom, 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        return format(percent);
    }

    @Override
    protected String getCacheKey(Object input, String pattern) {
        return "percent_" + pattern + "_" + input.toString();
    }

    private String buildResult(String formatted, double value) {
        if (config.isShowPlusSign() && value > 0) {
            return "+" + formatted + config.getSuffix();
        }
        return formatted + config.getSuffix();
    }

    private BigDecimal parseInput(Object input) {
        if (input == null) {
            return BigDecimal.ZERO;
        }

        if (input instanceof BigDecimal) {
            return (BigDecimal) input;
        }

        if (input instanceof Number) {
            return BigDecimal.valueOf(((Number) input).doubleValue());
        }

        if (input instanceof String) {
            try {
                String cleaned = ((String) input).replaceAll("[^0-9.,-]", "");
                return new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                throw new FormatterException("Unable to parse percent: " + input, e);
            }
        }

        throw new FormatterException("Unsupported input type for percent: " + input.getClass().getName());
    }

    private DecimalFormat createDecimalFormat(PercentFormatterConfig config) {
        return createDecimalFormat(config.getDecimalPlaces());
    }

    private DecimalFormat createDecimalFormat(int decimalPlaces) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator(config.getDecimalSeparator().charAt(0));

        String pattern = decimalPlaces > 0 && config.isShowDecimals()
            ? "#,##0." + "0".repeat(decimalPlaces)
            : "#,##0";

        DecimalFormat formatter = new DecimalFormat(pattern, symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter;
    }
}
