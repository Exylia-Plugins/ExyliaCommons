package net.exylia.commons.v2.redis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SimpleRedis {

    @Getter
    private static SimpleRedis instance;

    private final Plugin plugin;
    private final SimpleRedisConfig config;
    private final JedisPool pool;
    private final Gson gson;
    private final SimpleRedisPubSub pubSub;
    private final ExecutorService asyncExecutor;

    private SimpleRedis(Plugin plugin, SimpleRedisConfig config) {
        DebugUtils.logInternalDebug("SimpleRedis: Starting initialization");
        this.plugin = plugin;
        this.config = config;
        this.gson = new GsonBuilder().create();
        this.asyncExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "SimpleRedis-Async");
            t.setDaemon(true);
            return t;
        });
        DebugUtils.logInternalDebug("SimpleRedis: Async executor created with 2 threads");
        DebugUtils.logInternalDebug("SimpleRedis: Gson serializer created");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
         
        int totalPoolSize = config.getPoolSize() + 10;  
        poolConfig.setMaxTotal(totalPoolSize);
        poolConfig.setMaxIdle(totalPoolSize);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMaxWaitMillis(2000);   
        poolConfig.setBlockWhenExhausted(true);
        DebugUtils.logInternalDebug("SimpleRedis: Pool config created (size: " + totalPoolSize + " [" + config.getPoolSize() + " + 10 for PubSub], maxWait: 2000ms)");

        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            DebugUtils.logInternalDebug("SimpleRedis: Creating pool with password authentication");
            this.pool = new JedisPool(poolConfig, config.getHost(), config.getPort(),
                    config.getTimeout(), config.getPassword(), config.getDatabase());
        } else {
            DebugUtils.logInternalDebug("SimpleRedis: Creating pool without password");
            this.pool = new JedisPool(poolConfig, config.getHost(), config.getPort(),
                    config.getTimeout(), null, config.getDatabase());
        }
        DebugUtils.logInternalDebug("SimpleRedis: JedisPool created for " + config.getHost() + ":" + config.getPort());

        this.pubSub = new SimpleRedisPubSub(this);
        DebugUtils.logInternalDebug("SimpleRedis: PubSub system initialized");

        try (Jedis jedis = pool.getResource()) {
            DebugUtils.logInternalDebug("SimpleRedis: Testing connection with PING");
            String response = jedis.ping();
            DebugUtils.logInternalDebug("SimpleRedis: PING response: " + response);
            DebugUtils.logInternalSuccess("Connected to Redis at " + config.getHost() + ":" + config.getPort());
        }
        DebugUtils.logInternalDebug("SimpleRedis: Initialization completed successfully");
    }

    public static void init(Plugin plugin, SimpleRedisConfig config) {
        DebugUtils.logInternalDebug("SimpleRedis.init: Called with custom config");
        if (instance != null) {
            DebugUtils.logInternalDebug("SimpleRedis.init: Instance already exists, throwing exception");
            throw new IllegalStateException("SimpleRedis already initialized");
        }
        DebugUtils.logInternalDebug("SimpleRedis.init: Creating new instance with config: " + config);
        instance = new SimpleRedis(plugin, config);
        DebugUtils.logInternalDebug("SimpleRedis.init: Instance created successfully");
    }

    public static void init(Plugin plugin) {
        DebugUtils.logInternalDebug("SimpleRedis.init: Called with auto-config from redis.yml");
        if (instance != null) {
            DebugUtils.logInternalDebug("SimpleRedis.init: Instance already exists, throwing exception");
            throw new IllegalStateException("SimpleRedis already initialized");
        }
        DebugUtils.logInternalDebug("SimpleRedis.init: Loading configuration from redis.yml");

        File configFile = new File(plugin.getDataFolder(), "redis.yml");
        if (!configFile.exists()) {
            createDefaultConfig(plugin, configFile);
        }
        FileConfiguration rawConfig = YamlConfiguration.loadConfiguration(configFile);
        if (!rawConfig.getBoolean("redis.enabled", true)) {
            DebugUtils.logInternalInfo("Redis is disabled in redis.yml, skipping initialization");
            return;
        }

        SimpleRedisConfig config = loadConfigFromFile(rawConfig);
        DebugUtils.logInternalDebug("SimpleRedis.init: Configuration loaded, creating instance");
        instance = new SimpleRedis(plugin, config);
        DebugUtils.logInternalDebug("SimpleRedis.init: Instance created successfully");
    }

    private static SimpleRedisConfig loadOrCreateConfig(Plugin plugin) {
        DebugUtils.logInternalDebug("SimpleRedis.loadOrCreateConfig: Starting config load");
        File configFile = new File(plugin.getDataFolder(), "redis.yml");
        DebugUtils.logInternalDebug("SimpleRedis.loadOrCreateConfig: Config file path: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            DebugUtils.logInternalDebug("SimpleRedis.loadOrCreateConfig: Config file not found, creating default");
            createDefaultConfig(plugin, configFile);
        } else {
            DebugUtils.logInternalDebug("SimpleRedis.loadOrCreateConfig: Config file exists, loading from disk");
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        return loadConfigFromFile(config);
    }

    private static SimpleRedisConfig loadConfigFromFile(FileConfiguration config) {
        DebugUtils.logInternalDebug("SimpleRedis.loadConfigFromFile: YAML loaded successfully");

        String host = config.getString("redis.host", "localhost");
        int port = config.getInt("redis.port", 6379);
        String password = config.getString("redis.password");
        int database = config.getInt("redis.database", 0);
        int timeout = config.getInt("redis.timeout", 2000);
        int poolSize = config.getInt("redis.pool-size", 8);
        String keyPrefix = config.getString("redis.key-prefix", "");

        DebugUtils.logInternalDebug("SimpleRedis.loadConfigFromFile: Loaded values - host=" + host +
                ", port=" + port + ", database=" + database + ", timeout=" + timeout +
                ", poolSize=" + poolSize + ", keyPrefix='" + keyPrefix + "'");

        return SimpleRedisConfig.builder()
                .host(host)
                .port(port)
                .password(password)
                .database(database)
                .timeout(timeout)
                .poolSize(poolSize)
                .keyPrefix(keyPrefix)
                .build();
    }

    private static void createDefaultConfig(Plugin plugin, File configFile) {
        DebugUtils.logInternalDebug("SimpleRedis.createDefaultConfig: Starting default config creation");
        try {
            DebugUtils.logInternalDebug("SimpleRedis.createDefaultConfig: Creating parent directories");
            configFile.getParentFile().mkdirs();

            DebugUtils.logInternalDebug("SimpleRedis.createDefaultConfig: Creating config file");
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            DebugUtils.logInternalDebug("SimpleRedis.createDefaultConfig: Setting default values");

            config.set("redis.enabled", true);
            config.set("redis.host", "localhost");
            config.set("redis.port", 6379);
            config.set("redis.password", "");
            config.set("redis.database", 0);
            config.set("redis.timeout", 2000);
            config.set("redis.pool-size", 8);
            config.set("redis.key-prefix", "");

            DebugUtils.logInternalDebug("SimpleRedis.createDefaultConfig: Saving config to file");
            config.save(configFile);
            DebugUtils.logInternalInfo("Default Redis configuration created at redis.yml");
            DebugUtils.logInternalDebug("SimpleRedis.createDefaultConfig: Default config creation completed");

        } catch (IOException e) {
            DebugUtils.logInternalError("SimpleRedis.createDefaultConfig: Failed to create config - " + e.getMessage());
            throw new RuntimeException("Failed to create default Redis configuration", e);
        }
    }

    public static SimpleRedis get() {
        if (instance == null) {
            throw new IllegalStateException("SimpleRedis not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static boolean reload(Plugin plugin) {
        DebugUtils.logInternalDebug("SimpleRedis.reload: Reload requested");
        if (instance == null) {
            DebugUtils.logInternalInfo("SimpleRedis.reload: Instance is null, attempting initialization");
            try {
                init(plugin);
                DebugUtils.logInternalDebug("SimpleRedis.reload: Initialization successful");
                return true;
            } catch (Exception e) {
                DebugUtils.logInternalError("SimpleRedis.reload: Failed to initialize - " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        try {
            DebugUtils.logInternalInfo("SimpleRedis.reload: Starting reload process");
            Plugin savedPlugin = instance.plugin;
            DebugUtils.logInternalDebug("SimpleRedis.reload: Saved plugin reference: " + savedPlugin.getName());

            DebugUtils.logInternalDebug("SimpleRedis.reload: Shutting down current instance");
            instance.shutdown();
            DebugUtils.logInternalDebug("SimpleRedis.reload: Current instance shutdown complete");

            DebugUtils.logInternalDebug("SimpleRedis.reload: Loading new configuration");
            SimpleRedisConfig newConfig = loadOrCreateConfig(savedPlugin);
            DebugUtils.logInternalDebug("SimpleRedis.reload: New configuration loaded");

            DebugUtils.logInternalDebug("SimpleRedis.reload: Creating new instance with reloaded config");
            instance = new SimpleRedis(savedPlugin, newConfig);

            DebugUtils.logInternalSuccess("SimpleRedis reloaded successfully");
            DebugUtils.logInternalDebug("SimpleRedis.reload: Reload process completed");
            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("SimpleRedis.reload: Reload failed - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        DebugUtils.logInternalDebug("SimpleRedis.shutdown: Starting shutdown");

        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            DebugUtils.logInternalDebug("SimpleRedis.shutdown: Shutting down async executor");
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    DebugUtils.logInternalWarn("SimpleRedis.shutdown: Async executor did not terminate in time, forcing shutdown");
                    asyncExecutor.shutdownNow();
                } else {
                    DebugUtils.logInternalDebug("SimpleRedis.shutdown: Async executor shutdown complete");
                }
            } catch (InterruptedException e) {
                DebugUtils.logInternalWarn("SimpleRedis.shutdown: Async executor shutdown interrupted");
                asyncExecutor.shutdownNow();
            }
        }

        if (pubSub != null) {
            DebugUtils.logInternalDebug("SimpleRedis.shutdown: Shutting down PubSub system");
            pubSub.shutdown();
            DebugUtils.logInternalDebug("SimpleRedis.shutdown: PubSub shutdown complete");
        }
        if (pool != null && !pool.isClosed()) {
            DebugUtils.logInternalDebug("SimpleRedis.shutdown: Closing connection pool");
            pool.close();
            DebugUtils.logInternalDebug("SimpleRedis.shutdown: Connection pool closed");
        }
        instance = null;
        DebugUtils.logInternalDebug("SimpleRedis.shutdown: Shutdown completed");
    }

    public boolean isConnected() {
        DebugUtils.logInternalDebug("SimpleRedis.isConnected: Checking connection status");
        try {
            if (pool == null || pool.isClosed()) {
                DebugUtils.logInternalDebug("SimpleRedis.isConnected: Pool is null or closed");
                return false;
            }
            try (Jedis jedis = pool.getResource()) {
                String response = jedis.ping();
                boolean connected = "PONG".equals(response);
                DebugUtils.logInternalDebug("SimpleRedis.isConnected: PING response=" + response + ", connected=" + connected);
                return connected;
            }
        } catch (Exception e) {
            DebugUtils.logInternalDebug("SimpleRedis.isConnected: Connection check failed - " + e.getMessage());
            return false;
        }
    }

    private String key(String key) {
        return config.getKeyPrefix().isEmpty() ? key : config.getKeyPrefix() + key;
    }

    public <T> T execute(Function<Jedis, T> action) {
        try (Jedis jedis = pool.getResource()) {
            return action.apply(jedis);
        }
    }

    public <T> CompletableFuture<T> executeAsync(Function<Jedis, T> action) {
        return CompletableFuture.supplyAsync(() -> execute(action));
    }

    public void set(String key, String value) {
        execute(jedis -> jedis.set(key(key), value));
    }

    public void set(String key, String value, int seconds) {
        execute(jedis -> jedis.setex(key(key), seconds, value));
    }

    public String get(String key) {
        return execute(jedis -> jedis.get(key(key)));
    }

    public boolean exists(String key) {
        return execute(jedis -> jedis.exists(key(key)));
    }

    public void delete(String key) {
        execute(jedis -> jedis.del(key(key)));
    }

    public void expire(String key, int seconds) {
        execute(jedis -> jedis.expire(key(key), seconds));
    }

    public long ttl(String key) {
        return execute(jedis -> jedis.ttl(key(key)));
    }

    public <T> void setObject(String key, T object) {
        set(key, gson.toJson(object));
    }

    public <T> void setObject(String key, T object, int seconds) {
        set(key, gson.toJson(object), seconds);
    }

    public <T> T getObject(String key, Class<T> type) {
        String json = get(key);
        return json == null ? null : gson.fromJson(json, type);
    }

    public void hset(String key, String field, String value) {
        execute(jedis -> jedis.hset(key(key), field, value));
    }

    public String hget(String key, String field) {
        return execute(jedis -> jedis.hget(key(key), field));
    }

    public Map<String, String> hgetAll(String key) {
        return execute(jedis -> jedis.hgetAll(key(key)));
    }

    public void hdel(String key, String... fields) {
        execute(jedis -> jedis.hdel(key(key), fields));
    }

    public boolean hexists(String key, String field) {
        return execute(jedis -> jedis.hexists(key(key), field));
    }

    public <T> void hsetObject(String key, String field, T object) {
        hset(key, field, gson.toJson(object));
    }

    public <T> T hgetObject(String key, String field, Class<T> type) {
        String json = hget(key, field);
        return json == null ? null : gson.fromJson(json, type);
    }

    public void sadd(String key, String... members) {
        execute(jedis -> jedis.sadd(key(key), members));
    }

    public Set<String> smembers(String key) {
        return execute(jedis -> jedis.smembers(key(key)));
    }

    public boolean sismember(String key, String member) {
        return execute(jedis -> jedis.sismember(key(key), member));
    }

    public void srem(String key, String... members) {
        execute(jedis -> jedis.srem(key(key), members));
    }

    public long incr(String key) {
        return execute(jedis -> jedis.incr(key(key)));
    }

    public long incrBy(String key, long value) {
        return execute(jedis -> jedis.incrBy(key(key), value));
    }

    public long decr(String key) {
        return execute(jedis -> jedis.decr(key(key)));
    }

    public long decrBy(String key, long value) {
        return execute(jedis -> jedis.decrBy(key(key), value));
    }

    public SimpleRedisPubSub pubSub() {
        return pubSub;
    }

    public void publish(String channel, String message) {
        try {
            DebugUtils.logInternalDebug("SimpleRedis.publish: Getting Jedis connection from pool...");
            Long subscribers = execute(jedis -> {
                DebugUtils.logInternalDebug("SimpleRedis.publish: Got Jedis connection, publishing to channel '" + channel + "'");
                Long result = jedis.publish(channel, message);
                DebugUtils.logInternalDebug("SimpleRedis.publish: Published successfully, " + result + " subscribers received the message");
                return result;
            });
            DebugUtils.logInternalDebug("SimpleRedis.publish: Publish completed for channel '" + channel + "', reached " + subscribers + " subscribers");
        } catch (Exception e) {
            DebugUtils.logInternalError("SimpleRedis.publish: Failed to publish to channel '" + channel + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public <T> void publishObject(String channel, T object) {
        publish(channel, gson.toJson(object));
    }

    public CompletableFuture<Void> publishAsync(String channel, String message) {
        DebugUtils.logInternalDebug("SimpleRedis.publishAsync: Publishing to channel '" + channel + "', message length: " + message.length());
        return CompletableFuture.runAsync(() -> {
            DebugUtils.logInternalDebug("SimpleRedis.publishAsync: Executing publish in async thread for channel '" + channel + "'");
            publish(channel, message);
            DebugUtils.logInternalDebug("SimpleRedis.publishAsync: Publish completed for channel '" + channel + "'");
        }, asyncExecutor);
    }

    public <T> CompletableFuture<Void> publishObjectAsync(String channel, T object) {
        String json = gson.toJson(object);
        DebugUtils.logInternalDebug("SimpleRedis.publishObjectAsync: Publishing object to channel '" + channel + "', JSON length: " + json.length());
        return publishAsync(channel, json);
    }

    public void runSync(Runnable task) {
        Tasks.sync(task);
    }

    public void runAsync(Runnable task) {
        Tasks.run(task);
    }

    Plugin getPlugin() {
        return plugin;
    }

    Gson getGson() {
        return gson;
    }

    JedisPool getPool() {
        return pool;
    }
}
