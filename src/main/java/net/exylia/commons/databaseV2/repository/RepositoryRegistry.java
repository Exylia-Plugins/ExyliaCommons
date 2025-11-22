package net.exylia.commons.databaseV2.repository;

import lombok.Getter;
import net.exylia.commons.databaseV2.entity.Entity;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RepositoryRegistry {

    private static final RepositoryRegistry instance = new RepositoryRegistry();
    private final Map<Class<?>, Repository<?>> repositories = new HashMap<>();

    private RepositoryRegistry() {
    }

    public static RepositoryRegistry getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Repository<T> getRepository(Class<T> entityClass) {
        return (Repository<T>) repositories.get(entityClass);
    }

    public <T extends Entity> void registerRepository(Class<T> entityClass, Repository<T> repository) {
        repositories.put(entityClass, repository);
    }

    public boolean hasRepository(Class<?> entityClass) {
        return repositories.containsKey(entityClass);
    }

    public void clear() {
        repositories.clear();
    }
}
