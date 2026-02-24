package net.exylia.commons.v2.database.serialization;

import lombok.Getter;
import net.exylia.commons.v2.database.exception.SerializationException;
import net.exylia.commons.v2.database.serialization.builtin.EnumSerializer;

import java.util.HashMap;
import java.util.Map;

public class SerializationRegistry {

    @Getter
    private static final SerializationRegistry instance = new SerializationRegistry();

    private final Map<Class<?>, Serializer<?>> serializers = new HashMap<>();
    private final Map<Class<?>, Deserializer<?>> deserializers = new HashMap<>();

    private SerializationRegistry() {
    }

    public <T> void registerSerializer(Class<T> type, Serializer<T> serializer) {
        serializers.put(type, serializer);
    }

    public <T> void registerDeserializer(Class<T> type, Deserializer<T> deserializer) {
        deserializers.put(type, deserializer);
    }

    public <T> void registerSerializationPair(Class<T> type, Serializer<T> serializer, Deserializer<T> deserializer) {
        registerSerializer(type, serializer);
        registerDeserializer(type, deserializer);
    }

    @SuppressWarnings("unchecked")
    public <T> String serialize(T value, Class<T> type) {
        if (value == null) {
            return null;
        }

        Serializer<T> serializer = (Serializer<T>) serializers.get(type);
        if (serializer == null) {
            if (type.isEnum()) {
                return EnumSerializer.INSTANCE.serialize((Enum<?>) value);
            }
            throw new SerializationException("No serializer registered for type " + type.getName());
        }

        return serializer.serialize(value);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialize(String value, Class<T> type) {
        if (value == null) {
            return null;
        }

        Deserializer<T> deserializer = (Deserializer<T>) deserializers.get(type);
        if (deserializer == null) {
            if (type.isEnum()) {
                try {
                    return (T) Enum.valueOf((Class<Enum>) type, value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            throw new SerializationException("No deserializer registered for type " + type.getName());
        }

        return deserializer.deserialize(value, type);
    }

    public <T> boolean hasSerializer(Class<T> type) {
        return serializers.containsKey(type) || type.isEnum();
    }

    public <T> boolean hasDeserializer(Class<T> type) {
        return deserializers.containsKey(type) || type.isEnum();
    }
}
