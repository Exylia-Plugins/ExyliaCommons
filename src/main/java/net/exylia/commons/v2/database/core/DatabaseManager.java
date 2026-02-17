package net.exylia.commons.v2.database.core;

import lombok.Getter;
import net.exylia.commons.v2.config.Config;
import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.cache.CaffeineCacheStrategy;
import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.database.cache.CacheStrategy;
import net.exylia.commons.v2.database.config.DatabaseConfig;
import net.exylia.commons.v2.database.config.DatabaseDefaults;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.exception.ConnectionException;
import net.exylia.commons.v2.database.repository.Repository;
import net.exylia.commons.v2.database.repository.RepositoryImpl;
import net.exylia.commons.v2.database.repository.RepositoryRegistry;
import net.exylia.commons.v2.database.serialization.builtin.SerializerFactory;
import net.exylia.commons.utils.DebugUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
public class DatabaseManager {

    private static DatabaseManager instance;
    private static final Object LOCK = new Object();

    private final DatabaseConfig config;
    private final DatabaseAdapter adapter;
    private final CacheStrategy<CacheKey, Object> cacheStrategy;
    private final RepositoryRegistry repositoryRegistry;
    private final Map<Class<?>, EntityMetadata> entityMetadataCache;

    private DatabaseManager(DatabaseConfig config, DatabaseAdapter adapter) {
        this.config = config;
        this.adapter = adapter;
        this.repositoryRegistry = RepositoryRegistry.getInstance();
        this.entityMetadataCache = new HashMap<>();
        this.cacheStrategy = createCacheStrategy();

        SerializerFactory.registerBuiltinSerializers();

        DebugUtils.logInternalInfo("DatabaseManager initialized with adapter: " + adapter.getAdapterName());
    }

    public static void initialize(Config configFile) {
        initialize(configFile, null);
    }

    public static void initialize(Config configFile, org.bukkit.plugin.Plugin plugin) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("DatabaseManager already initialized");
            }

            try {
                ConfigSchemaRegistry.ensureDefaults(DatabaseDefaults.class);
                DatabaseConfig dbConfig = new DatabaseConfig(configFile, plugin);
                DatabaseAdapter dbAdapter = createAdapter(dbConfig);
                dbAdapter.connect();
                instance = new DatabaseManager(dbConfig, dbAdapter);
                DebugUtils.logInternalInfo("Database initialized successfully");
            } catch (Exception e) {
                DebugUtils.logInternalError("Failed to initialize Database: " + e.getMessage());
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
            adapter.createTable(metadata);
            adapter.updateTable(metadata);
            DebugUtils.logInternalInfo("Entity registered: " + entityClass.getSimpleName());
        } catch (Exception e) {
            throw new ConnectionException("Failed to register entity: " + entityClass.getName(), e);
        }
    }

    public <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return CompletableFuture.runAsync(() -> registerEntity(entityClass));
    }

    public <T extends Entity> Repository<T> getRepository(Class<T> entityClass) {
        if (!repositoryRegistry.hasRepository(entityClass)) {
            EntityMetadata metadata = entityMetadataCache.get(entityClass);
            if (metadata == null) {
                throw new IllegalArgumentException("Entity not registered: " + entityClass.getName());
            }
            Repository<T> repo = new RepositoryImpl<>(entityClass, metadata, adapter, cacheStrategy);
            repositoryRegistry.registerRepository(entityClass, repo);
        }
        return repositoryRegistry.getRepository(entityClass);
    }

    public void shutdown() {
        synchronized (LOCK) {
            try {
                if (cacheStrategy != null) {
                    cacheStrategy.clear();
                }
                if (adapter != null) {
                    adapter.disconnect();
                }
                repositoryRegistry.clear();
                entityMetadataCache.clear();
                instance = null;
                DebugUtils.logInternalInfo("DatabaseManager shutdown complete");
            } catch (Exception e) {
                DebugUtils.logInternalError("Error during DatabaseManager shutdown: " + e.getMessage());
            }
        }
    }

    private CacheStrategy<CacheKey, Object> createCacheStrategy() {
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

    private static class NoCacheStrategy<K, V> implements CacheStrategy<K, V> {
        @Override
        public V get(K key, java.util.function.Function<K, V> loader) {
            return loader.apply(key);
        }

        @Override
        public void put(K key, V value) {
        }

        @Override
        public void remove(K key) {
        }

        @Override
        public void removeAll() {
        }

        @Override
        public void invalidate(K key) {
        }

        @Override
        public void invalidateAll() {
        }

        @Override
        public net.exylia.commons.v2.database.cache.CacheStats getStats() {
            return new net.exylia.commons.v2.database.cache.CacheStats(0, 0, 0, 0, 0);
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public void clear() {
        }
    }
}
