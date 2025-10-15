package net.exylia.commons.redis.serialization;

public interface RedisSerializer {

    <T> String serialize(T object);

    <T> T deserialize(String data, Class<T> type);

    default boolean canSerialize(Class<?> type) {
        return true;
    }
}
