package net.exylia.commons.redis.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

@Deprecated
public class RedisConfig {

    private String host = "localhost";
    private int port = 6379;
    private String password = null;
    private int database = 0;
    private int timeout = 2000;
    private boolean ssl = false;

    private int maxTotal = 20;
    private int maxIdle = 10;
    private int minIdle = 2;
    private long maxWaitMillis = 3000;
    private boolean testOnBorrow = true;
    private boolean testOnReturn = false;
    private boolean testWhileIdle = true;
    private long timeBetweenEvictionRunsMillis = 30000;

    private int defaultTTL = 3600;  
    private String keyPrefix = "exylia:";

    private boolean enablePubSub = true;
    private String pubSubPrefix = "exylia:pubsub:";

    public RedisConfig() {
         
    }

    public static RedisConfig fromConfig(FileConfiguration config) {
        RedisConfig redisConfig = new RedisConfig();

        if (!config.contains("redis")) {
            return redisConfig;  
        }

        ConfigurationSection redis = config.getConfigurationSection("redis");
        if (redis == null) return redisConfig;

        redisConfig.host = redis.getString("host", "localhost");
        redisConfig.port = redis.getInt("port", 6379);
        redisConfig.password = redis.getString("password");
        redisConfig.database = redis.getInt("database", 0);
        redisConfig.timeout = redis.getInt("timeout", 2000);
        redisConfig.ssl = redis.getBoolean("ssl", false);

        ConfigurationSection pool = redis.getConfigurationSection("pool");
        if (pool != null) {
            redisConfig.maxTotal = pool.getInt("max-total", 20);
            redisConfig.maxIdle = pool.getInt("max-idle", 10);
            redisConfig.minIdle = pool.getInt("min-idle", 2);
            redisConfig.maxWaitMillis = pool.getLong("max-wait-millis", 3000);
            redisConfig.testOnBorrow = pool.getBoolean("test-on-borrow", true);
            redisConfig.testOnReturn = pool.getBoolean("test-on-return", false);
            redisConfig.testWhileIdle = pool.getBoolean("test-while-idle", true);
            redisConfig.timeBetweenEvictionRunsMillis = pool.getLong("time-between-eviction-runs-millis", 30000);
        }

        ConfigurationSection cache = redis.getConfigurationSection("cache");
        if (cache != null) {
            redisConfig.defaultTTL = cache.getInt("default-ttl", 3600);
            redisConfig.keyPrefix = cache.getString("key-prefix", "exylia:");
        }

        ConfigurationSection pubsub = redis.getConfigurationSection("pubsub");
        if (pubsub != null) {
            redisConfig.enablePubSub = pubsub.getBoolean("enabled", true);
            redisConfig.pubSubPrefix = pubsub.getString("prefix", "exylia:pubsub:");
        }

        return redisConfig;
    }

    public static class Builder {
        private final RedisConfig config = new RedisConfig();

        public Builder host(String host) {
            config.host = host;
            return this;
        }

        public Builder port(int port) {
            config.port = port;
            return this;
        }

        public Builder password(String password) {
            config.password = password;
            return this;
        }

        public Builder database(int database) {
            config.database = database;
            return this;
        }

        public Builder timeout(int timeout) {
            config.timeout = timeout;
            return this;
        }

        public Builder ssl(boolean ssl) {
            config.ssl = ssl;
            return this;
        }

        public Builder maxTotal(int maxTotal) {
            config.maxTotal = maxTotal;
            return this;
        }

        public Builder maxIdle(int maxIdle) {
            config.maxIdle = maxIdle;
            return this;
        }

        public Builder minIdle(int minIdle) {
            config.minIdle = minIdle;
            return this;
        }

        public Builder maxWaitMillis(long maxWaitMillis) {
            config.maxWaitMillis = maxWaitMillis;
            return this;
        }

        public Builder testOnBorrow(boolean testOnBorrow) {
            config.testOnBorrow = testOnBorrow;
            return this;
        }

        public Builder defaultTTL(int defaultTTL) {
            config.defaultTTL = defaultTTL;
            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            config.keyPrefix = keyPrefix;
            return this;
        }

        public Builder enablePubSub(boolean enablePubSub) {
            config.enablePubSub = enablePubSub;
            return this;
        }

        public Builder pubSubPrefix(String pubSubPrefix) {
            config.pubSubPrefix = pubSubPrefix;
            return this;
        }

        public RedisConfig build() {
            return config;
        }
    }

    public void validate() {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host de Redis no puede estar vacío");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Puerto de Redis debe estar entre 1 y 65535");
        }
        if (database < 0) {
            throw new IllegalArgumentException("Base de datos de Redis no puede ser negativa");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout debe ser mayor a 0");
        }
        if (maxTotal <= 0) {
            throw new IllegalArgumentException("maxTotal debe ser mayor a 0");
        }
        if (maxIdle < 0) {
            throw new IllegalArgumentException("maxIdle no puede ser negativo");
        }
        if (minIdle < 0) {
            throw new IllegalArgumentException("minIdle no puede ser negativo");
        }
        if (minIdle > maxIdle) {
            throw new IllegalArgumentException("minIdle no puede ser mayor que maxIdle");
        }
        if (defaultTTL <= 0) {
            throw new IllegalArgumentException("TTL por defecto debe ser mayor a 0");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public int getDefaultTTL() {
        return defaultTTL;
    }

    public void setDefaultTTL(int defaultTTL) {
        this.defaultTTL = defaultTTL;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isEnablePubSub() {
        return enablePubSub;
    }

    public void setEnablePubSub(boolean enablePubSub) {
        this.enablePubSub = enablePubSub;
    }

    public String getPubSubPrefix() {
        return pubSubPrefix;
    }

    public void setPubSubPrefix(String pubSubPrefix) {
        this.pubSubPrefix = pubSubPrefix;
    }

    @Override
    public String toString() {
        return "RedisConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", database=" + database +
                ", timeout=" + timeout +
                ", ssl=" + ssl +
                ", maxTotal=" + maxTotal +
                ", maxIdle=" + maxIdle +
                ", minIdle=" + minIdle +
                ", defaultTTL=" + defaultTTL +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", enablePubSub=" + enablePubSub +
                "}";
    }
}
