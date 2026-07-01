package net.exylia.commons.v2.database.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.database.cache.CacheStats;
import net.exylia.commons.v2.database.cache.CacheStrategy;
import net.exylia.commons.v2.debug.api.DebugAPI;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class RedisCacheStrategy implements CacheStrategy<CacheKey, Object> {

    private final String entityNamespace;
    private final Cache<CacheKey, Object> localCache;
    private final RedisConnectionPool pool;
    private final RedisInvalidationBus bus;
    private final String redisKeyPrefix;
    private final int ttlSeconds;
    private final Map<String, Class<?>> entityClassByName;
    private final Gson gson;

    public RedisCacheStrategy(
            String entityNamespace,
            RedisConnectionPool pool,
            RedisInvalidationBus bus,
            RedisConfig config,
            int localMaxEntries,
            int localTtlMinutes,
            Map<String, Class<?>> entityClassByName
    ) {
        this.entityNamespace = entityNamespace;
        this.pool = pool;
        this.bus = bus;
        this.redisKeyPrefix = config.getKeyPrefix() + ":cache:";
        this.ttlSeconds = config.getTtlSeconds();
        this.entityClassByName = entityClassByName;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(localMaxEntries)
                .expireAfterWrite(Duration.ofMinutes(localTtlMinutes))
                .build();
        this.gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(UUID.class, new UUIDTypeAdapter())
                .create();

        bus.registerHandler(entityNamespace, this::onRemoteInvalidate, localCache::invalidateAll);
    }

    @Override
    public Object get(CacheKey key, Function<CacheKey, Object> loader) {
        Object local = localCache.getIfPresent(key);
        if (local != null) return local;

        String redisKey = toRedisKey(key);
        try {
            String json = pool.execute(jedis -> jedis.get(redisKey));
            if (json != null) {
                Object value = deserialize(json);
                if (value != null) {
                    localCache.put(key, value);
                    return value;
                }
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn("Redis L2 read failed [" + key + "]: " + e.getMessage());
        }

        Object value = loader.apply(key);
        if (value != null) {
            localCache.put(key, value);
            writeToRedis(redisKey, key, value);
        }
        return value;
    }

    @Override
    public void put(CacheKey key, Object value) {
        if (value == null) return;
        localCache.put(key, value);
        writeToRedis(toRedisKey(key), key, value);
    }

    @Override
    public void remove(CacheKey key) {
        invalidate(key);
    }

    @Override
    public void removeAll() {
        invalidateAll();
    }

    @Override
    public void invalidate(CacheKey key) {
        localCache.invalidate(key);
        try {
            pool.execute(jedis -> jedis.del(toRedisKey(key)));
        } catch (Exception e) {
            DebugAPI.logLibWarn("Redis delete failed [" + key + "]: " + e.getMessage());
        }
        bus.publishInvalidate(key);
    }

    @Override
    public void invalidateAll() {
        localCache.invalidateAll();
        deleteByPattern(redisKeyPrefix + entityNamespace + ":*");
        bus.publishClear(entityNamespace);
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(0, 0, 0, 0, localCache.estimatedSize());
    }

    @Override
    public long size() {
        return localCache.estimatedSize();
    }

    @Override
    public void clear() {
        invalidateAll();
    }

    private void onRemoteInvalidate(CacheKey key) {
        localCache.invalidate(key);
    }

    private String toRedisKey(CacheKey key) {
        return redisKeyPrefix + key.getFullKey();
    }

    private void writeToRedis(String redisKey, CacheKey key, Object value) {
        try {
            String json = serialize(value);
            if (json != null) {
                pool.execute(jedis -> jedis.setex(redisKey, ttlSeconds, json));
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn("Redis L2 write failed [" + key + "]: " + e.getMessage());
        }
    }

    private String serialize(Object value) {
        JsonObject wrapper = new JsonObject();
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return null;
            wrapper.addProperty("c", list.get(0).getClass().getSimpleName());
            wrapper.addProperty("l", true);
            wrapper.add("v", gson.toJsonTree(list));
        } else {
            wrapper.addProperty("c", value.getClass().getSimpleName());
            wrapper.addProperty("l", false);
            wrapper.add("v", gson.toJsonTree(value));
        }
        return gson.toJson(wrapper);
    }

    private Object deserialize(String json) {
        try {
            JsonObject wrapper = JsonParser.parseString(json).getAsJsonObject();
            String className = wrapper.get("c").getAsString();
            boolean isList = wrapper.get("l").getAsBoolean();
            JsonElement data = wrapper.get("v");

            Class<?> entityClass = entityClassByName.get(className);
            if (entityClass == null) return null;

            if (isList) {
                Type listType = TypeToken.getParameterized(List.class, entityClass).getType();
                return gson.fromJson(data, listType);
            } else {
                return gson.fromJson(data, entityClass);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            pool.execute(jedis -> {
                String cursor = "0";
                ScanParams params = new ScanParams().match(pattern).count(100);
                do {
                    ScanResult<String> result = jedis.scan(cursor, params);
                    cursor = result.getCursor();
                    List<String> keys = result.getResult();
                    if (!keys.isEmpty()) {
                        jedis.del(keys.toArray(new String[0]));
                    }
                } while (!"0".equals(cursor));
                return null;
            });
        } catch (Exception e) {
            DebugAPI.logLibWarn("Redis pattern delete failed [" + pattern + "]: " + e.getMessage());
        }
    }

    private static final class UUIDTypeAdapter extends TypeAdapter<UUID> {
        @Override
        public void write(JsonWriter out, UUID value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.toString());
        }

        @Override
        public UUID read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) { in.nextNull(); return null; }
            return UUID.fromString(in.nextString());
        }
    }
}
