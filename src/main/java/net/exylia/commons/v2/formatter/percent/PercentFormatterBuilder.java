package net.exylia.commons.v2.formatter.percent;

import net.exylia.commons.v2.formatter.cache.FormatterCache;

public class PercentFormatterBuilder {
    private FormatterCache cache;
    private PercentFormatterConfig config;
    private String suffix;
    private Integer decimalPlaces;
    private Boolean showDecimals;
    private String decimalSeparator;
    private Boolean showPlusSign;

    public PercentFormatterBuilder cache(FormatterCache cache) {
        this.cache = cache;
        return this;
    }

    public PercentFormatterBuilder config(PercentFormatterConfig config) {
        this.config = config;
        return this;
    }

    public PercentFormatterBuilder suffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public PercentFormatterBuilder decimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    public PercentFormatterBuilder showDecimals(boolean showDecimals) {
        this.showDecimals = showDecimals;
        return this;
    }

    public PercentFormatterBuilder decimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
        return this;
    }

    public PercentFormatterBuilder showPlusSign(boolean showPlusSign) {
        this.showPlusSign = showPlusSign;
        return this;
    }

    public PercentFormatter build() {
        if (cache == null) {
            cache = FormatterCache.getInstance();
        }

        if (config == null) {
            config = PercentFormatterConfig.builder()
                .suffix(suffix != null ? suffix : "%")
                .decimalPlaces(decimalPlaces != null ? decimalPlaces : 0)
                .showDecimals(showDecimals != null ? showDecimals : false)
                .decimalSeparator(decimalSeparator != null ? decimalSeparator : ".")
                .showPlusSign(showPlusSign != null ? showPlusSign : false)
                .build();
        }

        return new PercentFormatter(cache, config);
    }
}
