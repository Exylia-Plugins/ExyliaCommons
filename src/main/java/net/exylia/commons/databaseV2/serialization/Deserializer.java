package net.exylia.commons.databaseV2.serialization;

public interface Deserializer<T> {
    T deserialize(String value, Class<T> type);
}
