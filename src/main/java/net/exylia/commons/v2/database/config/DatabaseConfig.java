package net.exylia.commons.v2.database.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.Configs;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class DatabaseConfig {

    private final String databaseType;
    private final boolean autoMigration;
    private final boolean debug;
    private final boolean enableMetrics;
    private final Map<String, AdapterConfig> adapterConfigs;
    private final CacheConfig cacheConfig;
    private final FallbackConfig fallbackConfig;

    public DatabaseConfig(Config config) {
        this(config, null);
    }

    public DatabaseConfig(Config config, Plugin plugin) {
        this.databaseType = config.string("settings.type") != null ? config.string("settings.type") : "H2";
        this.autoMigration = config.bool("settings.auto-migration") || config.bool("settings.auto-migration", true);
        this.debug = config.bool("settings.debug", false);
        this.enableMetrics = config.bool("settings.enable-metrics", false);

        this.adapterConfigs = new HashMap<>();
        adapterConfigs.put("H2", new AdapterConfig(config, "h2", plugin));
        adapterConfigs.put("MySQL", new AdapterConfig(config, "mysql", plugin));
        adapterConfigs.put("MongoDB", new AdapterConfig(config, "mongodb", plugin));

        this.cacheConfig = new CacheConfig(config);
        this.fallbackConfig = new FallbackConfig(config);
    }

    public static void ensureDefaults() {
        if (!Configs.exists("settings.type")) {
            Configs.set("settings.type", "H2");
        }
        if (!Configs.exists("settings.auto-migration")) {
            Configs.set("settings.auto-migration", true);
        }
        if (!Configs.exists("settings.debug")) {
            Configs.set("settings.debug", false);
        }
        if (!Configs.exists("settings.enable-metrics")) {
            Configs.set("settings.enable-metrics", false);
        }

        if (!Configs.exists("settings.cache.enabled")) {
            Configs.set("settings.cache.enabled", true);
        }
        if (!Configs.exists("settings.cache.strategy")) {
            Configs.set("settings.cache.strategy", "CAFFEINE");
        }
        if (!Configs.exists("settings.cache.ttl-minutes")) {
            Configs.set("settings.cache.ttl-minutes", 30);
        }
        if (!Configs.exists("settings.cache.max-entries")) {
            Configs.set("settings.cache.max-entries", 10000);
        }
        if (!Configs.exists("settings.cache.record-stats")) {
            Configs.set("settings.cache.record-stats", false);
        }
        if (!Configs.exists("settings.cache.refresh-after-access")) {
            Configs.set("settings.cache.refresh-after-access", true);
        }

        if (!Configs.exists("settings.fallback.enabled")) {
            Configs.set("settings.fallback.enabled", true);
        }
        if (!Configs.exists("settings.fallback.chain")) {
            Configs.set("settings.fallback.chain", Arrays.asList("H2", "YAML"));
        }

        if (!Configs.exists("h2.file-path")) {
            Configs.set("h2.file-path", "data/database");
        }
        if (!Configs.exists("h2.username")) {
            Configs.set("h2.username", "sa");
        }
        if (!Configs.exists("h2.password")) {
            Configs.set("h2.password", "");
        }
        if (!Configs.exists("h2.pool.max-size")) {
            Configs.set("h2.pool.max-size", 10);
        }
        if (!Configs.exists("h2.pool.min-idle")) {
            Configs.set("h2.pool.min-idle", 2);
        }
        if (!Configs.exists("h2.pool.connection-timeout")) {
            Configs.set("h2.pool.connection-timeout", 30000);
        }
        if (!Configs.exists("h2.pool.idle-timeout")) {
            Configs.set("h2.pool.idle-timeout", 600000);
        }
        if (!Configs.exists("h2.pool.max-lifetime")) {
            Configs.set("h2.pool.max-lifetime", 1800000);
        }

        if (!Configs.exists("mysql.host")) {
            Configs.set("mysql.host", "localhost");
        }
        if (!Configs.exists("mysql.port")) {
            Configs.set("mysql.port", 3306);
        }
        if (!Configs.exists("mysql.database")) {
            Configs.set("mysql.database", "minecraft");
        }
        if (!Configs.exists("mysql.username")) {
            Configs.set("mysql.username", "root");
        }
        if (!Configs.exists("mysql.password")) {
            Configs.set("mysql.password", "");
        }
        if (!Configs.exists("mysql.use-ssl")) {
            Configs.set("mysql.use-ssl", false);
        }
        if (!Configs.exists("mysql.pool.max-size")) {
            Configs.set("mysql.pool.max-size", 10);
        }
        if (!Configs.exists("mysql.pool.min-idle")) {
            Configs.set("mysql.pool.min-idle", 2);
        }
        if (!Configs.exists("mysql.pool.connection-timeout")) {
            Configs.set("mysql.pool.connection-timeout", 30000);
        }
        if (!Configs.exists("mysql.pool.idle-timeout")) {
            Configs.set("mysql.pool.idle-timeout", 600000);
        }
        if (!Configs.exists("mysql.pool.max-lifetime")) {
            Configs.set("mysql.pool.max-lifetime", 1800000);
        }
        if (!Configs.exists("mysql.properties.cachePrepStmts")) {
            Configs.set("mysql.properties.cachePrepStmts", true);
        }
        if (!Configs.exists("mysql.properties.prepStmtCacheSize")) {
            Configs.set("mysql.properties.prepStmtCacheSize", 250);
        }
        if (!Configs.exists("mysql.properties.prepStmtCacheSqlLimit")) {
            Configs.set("mysql.properties.prepStmtCacheSqlLimit", 2048);
        }
        if (!Configs.exists("mysql.properties.useServerPrepStmts")) {
            Configs.set("mysql.properties.useServerPrepStmts", true);
        }
        if (!Configs.exists("mysql.properties.useLocalSessionState")) {
            Configs.set("mysql.properties.useLocalSessionState", true);
        }
        if (!Configs.exists("mysql.properties.rewriteBatchedStatements")) {
            Configs.set("mysql.properties.rewriteBatchedStatements", true);
        }
        if (!Configs.exists("mysql.properties.cacheResultSetMetadata")) {
            Configs.set("mysql.properties.cacheResultSetMetadata", true);
        }
        if (!Configs.exists("mysql.properties.cacheServerConfiguration")) {
            Configs.set("mysql.properties.cacheServerConfiguration", true);
        }
        if (!Configs.exists("mysql.properties.elideSetAutoCommits")) {
            Configs.set("mysql.properties.elideSetAutoCommits", true);
        }
        if (!Configs.exists("mysql.properties.maintainTimeStats")) {
            Configs.set("mysql.properties.maintainTimeStats", false);
        }

        if (!Configs.exists("mongodb.connection-string")) {
            Configs.set("mongodb.connection-string", "mongodb://localhost:27017");
        }
        if (!Configs.exists("mongodb.database")) {
            Configs.set("mongodb.database", "minecraft");
        }
        if (!Configs.exists("mongodb.username")) {
            Configs.set("mongodb.username", "");
        }
        if (!Configs.exists("mongodb.password")) {
            Configs.set("mongodb.password", "");
        }
        if (!Configs.exists("mongodb.auth-database")) {
            Configs.set("mongodb.auth-database", "admin");
        }
        if (!Configs.exists("mongodb.pool.max-size")) {
            Configs.set("mongodb.pool.max-size", 100);
        }
        if (!Configs.exists("mongodb.pool.min-size")) {
            Configs.set("mongodb.pool.min-size", 10);
        }
        if (!Configs.exists("mongodb.pool.max-idle-time")) {
            Configs.set("mongodb.pool.max-idle-time", 120000);
        }
        if (!Configs.exists("mongodb.pool.max-connection-lifetime")) {
            Configs.set("mongodb.pool.max-connection-lifetime", 600000);
        }
        if (!Configs.exists("mongodb.pool.connection-timeout")) {
            Configs.set("mongodb.pool.connection-timeout", 30000);
        }

        Configs.save();
    }

    public AdapterConfig getAdapterConfig(String type) {
        return adapterConfigs.get(type);
    }

    @Getter
    public static class CacheConfig {
        private final boolean enabled;
        private final String strategy;
        private final long ttlMinutes;
        private final int maxEntries;
        private final boolean recordStats;
        private final boolean refreshAfterAccess;

        public CacheConfig(Config config) {
            this.enabled = config.bool("settings.cache.enabled", true);
            String str = config.string("settings.cache.strategy");
            this.strategy = str != null ? str : "CAFFEINE";
            this.ttlMinutes = config.longValue("settings.cache.ttl-minutes", 30);
            this.maxEntries = config.integer("settings.cache.max-entries", 10000);
            this.recordStats = config.bool("settings.cache.record-stats", false);
            this.refreshAfterAccess = config.bool("settings.cache.refresh-after-access", true);
        }
    }

    @Getter
    public static class FallbackConfig {
        private final boolean enabled;
        private final java.util.List<String> chain;

        public FallbackConfig(Config config) {
            this.enabled = config.bool("settings.fallback.enabled", true);
            this.chain = java.util.List.of("H2", "YAML");
        }
    }
}
