package net.exylia.commons.v2.database.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.function.Function;

public class RedisConnectionPool {

    private final JedisPool pool;
    private final RedisConfig config;

    public RedisConnectionPool(RedisConfig config) {
        this.config = config;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.getPoolSize());
        poolConfig.setMaxIdle(config.getPoolSize());
        poolConfig.setMinIdle(1);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(true);

        if (!config.getPassword().isEmpty()) {
            this.pool = new JedisPool(poolConfig, config.getHost(), config.getPort(),
                    2000, config.getPassword(), config.getDatabase());
        } else {
            this.pool = new JedisPool(poolConfig, config.getHost(), config.getPort(),
                    2000, null, config.getDatabase());
        }
    }

    public <T> T execute(Function<Jedis, T> action) {
        try (Jedis jedis = pool.getResource()) {
            return action.apply(jedis);
        }
    }

    public Jedis createSubscriberConnection() {
        Jedis jedis = new Jedis(config.getHost(), config.getPort(), 0);
        if (!config.getPassword().isEmpty()) {
            jedis.auth(config.getPassword());
        }
        jedis.select(config.getDatabase());
        return jedis;
    }

    public boolean ping() {
        try {
            return execute(jedis -> "PONG".equals(jedis.ping()));
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
