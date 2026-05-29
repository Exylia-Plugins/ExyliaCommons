package net.exylia.commons.v2.formatter.price;

import net.exylia.commons.v2.formatter.cache.FormatterCache;

public class PriceFormatterBuilder {
    private FormatterCache cache;
    private PriceFormatterConfig config;
    private String currencySymbol;
    private Boolean symbolBefore;
    private String decimalSeparator;
    private String thousandSeparator;
    private Integer decimalPlaces;
    private Boolean showDecimals;
    private Boolean compactNotation;

    public PriceFormatterBuilder cache(FormatterCache cache) {
        this.cache = cache;
        return this;
    }

    public PriceFormatterBuilder config(PriceFormatterConfig config) {
        this.config = config;
        return this;
    }

    public PriceFormatterBuilder currencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
        return this;
    }

    public PriceFormatterBuilder symbolBefore(boolean symbolBefore) {
        this.symbolBefore = symbolBefore;
        return this;
    }

    public PriceFormatterBuilder decimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
        return this;
    }

    public PriceFormatterBuilder thousandSeparator(String thousandSeparator) {
        this.thousandSeparator = thousandSeparator;
        return this;
    }

    public PriceFormatterBuilder decimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    public PriceFormatterBuilder showDecimals(boolean showDecimals) {
        this.showDecimals = showDecimals;
        return this;
    }

    public PriceFormatterBuilder compactNotation(boolean compactNotation) {
        this.compactNotation = compactNotation;
        return this;
    }

    public PriceFormatter build() {
        if (cache == null) {
            cache = FormatterCache.getInstance();
        }

        if (config == null) {
            config = PriceFormatterConfig.builder()
                .currencySymbol(currencySymbol != null ? currencySymbol : "$")
                .symbolBefore(symbolBefore != null ? symbolBefore : true)
                .decimalSeparator(decimalSeparator != null ? decimalSeparator : ".")
                .thousandSeparator(thousandSeparator != null ? thousandSeparator : ",")
                .decimalPlaces(decimalPlaces != null ? decimalPlaces : 2)
                .showDecimals(showDecimals != null ? showDecimals : true)
                .compactNotation(compactNotation != null ? compactNotation : false)
                .build();
        }

        return new PriceFormatter(cache, config);
    }
}
