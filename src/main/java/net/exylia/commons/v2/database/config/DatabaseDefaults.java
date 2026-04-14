package net.exylia.commons.v2.database.config;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

@ConfigSchema(file = "database", version = "1.0")
public class DatabaseDefaults {

    @ConfigSection("database")
    public static class Database {
        @ConfigValue("type")
        @Comment("Database type: h2, mysql, mongodb")
        public static String TYPE = "h2";

        @ConfigValue("server-id")
        @Comment("Unique identifier for this server in a multi-server network. Used for cross-server teleports and messaging.")
        public static String SERVER_ID = "server-1";

        @ConfigSection("settings")
        public static class Settings {
            @ConfigValue("pool-size")
            public static int POOL_SIZE = 10;

            @ConfigValue("minimum-idle")
            public static int MINIMUM_IDLE = 2;

            @ConfigValue("connection-timeout")
            public static int CONNECTION_TIMEOUT = 30000;

            @ConfigValue("idle-timeout")
            public static int IDLE_TIMEOUT = 600000;

            @ConfigValue("max-lifetime")
            public static int MAX_LIFETIME = 1800000;
        }

        @ConfigSection("write-behind")
        public static class WriteBehind {
            @ConfigValue("enabled")
            @Comment("Enable write-behind cache: saves are instant and flushed to DB periodically. Disable for direct synchronous saves.")
            public static boolean ENABLED = true;

            @ConfigValue("flush-interval")
            @Comment("How often (in seconds) dirty data is flushed to the database.")
            public static int FLUSH_INTERVAL = 30;
        }

        @ConfigSection("cache")
        public static class Cache {
            @ConfigValue("enabled")
            public static boolean ENABLED = true;

            @ConfigValue("ttl-minutes")
            public static int TTL_MINUTES = 30;

            @ConfigValue("max-entries")
            public static int MAX_ENTRIES = 10000;
        }

        @ConfigSection("h2")
        public static class H2 {
            @ConfigValue("file")
            public static String FILE = "database/h2";

            @ConfigValue("username")
            public static String USERNAME = "sa";

            @ConfigValue("password")
            public static String PASSWORD = "";

            @ConfigValue("auto-server")
            @Comment("Enable AUTO_SERVER mode for H2 (allows multiple connections)")
            public static boolean AUTO_SERVER = false;
        }

        @ConfigSection("mysql")
        public static class MySQL {
            @ConfigValue("host")
            public static String HOST = "localhost";

            @ConfigValue("port")
            public static int PORT = 3306;

            @ConfigValue("database")
            public static String DATABASE = "minecraft";

            @ConfigValue("username")
            public static String USERNAME = "root";

            @ConfigValue("password")
            public static String PASSWORD = "";

            @ConfigValue("ssl")
            public static boolean SSL = false;
        }

        @ConfigSection("mongodb")
        public static class MongoDB {
            @ConfigValue("host")
            public static String HOST = "localhost";

            @ConfigValue("port")
            public static int PORT = 27017;

            @ConfigValue("database")
            public static String DATABASE = "minecraft";

            @ConfigValue("username")
            public static String USERNAME = "";

            @ConfigValue("password")
            public static String PASSWORD = "";

            @ConfigValue("auth-database")
            public static String AUTH_DATABASE = "admin";

            @ConfigValue("connection-string")
            public static String CONNECTION_STRING = "";
        }

        @ConfigSection("redis")
        public static class Redis {
            @ConfigValue("enabled")
            @Comment("Enable Redis for shared L2 cache and cross-server cache invalidation. Required for multi-server setups.")
            public static boolean ENABLED = false;

            @ConfigValue("host")
            public static String HOST = "localhost";

            @ConfigValue("port")
            public static int PORT = 6379;

            @ConfigValue("password")
            public static String PASSWORD = "";

            @ConfigValue("database")
            public static int DATABASE = 0;

            @ConfigValue("pool-size")
            public static int POOL_SIZE = 8;

            @ConfigValue("ttl-seconds")
            @Comment("How long cached entities are stored in Redis before expiring (seconds).")
            public static int TTL_SECONDS = 1800;

            @ConfigValue("key-prefix")
            @Comment("Prefix for all Redis keys. Use a unique value per network to isolate environments.")
            public static String KEY_PREFIX = "exylia";
        }
    }
}
