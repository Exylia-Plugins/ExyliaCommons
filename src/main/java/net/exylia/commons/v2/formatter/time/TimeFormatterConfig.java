package net.exylia.commons.v2.formatter.time;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.config.Configs;

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
            .zeroText(Configs.string("formatters.time.zero-text", "0s"))
            .showMilliseconds(Configs.bool("formatters.time.show-milliseconds", false))
            .compactMode(Configs.bool("formatters.time.compact-mode", false))
            .precision(Configs.integer("formatters.time.precision", 2))
            .language(Configs.string("formatters.time.language", "en"))
            .forceShowZeroDecimals(Configs.bool("formatters.time.force-show-zero-decimals", false))
            .decimalThresholdMillis(Configs.longValue("formatters.time.decimal-threshold-millis", -1L))
            .showDecimalsWhenUnderThreshold(Configs.bool("formatters.time.show-decimals-under-threshold", false))
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
