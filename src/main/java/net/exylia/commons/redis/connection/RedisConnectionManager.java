package net.exylia.commons.redis.connection;

import net.exylia.commons.redis.config.RedisConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

@Deprecated
public class RedisConnectionManager {

    private final RedisConfig config;
    private JedisPool jedisPool;
    private volatile boolean initialized = false;

    public RedisConnectionManager(RedisConfig config) {
        this.config = config;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            config.validate();

            JedisPoolConfig poolConfig = createPoolConfig();

            if (config.getPassword() != null && !config.getPassword().trim().isEmpty()) {
                jedisPool = new JedisPool(
                        poolConfig,
                        config.getHost(),
                        config.getPort(),
                        config.getTimeout(),
                        config.getPassword(),
                        config.getDatabase(),
                        config.isSsl()
                );
            } else {
                jedisPool = new JedisPool(
                        poolConfig,
                        config.getHost(),
                        config.getPort(),
                        config.getTimeout(),
                        null,
                        config.getDatabase(),
                        config.isSsl()
                );
            }

            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            initialized = true;
            logInternalInfo("Pool de conexiones Redis inicializado - Host: " + config.getHost() + ":" + config.getPort());

        } catch (Exception e) {
            logInternalError("Error al inicializar pool de conexiones Redis: " + e.getMessage());
            if (jedisPool != null) {
                jedisPool.close();
                jedisPool = null;
            }
            throw new RuntimeException("No se pudo conectar a Redis", e);
        }
    }

    public Jedis getConnection() {
        if (!initialized || jedisPool == null) {
            throw new IllegalStateException("ConnectionManager no está inicializado");
        }

        try {
            return jedisPool.getResource();
        } catch (JedisException e) {
            logInternalError("Error al obtener conexión Redis: " + e.getMessage());
            throw e;
        }
    }

    public boolean isActive() {
        return initialized && jedisPool != null && !jedisPool.isClosed();
    }

    public PoolStats getPoolStats() {
        if (!isActive()) {
            return new PoolStats(0, 0, 0);
        }

        return new PoolStats(
                jedisPool.getNumActive(),
                jedisPool.getNumIdle(),
                jedisPool.getNumWaiters()
        );
    }

    public void validateConnections() {
        if (!isActive()) {
            return;
        }

        try (Jedis jedis = getConnection()) {
            jedis.ping();
        } catch (Exception e) {
            logInternalError("Error validando conexiones Redis: " + e.getMessage());
             
            reinitialize();
        }
    }

    public synchronized void reinitialize() {
        logInternalInfo("Reinicializando pool de conexiones Redis...");

        shutdown();

        try {
            Thread.sleep(1000);  
            initialize();
            logInternalInfo("Pool de conexiones Redis reinicializado correctamente");
        } catch (Exception e) {
            logInternalError("Error al reinicializar pool Redis: " + e.getMessage());
        }
    }

    public synchronized void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            try {
                jedisPool.close();
                logInternalInfo("Pool de conexiones Redis cerrado");
            } catch (Exception e) {
                logInternalError("Error al cerrar pool Redis: " + e.getMessage());
            }
        }

        jedisPool = null;
        initialized = false;
    }

    private JedisPoolConfig createPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(config.getMaxTotal());
        poolConfig.setMaxIdle(config.getMaxIdle());
        poolConfig.setMinIdle(config.getMinIdle());
        poolConfig.setMaxWaitMillis(config.getMaxWaitMillis());

        poolConfig.setTestOnBorrow(config.isTestOnBorrow());
        poolConfig.setTestOnReturn(config.isTestOnReturn());
        poolConfig.setTestWhileIdle(config.isTestWhileIdle());
        poolConfig.setTimeBetweenEvictionRunsMillis(config.getTimeBetweenEvictionRunsMillis());

        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setJmxEnabled(false);

        return poolConfig;
    }

    public static class PoolStats {
        private final int active;
        private final int idle;
        private final int waiters;

        public PoolStats(int active, int idle, int waiters) {
            this.active = active;
            this.idle = idle;
            this.waiters = waiters;
        }

        public int getActive() {
            return active;
        }

        public int getIdle() {
            return idle;
        }

        public int getWaiters() {
            return waiters;
        }

        public int getTotal() {
            return active + idle;
        }

        @Override
        public String toString() {
            return "PoolStats{" +
                    "active=" + active +
                    ", idle=" + idle +
                    ", waiters=" + waiters +
                    ", total=" + getTotal() +
                    '}';
        }
    }

    public RedisConfig getConfig() {
        return config;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
