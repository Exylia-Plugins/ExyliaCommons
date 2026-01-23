package net.exylia.commons.v2.formatter;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

@ConfigSchema(file = "config", version = "1.0")
public class FormattersDefaults {

    @ConfigSection("formatters")
    @Comment("Formatter configurations")
    public static class Formatters {

        @ConfigSection("time")
        @Comment("Time formatter configuration")
        public static class Time {
            @ConfigValue("zero-text")
            @Comment("Text to display for zero duration")
            public static String ZERO_TEXT = "0s";

            @ConfigValue("show-milliseconds")
            @Comment("Show milliseconds in output")
            public static boolean SHOW_MILLISECONDS = true;

            @ConfigValue("compact-mode")
            @Comment("Use compact format (1h2m vs 1 hour 2 minutes)")
            public static boolean COMPACT_MODE = false;

            @ConfigValue("precision")
            @Comment("Number of time units to show (1 = only largest unit)")
            public static int PRECISION = 1;

            @ConfigValue("language")
            @Comment("Language for time units (en, es, etc)")
            public static String LANGUAGE = "en";

            @ConfigValue("force-show-zero-decimals")
            @Comment("Always show decimal places even when zero")
            public static boolean FORCE_SHOW_ZERO_DECIMALS = true;

            @ConfigValue("decimal-threshold-millis")
            @Comment("Show decimals only below this threshold (ms)")
            public static int DECIMAL_THRESHOLD_MILLIS = 10000;

            @ConfigValue("show-decimals-under-threshold")
            @Comment("Enable decimal display under threshold")
            public static boolean SHOW_DECIMALS_UNDER_THRESHOLD = true;
        }

        @ConfigSection("date")
        @Comment("Date formatter configuration")
        public static class Date {
            @ConfigValue("default-pattern")
            @Comment("Default date-time format pattern")
            public static String DEFAULT_PATTERN = "dd/MM/yyyy HH:mm:ss";

            @ConfigValue("date-pattern")
            @Comment("Date-only format pattern")
            public static String DATE_PATTERN = "dd/MM/yyyy";

            @ConfigValue("time-pattern")
            @Comment("Time-only format pattern")
            public static String TIME_PATTERN = "HH:mm:ss";

            @ConfigValue("use-iso-default")
            @Comment("Use ISO 8601 format as default")
            public static boolean USE_ISO_DEFAULT = false;
        }

        @ConfigSection("price")
        @Comment("Price formatter configuration")
        public static class Price {
            @ConfigValue("currency-symbol")
            @Comment("Currency symbol to use")
            public static String CURRENCY_SYMBOL = "$";

            @ConfigValue("symbol-before")
            @Comment("Place symbol before amount ($100 vs 100$)")
            public static boolean SYMBOL_BEFORE = true;

            @ConfigValue("decimal-separator")
            @Comment("Decimal separator character")
            public static String DECIMAL_SEPARATOR = ".";

            @ConfigValue("thousand-separator")
            @Comment("Thousand separator character")
            public static String THOUSAND_SEPARATOR = ",";

            @ConfigValue("decimal-places")
            @Comment("Number of decimal places")
            public static int DECIMAL_PLACES = 2;

            @ConfigValue("show-decimals")
            @Comment("Show decimal places")
            public static boolean SHOW_DECIMALS = true;
        }

        @ConfigSection("percent")
        @Comment("Percent formatter configuration")
        public static class Percent {
            @ConfigValue("suffix")
            @Comment("Suffix to append (typically %)")
            public static String SUFFIX = "%";

            @ConfigValue("decimal-places")
            @Comment("Number of decimal places")
            public static int DECIMAL_PLACES = 0;

            @ConfigValue("show-decimals")
            @Comment("Show decimal places")
            public static boolean SHOW_DECIMALS = false;

            @ConfigValue("decimal-separator")
            @Comment("Decimal separator character")
            public static String DECIMAL_SEPARATOR = ".";

            @ConfigValue("show-plus-sign")
            @Comment("Show + sign for positive values")
            public static boolean SHOW_PLUS_SIGN = false;
        }
    }
}
