package net.exylia.commons.v2.database.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Getter
public class DatabaseConfig {

    private final String databaseType;
    private final Map<String, AdapterConfig> adapterConfigs;
    private final CacheConfig cacheConfig;

    public DatabaseConfig(Config config) {
        this(config, null);
    }

    public DatabaseConfig(Config config, Plugin plugin) {
        this.databaseType = DatabaseDefaults.Settings.TYPE;

        this.adapterConfigs = new HashMap<>();
        adapterConfigs.put("H2", new AdapterConfig(config, "h2", plugin));
        adapterConfigs.put("MySQL", new AdapterConfig(config, "mysql", plugin));
        adapterConfigs.put("MongoDB", new AdapterConfig(config, "mongodb", plugin));

        this.cacheConfig = new CacheConfig();
    }

    public AdapterConfig getAdapterConfig(String type) {
        return adapterConfigs.get(type);
    }

    @Getter
    public static class CacheConfig {
        private final boolean enabled;
        private final long ttlMinutes;
        private final int maxEntries;
        private final boolean recordStats;
        private final boolean refreshAfterAccess;

        public CacheConfig() {
            this.enabled = DatabaseDefaults.Settings.Cache.ENABLED;
            this.ttlMinutes = DatabaseDefaults.Settings.Cache.TTL_MINUTES;
            this.maxEntries = DatabaseDefaults.Settings.Cache.MAX_ENTRIES;
            this.recordStats = DatabaseDefaults.Settings.Cache.RECORD_STATS;
            this.refreshAfterAccess = DatabaseDefaults.Settings.Cache.REFRESH_AFTER_ACCESS;
        }
    }
}
