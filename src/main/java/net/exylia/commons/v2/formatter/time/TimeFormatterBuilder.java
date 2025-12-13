package net.exylia.commons.v2.formatter.time;

import net.exylia.commons.v2.formatter.cache.FormatterCache;

public class TimeFormatterBuilder {
    private FormatterCache cache;
    private TimeFormatterConfig config;
    private String zeroText;
    private Integer precision;
    private Boolean showMilliseconds;
    private Boolean compactMode;
    private String language;
    private Boolean forceShowZeroDecimals;
    private Long decimalThresholdMillis;
    private Boolean showDecimalsWhenUnderThreshold;

    public TimeFormatterBuilder cache(FormatterCache cache) {
        this.cache = cache;
        return this;
    }

    public TimeFormatterBuilder config(TimeFormatterConfig config) {
        this.config = config;
        return this;
    }

    public TimeFormatterBuilder zeroText(String zeroText) {
        this.zeroText = zeroText;
        return this;
    }

    public TimeFormatterBuilder precision(int precision) {
        this.precision = precision;
        return this;
    }

    public TimeFormatterBuilder showMilliseconds(boolean showMilliseconds) {
        this.showMilliseconds = showMilliseconds;
        return this;
    }

    public TimeFormatterBuilder compactMode(boolean compactMode) {
        this.compactMode = compactMode;
        return this;
    }

    public TimeFormatterBuilder language(String language) {
        this.language = language;
        return this;
    }

    public TimeFormatterBuilder forceShowZeroDecimals(boolean forceShowZeroDecimals) {
        this.forceShowZeroDecimals = forceShowZeroDecimals;
        return this;
    }

    public TimeFormatterBuilder decimalThresholdMillis(long decimalThresholdMillis) {
        this.decimalThresholdMillis = decimalThresholdMillis;
        return this;
    }

    public TimeFormatterBuilder showDecimalsWhenUnderThreshold(boolean showDecimalsWhenUnderThreshold) {
        this.showDecimalsWhenUnderThreshold = showDecimalsWhenUnderThreshold;
        return this;
    }

    public TimeFormatter build() {
        if (cache == null) {
            cache = FormatterCache.getInstance();
        }

        if (config == null) {
            config = TimeFormatterConfig.builder()
                .zeroText(zeroText != null ? zeroText : "0s")
                .precision(precision != null ? precision : 2)
                .showMilliseconds(showMilliseconds != null ? showMilliseconds : false)
                .compactMode(compactMode != null ? compactMode : false)
                .language(language != null ? language : "en")
                .forceShowZeroDecimals(forceShowZeroDecimals != null ? forceShowZeroDecimals : false)
                .decimalThresholdMillis(decimalThresholdMillis != null ? decimalThresholdMillis : -1)
                .showDecimalsWhenUnderThreshold(showDecimalsWhenUnderThreshold != null ? showDecimalsWhenUnderThreshold : false)
                .build();
        }

        return new TimeFormatter(cache, config);
    }
}
