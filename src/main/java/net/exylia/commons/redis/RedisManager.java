package net.exylia.commons.redis;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.redis.config.RedisConfig;
import net.exylia.commons.redis.connection.RedisConnectionManager;
import net.exylia.commons.redis.pubsub.RedisPubSubManager;
import net.exylia.commons.redis.cache.RedisCache;
import net.exylia.commons.redis.serialization.RedisSerializer;
import net.exylia.commons.redis.serialization.GsonRedisSerializer;
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

/**
 * Gestor principal de Redis para ExyliaCommons
 * Proporciona una interfaz simple y optimizada para todas las operaciones de Redis
 */
public class RedisManager {

    private static RedisManager instance;
    private final ExyliaPlugin plugin;
    private final RedisConfig config;
    private final RedisConnectionManager connectionManager;
    private final RedisPubSubManager pubSubManager;
    private final ConcurrentHashMap<String, RedisCache<?>> caches;
    private final RedisSerializer defaultSerializer;
    private final Executor asyncExecutor;
    private boolean initialized = false;

    private RedisManager(ExyliaPlugin plugin, RedisConfig config) {
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

    /**
     * Inicializa el sistema de Redis
     */
    public static synchronized void start(ExyliaPlugin plugin, RedisConfig config) {
        if (instance != null) {
            logInternalError("RedisManager ya está inicializado!");
            return;
        }

        instance = new RedisManager(plugin, config);
        instance.startup();
    }

    /**
     * Obtiene la instancia del RedisManager
     */
    public static RedisManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RedisManager no ha sido inicializado! Llama a initialize() primero.");
        }
        return instance;
    }

    /**
     * Verifica si Redis está disponible
     */
    public static boolean isAvailable() {
        return instance != null && instance.initialized;
    }

    private void startup() {
        try {
            connectionManager.initialize();

            // Probar conexión
            try (Jedis jedis = connectionManager.getConnection()) {
                jedis.ping();
                logInternalInfo("Conexión a Redis establecida correctamente");
            }

            pubSubManager.initialize();
            initialized = true;

            // Iniciar tarea de mantenimiento
            startMaintenanceTask();

            logInternalInfo("RedisManager inicializado correctamente");

        } catch (Exception e) {
            logInternalError("Error al inicializar RedisManager: " + e.getMessage());
            throw new RuntimeException("Fallo al inicializar Redis", e);
        }
    }

    /**
     * Cierra el sistema de Redis
     */
    public synchronized void shutdown() {
        if (!initialized) return;

        logInternalInfo("Cerrando RedisManager...");

        try {
            // Cerrar caches
            caches.values().forEach(RedisCache::close);
            caches.clear();

            // Cerrar pub/sub
            pubSubManager.shutdown();

            // Cerrar conexiones
            connectionManager.shutdown();

            initialized = false;
            instance = null;

            logInternalInfo("RedisManager cerrado correctamente");

        } catch (Exception e) {
            logInternalError("Error al cerrar RedisManager: " + e.getMessage());
        }
    }

    // ==================== OPERACIONES BÁSICAS ====================

    /**
     * Ejecuta una operación de Redis de forma síncrona
     */
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

    /**
     * Ejecuta una operación de Redis de forma asíncrona
     */
    public <T> CompletableFuture<T> executeAsync(Function<Jedis, T> operation) {
        return CompletableFuture.supplyAsync(() -> execute(operation), asyncExecutor);
    }

    /**
     * Ejecuta una operación sin retorno de forma asíncrona
     */
    public CompletableFuture<Void> executeAsync(Consumer<Jedis> operation) {
        return CompletableFuture.runAsync(() -> execute(jedis -> {
            operation.accept(jedis);
            return null;
        }), asyncExecutor);
    }

    // ==================== OPERACIONES DE STRING ====================

    /**
     * Establece un valor string
     */
    public void set(String key, String value) {
        execute(jedis -> jedis.set(key, value));
    }

    /**
     * Establece un valor string con expiración
     */
    public void set(String key, String value, int seconds) {
        execute(jedis -> jedis.setex(key, seconds, value));
    }

    /**
     * Obtiene un valor string
     */
    public String get(String key) {
        return execute(jedis -> jedis.get(key));
    }

    /**
     * Establece un objeto serializado
     */
    public <T> void setObject(String key, T object) {
        String serialized = defaultSerializer.serialize(object);
        set(key, serialized);
    }

    /**
     * Establece un objeto serializado con expiración
     */
    public <T> void setObject(String key, T object, int seconds) {
        String serialized = defaultSerializer.serialize(object);
        set(key, serialized, seconds);
    }

    /**
     * Obtiene un objeto deserializado
     */
    public <T> T getObject(String key, Class<T> type) {
        String serialized = get(key);
        if (serialized == null) return null;
        return defaultSerializer.deserialize(serialized, type);
    }

    // ==================== OPERACIONES ASÍNCRONAS ====================

    /**
     * Establece un valor de forma asíncrona
     */
    public CompletableFuture<Void> setAsync(String key, String value) {
        return CompletableFuture.runAsync(() -> {
            execute(jedis -> {
                jedis.set(key, value);
                return null;
            });
        }, asyncExecutor);
    }

    /**
     * Obtiene un valor de forma asíncrona
     */
    public CompletableFuture<String> getAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            return execute(jedis -> jedis.get(key));
        }, asyncExecutor);
    }

    /**
     * Establece un objeto de forma asíncrona
     */
    public <T> CompletableFuture<Void> setObjectAsync(String key, T object) {
        return CompletableFuture.runAsync(() -> setObject(key, object), asyncExecutor);
    }

    /**
     * Obtiene un objeto de forma asíncrona
     */
    public <T> CompletableFuture<T> getObjectAsync(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> getObject(key, type), asyncExecutor);
    }

    // ==================== UTILIDADES ====================

    /**
     * Verifica si una clave existe
     */
    public boolean exists(String key) {
        return execute(jedis -> jedis.exists(key));
    }

    /**
     * Elimina una clave
     */
    public boolean delete(String key) {
        return execute(jedis -> jedis.del(key) > 0);
    }

    /**
     * Establece expiración a una clave
     */
    public boolean expire(String key, int seconds) {
        return execute(jedis -> jedis.expire(key, seconds) == 1);
    }

    /**
     * Obtiene el TTL de una clave
     */
    public long getTTL(String key) {
        return execute(jedis -> jedis.ttl(key));
    }

    // ==================== GESTIÓN DE CACHÉ ====================

    /**
     * Crea o obtiene una caché tipada
     */
    @SuppressWarnings("unchecked")
    public <T> RedisCache<T> getCache(String name, Class<T> type) {
        return (RedisCache<T>) caches.computeIfAbsent(name,
                k -> new RedisCache<>(this, name, type, defaultSerializer));
    }

    /**
     * Crea o obtiene una caché tipada con serializer personalizado
     */
    @SuppressWarnings("unchecked")
    public <T> RedisCache<T> getCache(String name, Class<T> type, RedisSerializer serializer) {
        return (RedisCache<T>) caches.computeIfAbsent(name,
                k -> new RedisCache<>(this, name, type, serializer));
    }

    // ==================== PUB/SUB ====================

    /**
     * Obtiene el manager de Pub/Sub
     */
    public RedisPubSubManager getPubSub() {
        return pubSubManager;
    }

    /**
     * Publica un mensaje en un canal
     */
    public void publish(String channel, String message) {
        pubSubManager.publish(channel, message);
    }

    /**
     * Publica un objeto serializado en un canal
     */
    public <T> void publishObject(String channel, T object) {
        String serialized = defaultSerializer.serialize(object);
        publish(channel, serialized);
    }

    // ==================== GETTERS ====================

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

    // ==================== MANTENIMIENTO ====================

    private void startMaintenanceTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!initialized) {
                    cancel();
                    return;
                }

                try {
                    // Limpiar caches caducadas
                    caches.values().forEach(RedisCache::cleanup);

                    // Verificar conexión
                    connectionManager.validateConnections();

                } catch (Exception e) {
                    logInternalError("Error en tarea de mantenimiento Redis: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60); // Cada minuto
    }
}