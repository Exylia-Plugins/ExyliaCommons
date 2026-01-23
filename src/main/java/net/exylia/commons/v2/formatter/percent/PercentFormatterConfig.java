package net.exylia.commons.v2.formatter.percent;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.formatter.FormattersDefaults;

@Getter
@Builder
public class PercentFormatterConfig {
    private final String suffix;
    private final int decimalPlaces;
    private final boolean showDecimals;
    private final String decimalSeparator;
    private final boolean showPlusSign;

    public static PercentFormatterConfig fromConfig() {
        return PercentFormatterConfig.builder()
            .suffix(FormattersDefaults.Formatters.Percent.SUFFIX)
            .decimalPlaces(FormattersDefaults.Formatters.Percent.DECIMAL_PLACES)
            .showDecimals(FormattersDefaults.Formatters.Percent.SHOW_DECIMALS)
            .decimalSeparator(FormattersDefaults.Formatters.Percent.DECIMAL_SEPARATOR)
            .showPlusSign(FormattersDefaults.Formatters.Percent.SHOW_PLUS_SIGN)
            .build();
    }

    public static PercentFormatterConfig defaults() {
        return PercentFormatterConfig.builder()
            .suffix("%")
            .decimalPlaces(0)
            .showDecimals(false)
            .decimalSeparator(".")
            .showPlusSign(false)
            .build();
    }
}
