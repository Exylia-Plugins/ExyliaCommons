package net.exylia.commons.v2.config;

import lombok.Getter;
import net.exylia.commons.v2.formatter.date.DateFormatterConfig;
import net.exylia.commons.v2.formatter.price.PriceFormatterConfig;
import net.exylia.commons.v2.formatter.time.TimeFormatterConfig;

@Getter
public class FormattersConfig {
    private static volatile FormattersConfig instance;

    private final TimeFormatterConfig timeConfig;
    private final DateFormatterConfig dateConfig;
    private final PriceFormatterConfig priceConfig;

    private FormattersConfig() {
        this.timeConfig = TimeFormatterConfig.fromConfig();
        this.dateConfig = DateFormatterConfig.fromConfig();
        this.priceConfig = PriceFormatterConfig.fromConfig();
    }

    public static FormattersConfig getInstance() {
        if (instance == null) {
            synchronized (FormattersConfig.class) {
                if (instance == null) {
                    instance = new FormattersConfig();
                }
            }
        }
        return instance;
    }

    public static void reload() {
        synchronized (FormattersConfig.class) {
            instance = new FormattersConfig();
        }
    }

    public static void ensureDefaults() {
        if (!Configs.exists("formatters.time.zero-text")) {
            Configs.set("formatters.time.zero-text", "0s");
        }
        if (!Configs.exists("formatters.time.show-milliseconds")) {
            Configs.set("formatters.time.show-milliseconds", false);
        }
        if (!Configs.exists("formatters.time.compact-mode")) {
            Configs.set("formatters.time.compact-mode", false);
        }
        if (!Configs.exists("formatters.time.precision")) {
            Configs.set("formatters.time.precision", 2);
        }
        if (!Configs.exists("formatters.time.language")) {
            Configs.set("formatters.time.language", "en");
        }
        if (!Configs.exists("formatters.time.force-show-zero-decimals")) {
            Configs.set("formatters.time.force-show-zero-decimals", false);
        }
        if (!Configs.exists("formatters.time.decimal-threshold-millis")) {
            Configs.set("formatters.time.decimal-threshold-millis", -1);
        }
        if (!Configs.exists("formatters.time.show-decimals-under-threshold")) {
            Configs.set("formatters.time.show-decimals-under-threshold", false);
        }

        if (!Configs.exists("formatters.date.default-pattern")) {
            Configs.set("formatters.date.default-pattern", "dd/MM/yyyy HH:mm:ss");
        }
        if (!Configs.exists("formatters.date.date-pattern")) {
            Configs.set("formatters.date.date-pattern", "dd/MM/yyyy");
        }
        if (!Configs.exists("formatters.date.time-pattern")) {
            Configs.set("formatters.date.time-pattern", "HH:mm:ss");
        }
        if (!Configs.exists("formatters.date.use-iso-default")) {
            Configs.set("formatters.date.use-iso-default", false);
        }

        if (!Configs.exists("formatters.price.currency-symbol")) {
            Configs.set("formatters.price.currency-symbol", "$");
        }
        if (!Configs.exists("formatters.price.symbol-before")) {
            Configs.set("formatters.price.symbol-before", true);
        }
        if (!Configs.exists("formatters.price.decimal-separator")) {
            Configs.set("formatters.price.decimal-separator", ".");
        }
        if (!Configs.exists("formatters.price.thousand-separator")) {
            Configs.set("formatters.price.thousand-separator", ",");
        }
        if (!Configs.exists("formatters.price.decimal-places")) {
            Configs.set("formatters.price.decimal-places", 2);
        }
        if (!Configs.exists("formatters.price.show-decimals")) {
            Configs.set("formatters.price.show-decimals", true);
        }

        Configs.save();
    }
}
