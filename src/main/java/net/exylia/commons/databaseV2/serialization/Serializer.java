package net.exylia.commons.databaseV2.serialization;

public interface Serializer<T> {
    String serialize(T value);
}
