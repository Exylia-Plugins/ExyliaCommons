package net.exylia.commons.v2.database.api;

import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.database.core.DatabaseManager;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.repository.Repository;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public final class Database {

    private Database() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(Plugin plugin) {
        DatabaseManager.initialize(Configs.get("database"), plugin);
    }

    public static <T extends Entity> void registerEntity(Class<T> entityClass) {
        DatabaseManager.getInstance().registerEntity(entityClass);
    }

    public static <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return DatabaseManager.getInstance().registerEntityAsync(entityClass);
    }

    public static <T extends Entity> Repository<T> getRepository(Class<T> entityClass) {
        return DatabaseManager.getInstance().getRepository(entityClass);
    }

    public static void shutdown() {
        DatabaseManager.getInstance().shutdown();
    }

    public static DatabaseManager getManager() {
        return DatabaseManager.getInstance();
    }
}
