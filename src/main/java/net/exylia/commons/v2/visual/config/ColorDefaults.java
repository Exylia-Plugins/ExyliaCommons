package net.exylia.commons.v2.visual.config;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

@ConfigSchema(file = "config", strict = true,version = "1.0")
public class ColorDefaults {

    @ConfigSection("text")
    @Comment("Text formatting configuration")
    public static class Text {
        @ConfigValue("automatic-font")
        @Comment("Font style for automatic text formatting (none, small, fraktur, bold_fraktur, script, double_struck, squared, bold, italic, bold_italic, monospace, negative_squared)")
        public static String AUTOMATIC_FONT = "small";

        @ConfigValue("force-in-upper-case")
        @Comment("Force text to uppercase when using automatic font")
        public static boolean FORCE_IN_UPPER_CASE = true;
    }
}
