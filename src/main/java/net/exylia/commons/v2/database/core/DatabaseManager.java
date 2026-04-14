package net.exylia.commons.v2.database.core;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.cache.CaffeineCacheStrategy;
import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.database.cache.CacheStats;
import net.exylia.commons.v2.database.cache.CacheStrategy;
import net.exylia.commons.v2.database.config.DatabaseConfig;
import net.exylia.commons.v2.database.config.DatabaseDefaults;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.exception.ConnectionException;
import net.exylia.commons.v2.database.annotation.PlayerSession;
import net.exylia.commons.v2.database.redis.RedisConfig;
import net.exylia.commons.v2.database.redis.RedisConnectionPool;
import net.exylia.commons.v2.database.redis.RedisInvalidationBus;
import net.exylia.commons.v2.database.redis.RedisCacheStrategy;
import net.exylia.commons.v2.database.repository.Repository;
import net.exylia.commons.v2.database.repository.RepositoryImpl;
import net.exylia.commons.v2.database.repository.RepositoryRegistry;
import net.exylia.commons.v2.database.serialization.builtin.SerializerFactory;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Getter
public class DatabaseManager {

    private static DatabaseManager instance;
    private static final Object LOCK = new Object();

    private final DatabaseConfig config;
    private final DatabaseAdapter adapter;
    private final RepositoryRegistry repositoryRegistry;
    private final Map<Class<?>, EntityMetadata> entityMetadataCache;
    private final Map<String, Class<?>> entityClassByName;
    private final Set<Class<? extends Entity>> sessionEntities;
    private final org.bukkit.plugin.Plugin plugin;
    private volatile boolean sessionListenerRegistered = false;

    private final CacheStrategy<CacheKey, Object> localCacheStrategy;
    private RedisConnectionPool redisPool;
    private RedisInvalidationBus invalidationBus;
    private ConnectionHealthMonitor healthMonitor;

    private DatabaseManager(DatabaseConfig config, DatabaseAdapter adapter, org.bukkit.plugin.Plugin plugin) {
        this.config = config;
        this.adapter = adapter;
        this.plugin = plugin;
        this.repositoryRegistry = RepositoryRegistry.getInstance();
        this.entityMetadataCache = new HashMap<>();
        this.entityClassByName = new ConcurrentHashMap<>();
        this.sessionEntities = new LinkedHashSet<>();
        this.localCacheStrategy = createLocalCacheStrategy();

        initRedis(config.getRedisConfig());

        this.healthMonitor = new ConnectionHealthMonitor(adapter, entityMetadataCache);
        this.healthMonitor.start();

        SerializerFactory.registerBuiltinSerializers();
        DebugAPI.logLibInfo("DatabaseManager initialized with adapter: " + adapter.getAdapterName());
    }

    public static void initialize(net.exylia.commons.v2.config.Config configFile) {
        initialize(configFile, null);
    }

    public static void initialize(net.exylia.commons.v2.config.Config configFile, org.bukkit.plugin.Plugin plugin) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("DatabaseManager already initialized");
            }

            try {
                ConfigSchemaRegistry.ensureDefaults(DatabaseDefaults.class);
                DatabaseConfig dbConfig = new DatabaseConfig(configFile, plugin);
                DatabaseAdapter dbAdapter = createAdapter(dbConfig);
                dbAdapter.connect();
                instance = new DatabaseManager(dbConfig, dbAdapter, plugin);
            } catch (Exception e) {
                DebugAPI.logLibError("Failed to initialize Database: " + e.getMessage());
                throw new ConnectionException("Failed to initialize Database", e);
            }
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public <T extends Entity> void registerEntity(Class<T> entityClass) {
        try {
            EntityMetadata metadata = new EntityMetadata(entityClass);
            entityMetadataCache.put(entityClass, metadata);
            entityClassByName.put(entityClass.getSimpleName(), entityClass);
            adapter.createTable(metadata);
            adapter.updateTable(metadata);

            if (entityClass.isAnnotationPresent(PlayerSession.class)) {
                sessionEntities.add(entityClass);
                if (!sessionListenerRegistered && plugin != null) {
                    Bukkit.getPluginManager().registerEvents(new PlayerSessionFlushListener(), plugin);
                    sessionListenerRegistered = true;
                    DebugAPI.logLibInfo("PlayerSession auto-flush listener registered for " + entityClass.getSimpleName());
                }
            }
        } catch (Exception e) {
            throw new ConnectionException("Failed to register entity: " + entityClass.getName(), e);
        }
    }

    public <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return Tasks.dbRun(() -> registerEntity(entityClass));
    }

    public <T extends Entity> Repository<T> getRepository(Class<T> entityClass) {
        if (!repositoryRegistry.hasRepository(entityClass)) {
            EntityMetadata metadata = entityMetadataCache.get(entityClass);
            if (metadata == null) {
                throw new IllegalArgumentException("Entity not registered: " + entityClass.getName());
            }
            CacheStrategy<CacheKey, Object> strategy = buildCacheStrategy(entityClass);
            Repository<T> repo = new RepositoryImpl<>(entityClass, metadata, adapter, strategy);
            repositoryRegistry.registerRepository(entityClass, repo);
        }
        return repositoryRegistry.getRepository(entityClass);
    }

    public void shutdown() {
        synchronized (LOCK) {
            try {
                if (healthMonitor != null) healthMonitor.stop();
                if (localCacheStrategy != null) localCacheStrategy.clear();
                if (invalidationBus != null) invalidationBus.stop();
                if (redisPool != null) redisPool.close();
                if (adapter != null) adapter.disconnect();
                repositoryRegistry.clear();
                entityMetadataCache.clear();
                entityClassByName.clear();
                instance = null;
                DebugAPI.logLibInfo("DatabaseManager shutdown complete");
            } catch (Exception e) {
                DebugAPI.logLibError("Error during DatabaseManager shutdown: " + e.getMessage());
            }
        }
    }

    private void initRedis(RedisConfig redisConfig) {
        if (!redisConfig.isEnabled()) return;

        try {
            redisPool = new RedisConnectionPool(redisConfig);
            if (!redisPool.ping()) {
                DebugAPI.logLibWarn("Redis not reachable, falling back to local Caffeine cache");
                redisPool.close();
                redisPool = null;
                return;
            }
            invalidationBus = new RedisInvalidationBus(redisPool, redisConfig.getInvalidationChannel());
            invalidationBus.start();
            DebugAPI.logLibInfo("Redis cache enabled (" + redisConfig.getHost() + ":" + redisConfig.getPort() + ")");
        } catch (Exception e) {
            DebugAPI.logLibWarn("Failed to connect to Redis, using local cache: " + e.getMessage());
            if (redisPool != null) {
                redisPool.close();
                redisPool = null;
            }
            invalidationBus = null;
        }
    }

    private CacheStrategy<CacheKey, Object> buildCacheStrategy(Class<? extends Entity> entityClass) {
        if (redisPool != null) {
            return new RedisCacheStrategy(
                    entityClass.getSimpleName(),
                    redisPool,
                    invalidationBus,
                    config.getRedisConfig(),
                    DatabaseDefaults.Database.Cache.MAX_ENTRIES,
                    DatabaseDefaults.Database.Cache.TTL_MINUTES,
                    entityClassByName
            );
        }
        return localCacheStrategy;
    }

    private CacheStrategy<CacheKey, Object> createLocalCacheStrategy() {
        if (!DatabaseDefaults.Database.Cache.ENABLED) {
            return new NoCacheStrategy<>();
        }
        return new CaffeineCacheStrategy<>(
                DatabaseDefaults.Database.Cache.TTL_MINUTES,
                DatabaseDefaults.Database.Cache.MAX_ENTRIES,
                false,
                true
        );
    }

    private static DatabaseAdapter createAdapter(DatabaseConfig config) throws Exception {
        String type = DatabaseDefaults.Database.TYPE.toLowerCase();
        return switch (type) {
            case "h2" -> new net.exylia.commons.v2.database.adapter.sql.H2Adapter(config.getAdapterConfig("h2"));
            case "mysql" -> new net.exylia.commons.v2.database.adapter.sql.MySQLAdapter(config.getAdapterConfig("mysql"));
            case "mongodb" -> new net.exylia.commons.v2.database.adapter.mongo.MongoDBAdapter(config.getAdapterConfig("mongodb"));
            case "yaml" -> new net.exylia.commons.v2.database.adapter.fallback.YAMLFallbackAdapter(config.getAdapterConfig("h2"));
            default -> throw new IllegalArgumentException("Unknown database type: " + type);
        };
    }

    public Object resolvePlayerSessionId(Class<? extends Entity> entityClass, java.util.UUID playerUuid) {
        EntityMetadata metadata = entityMetadataCache.get(entityClass);
        if (metadata != null && metadata.getPrimaryKeyField() != null) {
            Class<?> pkType = metadata.getPrimaryKeyField().getType();
            if (java.util.UUID.class.isAssignableFrom(pkType)) {
                return playerUuid;
            }
        }
        return playerUuid.toString();
    }

    public Object resolvePlayerFieldValue(Class<? extends Entity> entityClass, String fieldName, java.util.UUID playerUuid) {
        EntityMetadata metadata = entityMetadataCache.get(entityClass);
        if (metadata != null && metadata.hasField(fieldName)) {
            Class<?> fieldType = metadata.getField(fieldName).getType();
            if (java.util.UUID.class.isAssignableFrom(fieldType)) {
                return playerUuid;
            }
        }
        return playerUuid.toString();
    }

    private class PlayerSessionFlushListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onQuit(PlayerQuitEvent event) {
            net.exylia.commons.v2.database.api.Database.flushPlayerSession(event.getPlayer().getUniqueId());
        }
    }

    private static class NoCacheStrategy<K, V> implements CacheStrategy<K, V> {
        @Override public V get(K key, Function<K, V> loader) { return loader.apply(key); }
        @Override public void put(K key, V value) {}
        @Override public void remove(K key) {}
        @Override public void removeAll() {}
        @Override public void invalidate(K key) {}
        @Override public void invalidateAll() {}
        @Override public CacheStats getStats() { return new CacheStats(0, 0, 0, 0, 0); }
        @Override public long size() { return 0; }
        @Override public void clear() {}
    }
}
