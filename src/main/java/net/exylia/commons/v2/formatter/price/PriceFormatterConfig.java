package net.exylia.commons.v2.formatter.price;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.config.Configs;

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
            .currencySymbol(Configs.string("formatters.price.currency-symbol", "$"))
            .symbolBefore(Configs.bool("formatters.price.symbol-before", true))
            .decimalSeparator(Configs.string("formatters.price.decimal-separator", "."))
            .thousandSeparator(Configs.string("formatters.price.thousand-separator", ","))
            .decimalPlaces(Configs.integer("formatters.price.decimal-places", 2))
            .showDecimals(Configs.bool("formatters.price.show-decimals", true))
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
