package net.exylia.commons.v2.discord.config;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

@ConfigSchema(file = "config", strict = true,version = "1.0")
public class DiscordConfig {

    @ConfigSection("http")
    public static class Http {
        @ConfigValue("timeout")
        @Comment("Seconds for connections timeout")
        public static int TIMEOUT = 10;
    }
}
