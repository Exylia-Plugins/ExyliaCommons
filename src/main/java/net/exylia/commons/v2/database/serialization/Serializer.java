package net.exylia.commons.v2.database.serialization;

public interface Serializer<T> {
    String serialize(T value);
}
