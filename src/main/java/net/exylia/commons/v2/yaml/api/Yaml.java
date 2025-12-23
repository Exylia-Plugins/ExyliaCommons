package net.exylia.commons.v2.yaml.api;

import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.yaml.config.YamlConfig;
import net.exylia.commons.v2.yaml.core.YamlManager;
import net.exylia.commons.v2.yaml.repository.YamlRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public final class Yaml {

    private Yaml() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(YamlConfig config) {
        YamlManager.initialize(config);
    }

    public static void initialize(Path baseDir) {
        YamlManager.initialize(YamlConfig.builder()
            .baseDir(baseDir)
            .build());
    }

    public static void initialize() {
        YamlManager.initialize(YamlConfig.builder()
            .baseDir(Paths.get("data"))
            .build());
    }

    public static <T extends Entity> void registerEntity(Class<T> entityClass) {
        YamlManager.getInstance().registerEntity(entityClass);
    }

    public static <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return YamlManager.getInstance().registerEntityAsync(entityClass);
    }

    public static <T extends Entity> YamlRepository<T> getRepository(Class<T> entityClass) {
        return YamlManager.getInstance().getRepository(entityClass);
    }

    public static void shutdown() {
        YamlManager.getInstance().shutdown();
    }

    public static YamlManager getManager() {
        return YamlManager.getInstance();
    }

    public static boolean isInitialized() {
        return YamlManager.isInitialized();
    }
}
