package net.exylia.commons.v2.yaml.core;

import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YamlEntityRegistry {

    private static final YamlEntityRegistry instance = new YamlEntityRegistry();

    private final Map<Class<?>, EntityMetadata> entityMetadataCache = new ConcurrentHashMap<>();

    private YamlEntityRegistry() {
    }

    public static YamlEntityRegistry getInstance() {
        return instance;
    }

    public <T extends Entity> void registerEntity(Class<T> entityClass) {
        if (!entityMetadataCache.containsKey(entityClass)) {
            EntityMetadata metadata = new EntityMetadata(entityClass);
            entityMetadataCache.put(entityClass, metadata);
        }
    }

    public <T extends Entity> EntityMetadata getMetadata(Class<T> entityClass) {
        return entityMetadataCache.get(entityClass);
    }

    public boolean hasEntity(Class<?> entityClass) {
        return entityMetadataCache.containsKey(entityClass);
    }
}
