package net.exylia.commons.v2.formatter.time;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.formatter.FormattersDefaults;

@Getter
@Builder
public class TimeFormatterConfig {
    private final String zeroText;
    private final boolean showMilliseconds;
    private final boolean compactMode;
    private final int precision;
    private final String language;
    private final boolean forceShowZeroDecimals;
    @Builder.Default
    private final long decimalThresholdMillis = -1;
    @Builder.Default
    private final boolean showDecimalsWhenUnderThreshold = false;

    public static TimeFormatterConfig fromConfig() {
        return TimeFormatterConfig.builder()
            .zeroText(FormattersDefaults.Formatters.Time.ZERO_TEXT)
            .showMilliseconds(FormattersDefaults.Formatters.Time.SHOW_MILLISECONDS)
            .compactMode(FormattersDefaults.Formatters.Time.COMPACT_MODE)
            .precision(FormattersDefaults.Formatters.Time.PRECISION)
            .language(FormattersDefaults.Formatters.Time.LANGUAGE)
            .forceShowZeroDecimals(FormattersDefaults.Formatters.Time.FORCE_SHOW_ZERO_DECIMALS)
            .decimalThresholdMillis(FormattersDefaults.Formatters.Time.DECIMAL_THRESHOLD_MILLIS)
            .showDecimalsWhenUnderThreshold(FormattersDefaults.Formatters.Time.SHOW_DECIMALS_UNDER_THRESHOLD)
            .build();
    }

    public static TimeFormatterConfig defaults() {
        return TimeFormatterConfig.builder()
            .zeroText("0s")
            .showMilliseconds(false)
            .compactMode(false)
            .precision(2)
            .language("en")
            .forceShowZeroDecimals(false)
            .decimalThresholdMillis(-1)
            .showDecimalsWhenUnderThreshold(false)
            .build();
    }
}
