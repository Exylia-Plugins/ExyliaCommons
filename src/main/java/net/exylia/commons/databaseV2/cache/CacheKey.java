package net.exylia.commons.databaseV2.cache;

import java.util.Objects;

public class CacheKey {

    private final String namespace;
    private final String key;

    public CacheKey(String namespace, String key) {
        this.namespace = namespace;
        this.key = key;
    }

    public static CacheKey of(String namespace, Object id) {
        return new CacheKey(namespace, String.valueOf(id));
    }

    public static CacheKey of(Class<?> entityClass, Object id) {
        return new CacheKey(entityClass.getSimpleName(), String.valueOf(id));
    }

    public static CacheKey of(String namespace, String fieldName, Object value) {
        return new CacheKey(namespace + ":" + fieldName, String.valueOf(value));
    }

    public String getFullKey() {
        return namespace + ":" + key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(namespace, cacheKey.namespace) &&
                Objects.equals(key, cacheKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, key);
    }

    @Override
    public String toString() {
        return getFullKey();
    }
}
