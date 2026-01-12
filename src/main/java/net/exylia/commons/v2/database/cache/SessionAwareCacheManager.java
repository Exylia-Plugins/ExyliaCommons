package net.exylia.commons.v2.database.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.repository.Repository;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class SessionAwareCacheManager<K, V extends Entity> {

    private final Repository<V> repository;
    private final Function<K, V> defaultFactory;
    @Getter
    private final SessionCacheConfig config;
    private final AsyncLoadingCache<K, V> cache;
    private final ConcurrentHashMap<K, SessionMetadata> activeSessions;
    private ScheduledTask batchWriteTask;
    private ScheduledTask cleanupTask;
    private volatile boolean shutdown = false;
    private static final Logger LOGGER = Logger.getLogger(SessionAwareCacheManager.class.getName());

    public SessionAwareCacheManager(Repository<V> repository, Function<K, V> defaultFactory, SessionCacheConfig config) {
        this.repository = repository;
        this.defaultFactory = defaultFactory;
        this.config = config;
        this.activeSessions = new ConcurrentHashMap<>();
        this.cache = buildCache();
        startScheduledTasks();
    }

    private AsyncLoadingCache<K, V> buildCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize());

        if (config.getMode() == ExpirationMode.TIMED) {
            builder.expireAfterWrite(Duration.ofMinutes(config.getWriteExpirationMinutes()));
            builder.expireAfterAccess(Duration.ofMinutes(config.getAccessExpirationMinutes()));
        } else if (config.getMode() == ExpirationMode.ACTIVE) {
            builder.expireAfter(new com.github.benmanes.caffeine.cache.Expiry<K, V>() {
                @Override
                public long expireAfterCreate(K key, V value, long currentTime) {
                    return activeSessions.containsKey(key)
                        ? Long.MAX_VALUE
                        : TimeUnit.MINUTES.toNanos(config.getWriteExpirationMinutes());
                }

                @Override
                public long expireAfterUpdate(K key, V value, long currentTime, long currentDuration) {
                    return activeSessions.containsKey(key)
                        ? Long.MAX_VALUE
                        : TimeUnit.MINUTES.toNanos(config.getWriteExpirationMinutes());
                }

                @Override
                public long expireAfterRead(K key, V value, long currentTime, long currentDuration) {
                    return activeSessions.containsKey(key)
                        ? Long.MAX_VALUE
                        : TimeUnit.MINUTES.toNanos(config.getAccessExpirationMinutes());
                }
            });
        }

        if (config.isEnableStats()) {
            builder.recordStats();
        }

        return builder.buildAsync((key, executor) ->
            Schedulers.supplyAsyncDb(() -> loadFromDatabase(key))
        );
    }

    private V loadFromDatabase(K key) {
        try {
            Optional<V> result = repository.findById(key);
            return result.orElseGet(() -> defaultFactory.apply(key));
        } catch (Exception e) {
            LOGGER.severe("Failed to load entity from database for key: " + key + " - " + e.getMessage());
            return defaultFactory.apply(key);
        }
    }

    private void startScheduledTasks() {
        if (config.isEnableBatchWrite()) {
            long intervalMillis = config.getBatchWriteIntervalMinutes() * 60 * 1000;
            batchWriteTask = Schedulers.asyncTimer(
                this::flushDirtySessions,
                intervalMillis,
                intervalMillis,
                TimeUnit.MILLISECONDS
            );
        }

        long cleanupIntervalMillis = config.getCleanupIntervalMinutes() * 60 * 1000;
        cleanupTask = Schedulers.asyncTimer(
            this::cleanupOrphanedSessions,
            cleanupIntervalMillis,
            cleanupIntervalMillis,
            TimeUnit.MILLISECONDS
        );
    }

    public CompletableFuture<V> get(K key) {
        return cache.get(key);
    }

    public Optional<V> getSync(K key) {
        try {
            return Optional.ofNullable(cache.get(key).join());
        } catch (Exception e) {
            LOGGER.warning("Failed to get sync for key: " + key + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public V getIfPresent(K key) {
        return cache.synchronous().getIfPresent(key);
    }

    public CompletableFuture<V> save(K key, Consumer<V> modifier) {
        invalidate(key);
        return get(key).thenCompose(entity -> {
            modifier.accept(entity);
            entity.updateTimestamp();
            return Schedulers.supplyAsyncDb(() -> {
                repository.save(entity);
                return entity;
            });
        }).thenApply(entity -> {
            cache.synchronous().put(key, entity);
            return entity;
        });
    }

    public void invalidate(K key) {
        cache.synchronous().invalidate(key);
    }

    public void invalidateAll() {
        cache.synchronous().invalidateAll();
    }

    public void registerActiveSession(K key, SessionMetadata metadata) {
        activeSessions.put(key, metadata);
        if (config.getMode() == ExpirationMode.ACTIVE) {
            V cached = cache.synchronous().getIfPresent(key);
            if (cached != null) {
                cache.synchronous().put(key, cached);
            }
        }
    }

    public void unregisterActiveSession(K key) {
        SessionMetadata metadata = activeSessions.remove(key);
        if (metadata != null && config.isEnableBatchWrite() && metadata.isDirty()) {
            flushSession(key);
        }
    }

    public boolean isSessionActive(K key) {
        return activeSessions.containsKey(key);
    }

    public SessionMetadata getSessionMetadata(K key) {
        return activeSessions.get(key);
    }

    public void markSessionDirty(K key) {
        SessionMetadata metadata = activeSessions.get(key);
        if (metadata != null) {
            metadata.markDirty();
            metadata.updateActivity();
        }
    }

    public void warmup(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (K key : keys) {
            cache.get(key);
        }
    }

    private void flushDirtySessions() {
        if (shutdown) return;

        List<K> dirtyKeys = new ArrayList<>();
        for (Map.Entry<K, SessionMetadata> entry : activeSessions.entrySet()) {
            if (entry.getValue().isDirty()) {
                dirtyKeys.add(entry.getKey());
            }
        }

        if (!dirtyKeys.isEmpty()) {
            Schedulers.runAsyncDbTask(() -> {
                for (K key : dirtyKeys) {
                    try {
                        flushSession(key);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to flush session for key: " + key + " - " + e.getMessage());
                    }
                }
            });
        }
    }

    private void flushSession(K key) {
        V entity = cache.synchronous().getIfPresent(key);
        if (entity != null) {
            entity.updateTimestamp();
            repository.save(entity);
            SessionMetadata metadata = activeSessions.get(key);
            if (metadata != null) {
                metadata.markClean();
            }
        }
    }

    private void cleanupOrphanedSessions() {
        if (shutdown) return;

        long ttlMillis = config.getSessionTTLMinutes() * 60 * 1000;
        long now = System.currentTimeMillis();
        List<K> orphanedKeys = new ArrayList<>();

        for (Map.Entry<K, SessionMetadata> entry : activeSessions.entrySet()) {
            SessionMetadata metadata = entry.getValue();
            if (now - metadata.getLastActivityTime() > ttlMillis) {
                orphanedKeys.add(entry.getKey());
            }
        }

        for (K key : orphanedKeys) {
            LOGGER.info("Cleaning up orphaned session for key: " + key);
            unregisterActiveSession(key);
        }
    }

    public void reload() {
        invalidateAll();
    }

    public void shutdown() {
        shutdown = true;

        if (batchWriteTask != null) {
            batchWriteTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        if (config.isEnableBatchWrite()) {
            flushDirtySessions();
        }

        activeSessions.clear();
        cache.synchronous().invalidateAll();
    }

    public CacheStats getStats() {
        if (!config.isEnableStats()) {
            return new CacheStats(0, 0, 0, 0, cache.synchronous().estimatedSize());
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.synchronous().stats();
        return new CacheStats(
            caffeineStats.hitCount(),
            caffeineStats.missCount(),
            caffeineStats.evictionCount(),
            caffeineStats.loadCount(),
            cache.synchronous().estimatedSize()
        );
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public int getDirtySessionCount() {
        return (int) activeSessions.values().stream()
            .filter(SessionMetadata::isDirty)
            .count();
    }
}
