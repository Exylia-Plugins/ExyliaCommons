package net.exylia.commons.databaseV2.core;

import lombok.Getter;
import net.exylia.commons.configSimple.Config;
import net.exylia.commons.databaseV2.adapter.DatabaseAdapter;
import net.exylia.commons.databaseV2.cache.CaffeineCacheStrategy;
import net.exylia.commons.databaseV2.cache.CacheKey;
import net.exylia.commons.databaseV2.cache.CacheStrategy;
import net.exylia.commons.databaseV2.config.DatabaseV2Config;
import net.exylia.commons.databaseV2.entity.Entity;
import net.exylia.commons.databaseV2.entity.EntityMetadata;
import net.exylia.commons.databaseV2.exception.ConnectionException;
import net.exylia.commons.databaseV2.repository.Repository;
import net.exylia.commons.databaseV2.repository.RepositoryImpl;
import net.exylia.commons.databaseV2.repository.RepositoryRegistry;
import net.exylia.commons.databaseV2.serialization.builtin.SerializerFactory;
import net.exylia.commons.utils.DebugUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
public class DatabaseV2Manager {

    private static DatabaseV2Manager instance;
    private static final Object LOCK = new Object();

    private final DatabaseV2Config config;
    private final DatabaseAdapter adapter;
    private final CacheStrategy<CacheKey, Object> cacheStrategy;
    private final RepositoryRegistry repositoryRegistry;
    private final Map<Class<?>, EntityMetadata> entityMetadataCache;

    private DatabaseV2Manager(DatabaseV2Config config, DatabaseAdapter adapter) {
        this.config = config;
        this.adapter = adapter;
        this.repositoryRegistry = RepositoryRegistry.getInstance();
        this.entityMetadataCache = new HashMap<>();
        this.cacheStrategy = createCacheStrategy();

        SerializerFactory.registerBuiltinSerializers();

        DebugUtils.logInfo("DatabaseV2Manager initialized with adapter: " + adapter.getAdapterName());
    }

    public static void initialize(Config configFile) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("DatabaseV2Manager already initialized");
            }

            try {
                DatabaseV2Config dbConfig = new DatabaseV2Config(configFile);
                DatabaseAdapter dbAdapter = createAdapter(dbConfig);
                dbAdapter.connect();
                instance = new DatabaseV2Manager(dbConfig, dbAdapter);
                DebugUtils.logInfo("DatabaseV2 initialized successfully");
            } catch (Exception e) {
                DebugUtils.logError("Failed to initialize DatabaseV2: " + e.getMessage());
                throw new ConnectionException("Failed to initialize DatabaseV2", e);
            }
        }
    }

    public static DatabaseV2Manager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseV2Manager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public <T extends Entity> void registerEntity(Class<T> entityClass) {
        try {
            EntityMetadata metadata = new EntityMetadata(entityClass);
            entityMetadataCache.put(entityClass, metadata);
            adapter.createTable(metadata);
            DebugUtils.logInfo("Entity registered: " + entityClass.getSimpleName());
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
                DebugUtils.logInfo("DatabaseV2Manager shutdown complete");
            } catch (Exception e) {
                DebugUtils.logError("Error during DatabaseV2Manager shutdown: " + e.getMessage());
            }
        }
    }

    private CacheStrategy<CacheKey, Object> createCacheStrategy() {
        DatabaseV2Config.CacheConfig cacheConfig = config.getCacheConfig();
        if (!cacheConfig.isEnabled()) {
            return new NoCacheStrategy<>();
        }

        return new CaffeineCacheStrategy<>(
                cacheConfig.getTtlMinutes(),
                cacheConfig.getMaxEntries(),
                cacheConfig.isRecordStats(),
                cacheConfig.isRefreshAfterAccess()
        );
    }

    private static DatabaseAdapter createAdapter(DatabaseV2Config config) throws Exception {
        String type = config.getDatabaseType().toUpperCase();
        return switch (type) {
            case "H2" -> new net.exylia.commons.databaseV2.adapter.sql.H2Adapter(config.getAdapterConfig("H2"));
            case "MYSQL" -> new net.exylia.commons.databaseV2.adapter.sql.MySQLAdapter(config.getAdapterConfig("MySQL"));
            case "MONGODB" -> new net.exylia.commons.databaseV2.adapter.mongo.MongoDBAdapter(config.getAdapterConfig("MongoDB"));
            case "YAML" -> new net.exylia.commons.databaseV2.adapter.fallback.YAMLFallbackAdapter(config.getAdapterConfig("H2"));
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
        public net.exylia.commons.databaseV2.cache.CacheStats getStats() {
            return new net.exylia.commons.databaseV2.cache.CacheStats(0, 0, 0, 0, 0);
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
