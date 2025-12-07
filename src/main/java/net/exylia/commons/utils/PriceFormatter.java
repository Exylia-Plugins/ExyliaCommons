package net.exylia.commons.utils;

import net.exylia.commons.v2.config.Configs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class PriceFormatter {

    private static String currencySymbol;
    private static boolean symbolBefore;
    private static String decimalSeparator;
    private static String thousandSeparator;
    private static int decimalPlaces;
    private static boolean showDecimals;

    static {
        init();
    }

    public static void init() {
        reload();
    }

    public static void reload() {
        currencySymbol = Configs.string("price-formatter.currency-symbol", "$");
        symbolBefore = Configs.bool("price-formatter.symbol-before", true);
        decimalSeparator = Configs.string("price-formatter.decimal-separator", ".");
        thousandSeparator = Configs.string("price-formatter.thousand-separator", ",");
        decimalPlaces = Configs.integer("price-formatter.decimal-places", 2);
        showDecimals = Configs.bool("price-formatter.show-decimals", true);
    }

    public static String format(Object input) {
        BigDecimal amount = parseInput(input);
        return formatAmount(amount);
    }

    public static String formatCompact(Object input) {
        BigDecimal amount = parseInput(input);
        return formatCompactAmount(amount);
    }

    public static String formatNoSymbol(Object input) {
        BigDecimal amount = parseInput(input);
        return formatNumber(amount);
    }

    public static String formatWithSymbol(Object input, String symbol) {
        BigDecimal amount = parseInput(input);
        String formatted = formatNumber(amount);
        return symbolBefore ? symbol + formatted : formatted + symbol;
    }

    private static BigDecimal parseInput(Object input) {
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
                String cleaned = ((String) input)
                        .replaceAll("[^0-9.,\\-]", "")
                        .replace(",", ".");
                return new BigDecimal(cleaned);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }

        return BigDecimal.ZERO;
    }

    private static String formatAmount(BigDecimal amount) {
        String formatted = formatNumber(amount);
        return symbolBefore ? currencySymbol + formatted : formatted + currencySymbol;
    }

    private static String formatNumber(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator(decimalSeparator.charAt(0));
        symbols.setGroupingSeparator(thousandSeparator.charAt(0));

        String pattern = showDecimals
                ? "#,##0." + "0".repeat(decimalPlaces)
                : "#,##0";

        DecimalFormat formatter = new DecimalFormat(pattern, symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);

        return formatter.format(amount);
    }

    private static String formatCompactAmount(BigDecimal amount) {
        double value = amount.doubleValue();
        String suffix = "";

        if (value >= 1_000_000_000_000.0) {
            value /= 1_000_000_000_000.0;
            suffix = "T";
        } else if (value >= 1_000_000_000.0) {
            value /= 1_000_000_000.0;
            suffix = "B";
        } else if (value >= 1_000_000.0) {
            value /= 1_000_000.0;
            suffix = "M";
        } else if (value >= 1_000.0) {
            value /= 1_000.0;
            suffix = "K";
        }

        DecimalFormat df = new DecimalFormat("0.#");
        String formatted = df.format(value) + suffix;

        return symbolBefore ? currencySymbol + formatted : formatted + currencySymbol;
    }
}
