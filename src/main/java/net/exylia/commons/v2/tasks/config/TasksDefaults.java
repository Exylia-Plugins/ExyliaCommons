package net.exylia.commons.v2.tasks.config;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

@ConfigSchema(file = "config", strict = true,version = "1.0")
public class TasksDefaults {

    @ConfigSection("tasks")
    @Comment("Task system configuration (async pools, schedulers, monitoring)")
    public static class Tasks {

        @ConfigSection("pools")
        @Comment("Thread pool configurations per category")
        public static class Pools {

            @ConfigSection("general")
            @Comment("General purpose async tasks")
            public static class General {
                @ConfigValue("core-size")
                public static int CORE_SIZE = 4;

                @ConfigValue("max-size")
                public static int MAX_SIZE = 8;

                @ConfigValue("queue-size")
                public static int QUEUE_SIZE = 500;
            }

            @ConfigSection("database")
            @Comment("Database operations pool")
            public static class Database {
                @ConfigValue("core-size")
                public static int CORE_SIZE = 4;

                @ConfigValue("max-size")
                public static int MAX_SIZE = 4;

                @ConfigValue("queue-size")
                public static int QUEUE_SIZE = 500;
            }

            @ConfigSection("io")
            @Comment("IO operations pool (files, network)")
            public static class IO {
                @ConfigValue("core-size")
                public static int CORE_SIZE = 2;

                @ConfigValue("max-size")
                public static int MAX_SIZE = 8;

                @ConfigValue("queue-size")
                public static int QUEUE_SIZE = 300;
            }

            @ConfigSection("compute")
            @Comment("Heavy computation pool")
            public static class Compute {
                @ConfigValue("core-size")
                public static int CORE_SIZE = 2;

                @ConfigValue("max-size")
                public static int MAX_SIZE = 4;

                @ConfigValue("queue-size")
                public static int QUEUE_SIZE = 500;
            }
        }

        @ConfigSection("settings")
        @Comment("General task system settings")
        public static class Settings {
            @ConfigValue("keep-alive-seconds")
            @Comment("Idle thread keep-alive time")
            public static int KEEP_ALIVE_SECONDS = 60;

            @ConfigValue("default-timeout-seconds")
            @Comment("Default task timeout")
            public static int DEFAULT_TIMEOUT_SECONDS = 30;

            @ConfigValue("shutdown-timeout-seconds")
            @Comment("Max wait time on shutdown")
            public static int SHUTDOWN_TIMEOUT_SECONDS = 10;
        }

        @ConfigSection("monitoring")
        @Comment("Pool monitoring and alerts")
        public static class Monitoring {
            @ConfigValue("enabled")
            public static boolean ENABLED = true;

            @ConfigValue("interval-seconds")
            @Comment("Check interval for pool health")
            public static int INTERVAL_SECONDS = 30;

            @ConfigValue("high-load-threshold")
            @Comment("Threshold (0.0-1.0) to trigger warnings")
            public static double HIGH_LOAD_THRESHOLD = 0.8;

            @ConfigValue("log-stats-on-shutdown")
            @Comment("Print stats when shutting down")
            public static boolean LOG_STATS_ON_SHUTDOWN = true;
        }

        @ConfigSection("stats")
        @Comment("Statistics collection")
        public static class Stats {
            @ConfigValue("enabled")
            public static boolean ENABLED = true;

            @ConfigValue("track-durations")
            @Comment("Track min/max/avg execution times")
            public static boolean TRACK_DURATIONS = true;
        }
    }
}
