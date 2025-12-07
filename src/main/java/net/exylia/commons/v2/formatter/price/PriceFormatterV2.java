package net.exylia.commons.v2.formatter.price;

import lombok.Getter;
import net.exylia.commons.v2.formatter.core.AbstractFormatter;
import net.exylia.commons.v2.formatter.core.FormatterException;
import net.exylia.commons.v2.formatter.cache.FormatterCache;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Getter
public class PriceFormatterV2 extends AbstractFormatter<Object, String> {
    private final PriceFormatterConfig config;
    private final ThreadLocal<DecimalFormat> formatters;

    PriceFormatterV2(FormatterCache cache, PriceFormatterConfig config) {
        super(cache);
        this.config = config;
        this.formatters = ThreadLocal.withInitial(() -> createDecimalFormat(config));
    }

    public static PriceFormatterBuilder builder() {
        return new PriceFormatterBuilder();
    }

    @Override
    public String format(Object input) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        String formatted = formatters.get().format(amount);
        String result = config.isSymbolBefore()
            ? config.getCurrencySymbol() + formatted
            : formatted + config.getCurrencySymbol();

        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    @Override
    public String format(Object input, String customPattern) {
        return format(input);
    }

    public String formatCompact(Object input) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        CompactSuffix suffix = CompactSuffix.fromValue(amount.doubleValue());
        double value = amount.doubleValue() / suffix.getDivisor();

        DecimalFormat df = cache.getDecimalFormat(1, false,
            config.getDecimalSeparator(), config.getThousandSeparator());
        String formatted = df.format(value) + suffix.getSuffix();

        String result = config.isSymbolBefore()
            ? config.getCurrencySymbol() + formatted
            : formatted + config.getCurrencySymbol();

        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatNoSymbol(Object input) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        String result = formatters.get().format(amount);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatWithSymbol(Object input, String symbol) {
        long startTime = System.nanoTime();
        BigDecimal amount = parseInput(input);

        String formatted = formatters.get().format(amount);
        String result = config.isSymbolBefore()
            ? symbol + formatted
            : formatted + symbol;

        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    @Override
    protected String getCacheKey(Object input, String pattern) {
        return "price_" + pattern + "_" + input.toString();
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
                String cleaned = ((String) input).replaceAll("[^0-9.-]", "");
                return new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                throw new FormatterException("Unable to parse price: " + input, e);
            }
        }

        throw new FormatterException("Unsupported input type for price: " + input.getClass().getName());
    }

    private DecimalFormat createDecimalFormat(PriceFormatterConfig config) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator(config.getDecimalSeparator().charAt(0));
        symbols.setGroupingSeparator(config.getThousandSeparator().charAt(0));

        String pattern = config.isShowDecimals()
            ? "#,##0." + "0".repeat(config.getDecimalPlaces())
            : "#,##0";

        DecimalFormat formatter = new DecimalFormat(pattern, symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter;
    }
}
