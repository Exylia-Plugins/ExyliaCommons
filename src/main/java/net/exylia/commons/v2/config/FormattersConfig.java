package net.exylia.commons.v2.config;

import lombok.Getter;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.formatter.FormattersDefaults;
import net.exylia.commons.v2.formatter.date.DateFormatterConfig;
import net.exylia.commons.v2.formatter.percent.PercentFormatterConfig;
import net.exylia.commons.v2.formatter.price.PriceFormatterConfig;
import net.exylia.commons.v2.formatter.time.TimeFormatterConfig;

@Getter
public class FormattersConfig {
    private static volatile FormattersConfig instance;

    private final TimeFormatterConfig timeConfig;
    private final DateFormatterConfig dateConfig;
    private final PriceFormatterConfig priceConfig;
    private final PercentFormatterConfig percentConfig;

    private FormattersConfig() {
        this.timeConfig = TimeFormatterConfig.fromConfig();
        this.dateConfig = DateFormatterConfig.fromConfig();
        this.priceConfig = PriceFormatterConfig.fromConfig();
        this.percentConfig = PercentFormatterConfig.fromConfig();
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
}
