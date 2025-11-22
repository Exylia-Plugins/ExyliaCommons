package net.exylia.commons.databaseV2.api;

import net.exylia.commons.configSimple.Config;
import net.exylia.commons.configSimple.Configs;
import net.exylia.commons.databaseV2.core.DatabaseV2Manager;
import net.exylia.commons.databaseV2.entity.Entity;
import net.exylia.commons.databaseV2.repository.Repository;

import java.util.concurrent.CompletableFuture;

public final class DatabaseV2 {

    private DatabaseV2() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(Config config) {
        DatabaseV2Manager.initialize(config);
    }

    public static void initialize() {
        DatabaseV2Manager.initialize(Configs.get("database-v2"));
    }

    public static <T extends Entity> void registerEntity(Class<T> entityClass) {
        DatabaseV2Manager.getInstance().registerEntity(entityClass);
    }

    public static <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return DatabaseV2Manager.getInstance().registerEntityAsync(entityClass);
    }

    public static <T extends Entity> Repository<T> getRepository(Class<T> entityClass) {
        return DatabaseV2Manager.getInstance().getRepository(entityClass);
    }

    public static void shutdown() {
        DatabaseV2Manager.getInstance().shutdown();
    }

    public static DatabaseV2Manager getManager() {
        return DatabaseV2Manager.getInstance();
    }
}
