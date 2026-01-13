package net.exylia.commons.v2.formatter.price;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.formatter.FormattersDefaults;

@Getter
@Builder
public class PriceFormatterConfig {
    private final String currencySymbol;
    private final boolean symbolBefore;
    private final String decimalSeparator;
    private final String thousandSeparator;
    private final int decimalPlaces;
    private final boolean showDecimals;

    public static PriceFormatterConfig fromConfig() {
        return PriceFormatterConfig.builder()
            .currencySymbol(FormattersDefaults.Formatters.Price.CURRENCY_SYMBOL)
            .symbolBefore(FormattersDefaults.Formatters.Price.SYMBOL_BEFORE)
            .decimalSeparator(FormattersDefaults.Formatters.Price.DECIMAL_SEPARATOR)
            .thousandSeparator(FormattersDefaults.Formatters.Price.THOUSAND_SEPARATOR)
            .decimalPlaces(FormattersDefaults.Formatters.Price.DECIMAL_PLACES)
            .showDecimals(FormattersDefaults.Formatters.Price.SHOW_DECIMALS)
            .build();
    }

    public static PriceFormatterConfig defaults() {
        return PriceFormatterConfig.builder()
            .currencySymbol("$")
            .symbolBefore(true)
            .decimalSeparator(".")
            .thousandSeparator(",")
            .decimalPlaces(2)
            .showDecimals(true)
            .build();
    }
}
