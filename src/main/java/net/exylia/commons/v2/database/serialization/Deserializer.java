package net.exylia.commons.v2.database.serialization;

public interface Deserializer<T> {
    T deserialize(String value, Class<T> type);
}
