package net.exylia.commons.v2.database.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Getter
public class DatabaseV2Config {

    private final String databaseType;
    private final boolean autoMigration;
    private final boolean debug;
    private final boolean enableMetrics;
    private final Map<String, AdapterConfig> adapterConfigs;
    private final CacheConfig cacheConfig;
    private final FallbackConfig fallbackConfig;

    public DatabaseV2Config(Config config) {
        this(config, null);
    }

    public DatabaseV2Config(Config config, Plugin plugin) {
        this.databaseType = config.string("database.type") != null ? config.string("database.type") : "H2";
        this.autoMigration = config.bool("database.auto-migration") || config.bool("database.auto-migration", true);
        this.debug = config.bool("database.debug", false);
        this.enableMetrics = config.bool("database.enable-metrics", false);

        this.adapterConfigs = new HashMap<>();
        adapterConfigs.put("H2", new AdapterConfig(config, "h2", plugin));
        adapterConfigs.put("MySQL", new AdapterConfig(config, "mysql", plugin));
        adapterConfigs.put("MongoDB", new AdapterConfig(config, "mongodb", plugin));

        this.cacheConfig = new CacheConfig(config);
        this.fallbackConfig = new FallbackConfig(config);
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
            this.enabled = config.bool("database.cache.enabled", true);
            String str = config.string("database.cache.strategy");
            this.strategy = str != null ? str : "CAFFEINE";
            this.ttlMinutes = config.longValue("database.cache.ttl-minutes", 30);
            this.maxEntries = config.integer("database.cache.max-entries", 10000);
            this.recordStats = config.bool("database.cache.record-stats", false);
            this.refreshAfterAccess = config.bool("database.cache.refresh-after-access", true);
        }
    }

    @Getter
    public static class FallbackConfig {
        private final boolean enabled;
        private final java.util.List<String> chain;

        public FallbackConfig(Config config) {
            this.enabled = config.bool("database.fallback.enabled", true);
            this.chain = java.util.List.of("H2", "YAML");
        }
    }
}
