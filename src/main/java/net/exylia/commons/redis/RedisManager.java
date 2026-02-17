package net.exylia.commons.redis;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.redis.config.RedisConfig;
import net.exylia.commons.redis.connection.RedisConnectionManager;
import net.exylia.commons.redis.pubsub.RedisPubSubManager;
import net.exylia.commons.redis.cache.RedisCache;
import net.exylia.commons.redis.serialization.RedisSerializer;
import net.exylia.commons.redis.serialization.GsonRedisSerializer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

@Deprecated
public class RedisManager {

    private static RedisManager instance;
    private final JavaPlugin plugin;
    private final RedisConfig config;
    private final RedisConnectionManager connectionManager;
    private final RedisPubSubManager pubSubManager;
    private final ConcurrentHashMap<String, RedisCache<?>> caches;
    private final RedisSerializer defaultSerializer;
    private final Executor asyncExecutor;
    private boolean initialized = false;

    private RedisManager(JavaPlugin plugin, RedisConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.connectionManager = new RedisConnectionManager(config);
        this.pubSubManager = new RedisPubSubManager(connectionManager);
        this.caches = new ConcurrentHashMap<>();
        this.defaultSerializer = new GsonRedisSerializer();
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "ExyliaRedis-Async");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static synchronized void start(JavaPlugin plugin, RedisConfig config) {
        if (instance != null) {
            logInternalError("RedisManager ya está inicializado!");
            return;
        }

        instance = new RedisManager(plugin, config);
        instance.startup();
    }

    public static RedisManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RedisManager no ha sido inicializado! Llama a initialize() primero.");
        }
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null && instance.initialized;
    }

    private void startup() {
        try {
            connectionManager.initialize();

            try (Jedis jedis = connectionManager.getConnection()) {
                jedis.ping();
                logInternalInfo("Conexión a Redis establecida correctamente");
            }

            pubSubManager.initialize();
            initialized = true;

            startMaintenanceTask();

            logInternalInfo("RedisManager inicializado correctamente");

        } catch (Exception e) {
            logInternalError("Error al inicializar RedisManager: " + e.getMessage());
            throw new RuntimeException("Fallo al inicializar Redis", e);
        }
    }

    public synchronized void shutdown() {
        if (!initialized) return;

        logInternalInfo("Cerrando RedisManager...");

        try {
             
            caches.values().forEach(RedisCache::close);
            caches.clear();

            pubSubManager.shutdown();

            connectionManager.shutdown();

            initialized = false;
            instance = null;

            logInternalInfo("RedisManager cerrado correctamente");

        } catch (Exception e) {
            logInternalError("Error al cerrar RedisManager: " + e.getMessage());
        }
    }

    public <T> T execute(Function<Jedis, T> operation) {
        if (!initialized) {
            throw new IllegalStateException("RedisManager no está inicializado");
        }

        try (Jedis jedis = connectionManager.getConnection()) {
            return operation.apply(jedis);
        } catch (JedisException e) {
            logInternalError("Error ejecutando operación Redis: " + e.getMessage());
            throw e;
        }
    }

    public <T> CompletableFuture<T> executeAsync(Function<Jedis, T> operation) {
        return CompletableFuture.supplyAsync(() -> execute(operation), asyncExecutor);
    }

    public CompletableFuture<Void> executeAsync(Consumer<Jedis> operation) {
        return CompletableFuture.runAsync(() -> execute(jedis -> {
            operation.accept(jedis);
            return null;
        }), asyncExecutor);
    }

    public void set(String key, String value) {
        execute(jedis -> jedis.set(key, value));
    }

    public void set(String key, String value, int seconds) {
        execute(jedis -> jedis.setex(key, seconds, value));
    }

    public String get(String key) {
        return execute(jedis -> jedis.get(key));
    }

    public <T> void setObject(String key, T object) {
        String serialized = defaultSerializer.serialize(object);
        set(key, serialized);
    }

    public <T> void setObject(String key, T object, int seconds) {
        String serialized = defaultSerializer.serialize(object);
        set(key, serialized, seconds);
    }

    public <T> T getObject(String key, Class<T> type) {
        String serialized = get(key);
        if (serialized == null) return null;
        return defaultSerializer.deserialize(serialized, type);
    }

    public CompletableFuture<Void> setAsync(String key, String value) {
        return CompletableFuture.runAsync(() -> {
            execute(jedis -> {
                jedis.set(key, value);
                return null;
            });
        }, asyncExecutor);
    }

    public CompletableFuture<String> getAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            return execute(jedis -> jedis.get(key));
        }, asyncExecutor);
    }

    public <T> CompletableFuture<Void> setObjectAsync(String key, T object) {
        return CompletableFuture.runAsync(() -> setObject(key, object), asyncExecutor);
    }

    public <T> CompletableFuture<T> getObjectAsync(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> getObject(key, type), asyncExecutor);
    }

    public boolean exists(String key) {
        return execute(jedis -> jedis.exists(key));
    }

    public boolean delete(String key) {
        return execute(jedis -> jedis.del(key) > 0);
    }

    public boolean expire(String key, int seconds) {
        return execute(jedis -> jedis.expire(key, seconds) == 1);
    }

    public long getTTL(String key) {
        return execute(jedis -> jedis.ttl(key));
    }

    @SuppressWarnings("unchecked")
    public <T> RedisCache<T> getCache(String name, Class<T> type) {
        return (RedisCache<T>) caches.computeIfAbsent(name,
                k -> new RedisCache<>(this, name, type, defaultSerializer));
    }

    @SuppressWarnings("unchecked")
    public <T> RedisCache<T> getCache(String name, Class<T> type, RedisSerializer serializer) {
        return (RedisCache<T>) caches.computeIfAbsent(name,
                k -> new RedisCache<>(this, name, type, serializer));
    }

    public RedisPubSubManager getPubSub() {
        return pubSubManager;
    }

    public void publish(String channel, String message) {
        pubSubManager.publish(channel, message);
    }

    public <T> void publishObject(String channel, T object) {
        String serialized = defaultSerializer.serialize(object);
        publish(channel, serialized);
    }

    public RedisConfig getConfig() {
        return config;
    }

    public RedisConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public RedisSerializer getDefaultSerializer() {
        return defaultSerializer;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void startMaintenanceTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!initialized) {
                    cancel();
                    return;
                }

                try {
                     
                    caches.values().forEach(RedisCache::cleanup);

                    connectionManager.validateConnections();

                } catch (Exception e) {
                    logInternalError("Error en tarea de mantenimiento Redis: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60);  
    }
}
