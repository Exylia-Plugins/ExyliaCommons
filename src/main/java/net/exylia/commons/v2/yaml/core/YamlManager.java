package net.exylia.commons.v2.yaml.core;

import lombok.Getter;
import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.database.cache.CacheStrategy;
import net.exylia.commons.v2.database.cache.NoCacheStrategy;
import net.exylia.commons.v2.database.cache.CaffeineCacheStrategy;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.yaml.adapter.YamlStorageAdapter;
import net.exylia.commons.v2.yaml.config.YamlConfig;
import net.exylia.commons.v2.yaml.repository.YamlRepository;
import net.exylia.commons.v2.yaml.repository.YamlRepositoryImpl;
import net.exylia.commons.v2.yaml.repository.YamlRepositoryRegistry;

import java.util.concurrent.CompletableFuture;

@Getter
public class YamlManager {

    private static YamlManager instance;
    private static final Object LOCK = new Object();

    private final YamlConfig config;
    private final YamlStorageAdapter adapter;
    private final CacheStrategy<CacheKey, Object> cacheStrategy;
    private final YamlRepositoryRegistry repositoryRegistry;
    private final YamlEntityRegistry entityRegistry;

    private YamlManager(YamlConfig config) {
        this.config = config;
        this.adapter = new YamlStorageAdapter(config);
        this.repositoryRegistry = YamlRepositoryRegistry.getInstance();
        this.entityRegistry = YamlEntityRegistry.getInstance();
        this.cacheStrategy = createCacheStrategy();

        DebugAPI.logLibInfo(DebugCategory.GENERAL, "YamlManager initialized at: " + config.getBaseDir());
    }

    public static void initialize(YamlConfig config) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new YamlManager(config);
            }
        }
    }

    public static YamlManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("YamlManager not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public <T extends Entity> void registerEntity(Class<T> entityClass) {
        entityRegistry.registerEntity(entityClass);
        DebugAPI.logLibDebug(DebugCategory.GENERAL, "Entity registered: " + entityClass.getSimpleName());
    }

    public <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return CompletableFuture.runAsync(() -> registerEntity(entityClass));
    }

    public <T extends Entity> YamlRepository<T> getRepository(Class<T> entityClass) {
        if (!entityRegistry.hasEntity(entityClass)) {
            registerEntity(entityClass);
        }

        if (!repositoryRegistry.hasRepository(entityClass)) {
            EntityMetadata metadata = entityRegistry.getMetadata(entityClass);
            YamlRepository<T> repository = new YamlRepositoryImpl<>(
                entityClass,
                metadata,
                adapter,
                cacheStrategy
            );
            repositoryRegistry.registerRepository(entityClass, repository);
        }

        return repositoryRegistry.getRepository(entityClass);
    }

    public void shutdown() {
        DebugAPI.logLibInfo(DebugCategory.GENERAL, "YamlManager shutting down...");

        synchronized (LOCK) {
            instance = null;
        }

        DebugAPI.logLibInfo(DebugCategory.GENERAL, "YamlManager shutdown complete");
    }

    private CacheStrategy<CacheKey, Object> createCacheStrategy() {
        if (!config.isCacheEnabled()) {
            return new NoCacheStrategy<>();
        }

        return new CaffeineCacheStrategy<>(
            config.getCacheTtlMinutes(),
            config.getCacheMaxEntries(),
            config.isCacheRecordStats(),
            true
        );
    }
}
