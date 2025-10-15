package net.exylia.commons.redis.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CustomRedisSerializer implements RedisSerializer {

    private final Map<Class<?>, Function<Object, String>> serializers;
    private final Map<Class<?>, Function<String, Object>> deserializers;
    private final RedisSerializer fallbackSerializer;

    public CustomRedisSerializer() {
        this(new GsonRedisSerializer());
    }

    public CustomRedisSerializer(RedisSerializer fallbackSerializer) {
        this.serializers = new HashMap<>();
        this.deserializers = new HashMap<>();
        this.fallbackSerializer = fallbackSerializer;
    }

    public <T> CustomRedisSerializer registerSerializer(Class<T> type,
                                                        Function<T, String> serializer,
                                                        Function<String, T> deserializer) {
        serializers.put(type, obj -> serializer.apply(type.cast(obj)));
        deserializers.put(type, data -> deserializer.apply(data));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> String serialize(T object) {
        if (object == null) {
            return null;
        }

        Class<?> type = object.getClass();
        Function<Object, String> serializer = serializers.get(type);

        if (serializer != null) {
            return serializer.apply(object);
        }

        for (Map.Entry<Class<?>, Function<Object, String>> entry : serializers.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                return entry.getValue().apply(object);
            }
        }

        return fallbackSerializer.serialize(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String data, Class<T> type) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        Function<String, Object> deserializer = deserializers.get(type);

        if (deserializer != null) {
            return (T) deserializer.apply(data);
        }

        for (Map.Entry<Class<?>, Function<String, Object>> entry : deserializers.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                return (T) entry.getValue().apply(data);
            }
        }

        return fallbackSerializer.deserialize(data, type);
    }

    @Override
    public boolean canSerialize(Class<?> type) {
        return serializers.containsKey(type) ||
                serializers.keySet().stream().anyMatch(key -> key.isAssignableFrom(type)) ||
                fallbackSerializer.canSerialize(type);
    }
}
