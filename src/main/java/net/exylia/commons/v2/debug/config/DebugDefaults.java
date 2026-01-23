package net.exylia.commons.v2.debug.config;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

import java.util.Collections;
import java.util.List;

@ConfigSchema(file = "config", version = "1.0")
public class DebugDefaults {

    @ConfigSection("debug")
    @Comment("Debug configuration")
    public static class Debug {
        @ConfigValue("level")
        @Comment("Debug level: 0=DISABLED, 1=INFO, 2=VERBOSE, 3=ALL")
        public static int LEVEL = 0;

        @ConfigValue("categories")
        @Comment("Allowed debug categories (empty = all)")
        public static List<String> CATEGORIES = Collections.emptyList();

        @ConfigValue("show-timestamps")
        @Comment("Show timestamps in debug output")
        public static boolean SHOW_TIMESTAMPS = false;

        @ConfigValue("show-class-names")
        @Comment("Show class names in debug output")
        public static boolean SHOW_CLASS_NAMES = false;

        @ConfigValue("async-logging")
        @Comment("Use async logging for better performance")
        public static boolean ASYNC_LOGGING = false;
    }
}
