package net.exylia.commons.v2.yaml.repository;

import net.exylia.commons.v2.database.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class YamlRepositoryRegistry {

    private static final YamlRepositoryRegistry instance = new YamlRepositoryRegistry();

    private final Map<Class<?>, YamlRepository<?>> repositories = new HashMap<>();

    private YamlRepositoryRegistry() {
    }

    public static YamlRepositoryRegistry getInstance() {
        return instance;
    }

    public <T extends Entity> YamlRepository<T> getRepository(Class<T> entityClass) {
        return (YamlRepository<T>) repositories.get(entityClass);
    }

    public <T extends Entity> void registerRepository(Class<T> entityClass, YamlRepository<T> repository) {
        repositories.put(entityClass, repository);
    }

    public boolean hasRepository(Class<?> entityClass) {
        return repositories.containsKey(entityClass);
    }
}
