package net.exylia.commons.v2.database.config;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.database.redis.RedisConfig;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Getter
public class DatabaseConfig {
    private final Map<String, AdapterConfig> adapterConfigs;
    private final RedisConfig redisConfig;
    private final String serverId;

    public DatabaseConfig(Config config) {
        this(config, null);
    }

    public DatabaseConfig(Config config, Plugin plugin) {
        this.adapterConfigs = new HashMap<>();
        adapterConfigs.put("h2", new AdapterConfig(config, "database.h2", plugin));
        adapterConfigs.put("mysql", new AdapterConfig(config, "database.mysql", plugin));
        adapterConfigs.put("mongodb", new AdapterConfig(config, "database.mongodb", plugin));
        this.redisConfig = new RedisConfig(config);
        String sid = config.string("database.server-id");
        this.serverId = sid != null && !sid.isEmpty() ? sid : "server-1";
    }

    public AdapterConfig getAdapterConfig(String type) {
        return adapterConfigs.get(type.toLowerCase());
    }
}
