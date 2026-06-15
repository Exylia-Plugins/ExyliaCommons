package net.exylia.commons.v2.database.redis;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;

@Getter
public class RedisConfig {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    private final int poolSize;
    private final int ttlSeconds;
    private final String keyPrefix;
    private final String invalidationChannel;

    public RedisConfig(Config config) {
        this.enabled = config.bool("database.redis.enabled", false);
        String h = config.string("database.redis.host");
        this.host = h != null ? h : "localhost";
        this.port = config.integer("database.redis.port", 6379);
        String pw = config.string("database.redis.password");
        this.password = pw != null ? pw : "";
        this.database = config.integer("database.redis.database", 0);
        this.poolSize = config.integer("database.redis.pool-size", 8);
        this.ttlSeconds = config.integer("database.redis.ttl-seconds", 1800);
        String kp = config.string("database.redis.key-prefix");
        this.keyPrefix = kp != null && !kp.isEmpty() ? kp : "exylia";
        this.invalidationChannel = this.keyPrefix + ":db:invalidate";
    }
}
