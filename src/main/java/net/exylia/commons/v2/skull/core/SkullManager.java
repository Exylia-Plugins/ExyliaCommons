package net.exylia.commons.v2.skull.core;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.skull.config.SkullConfig;
import net.exylia.commons.v2.skull.fetcher.MojangFetcher;
import net.exylia.commons.v2.skull.fetcher.TextureFetcher;
import net.exylia.commons.v2.skull.persistence.SkullPersistence;
import net.exylia.commons.v2.skull.renderer.SkullFactory;
import net.exylia.commons.v2.skull.renderer.SkullRenderer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkullManager {

    private static SkullManager instance;

    @Getter
    private final SkullConfig config;

    @Getter
    private final SkullCache cache;

    @Getter
    private final SkullExecutor executor;

    @Getter
    private final SkullRenderer renderer;

    @Getter
    private final SkullPersistence persistence;

    private final SkullFactory factory;
    private final MojangFetcher mojangFetcher;
    private final TextureFetcher textureFetcher;

    private volatile boolean initialized = false;

    private SkullManager(SkullConfig config) {
        this.config = config;
        this.cache = new SkullCache(config);
        this.executor = new SkullExecutor(config);
        this.factory = new SkullFactory();
        this.mojangFetcher = new MojangFetcher(config, cache);
        this.persistence = config.getDataFolder() != null
                ? new SkullPersistence(config.getDataFolder(), config.getPersistentCacheTtl())
                : null;
        this.textureFetcher = new TextureFetcher(mojangFetcher, executor, cache, config, persistence);
        this.renderer = new SkullRenderer(factory, textureFetcher, cache, executor, config);
    }

    public static void initialize() {
        initialize(new SkullConfig());
    }

    public static void initialize(SkullConfig config) {
        if (instance != null) {
            DebugAPI.logLibWarn(DebugCategory.SKULL, "SkullManager already initialized");
            return;
        }
        instance = new SkullManager(config);
        instance.start();
    }

    public static SkullManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SkullManager not initialized! Call initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }

    private void start() {
        if (initialized) {
            return;
        }

        if (persistence != null) {
            persistence.load();
        }

        executor.scheduleCleanup(() -> cache.clearAll());
        initialized = true;
    }

    public void reload() {
        if (!initialized) {
            throw new IllegalStateException("SkullManager not initialized");
        }

        DebugAPI.logLibInfo(DebugCategory.SKULL, "Reloading SkullManager");
        cache.clearAll();
        DebugAPI.logLibSuccess(DebugCategory.SKULL, "SkullManager reloaded successfully");
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        DebugAPI.logLibInfo(DebugCategory.SKULL, "Shutting down SkullManager");

        if (persistence != null) {
            persistence.save();
        }

        executor.shutdown();
        cache.clearAll();
        initialized = false;
        instance = null;
        DebugAPI.logLibSuccess(DebugCategory.SKULL, "SkullManager shutdown completed");
    }

    public void preloadPlayers(String... playerNames) {
        if (playerNames == null || playerNames.length == 0) {
            return;
        }

        if (cache.isRateLimited()) {
            DebugAPI.logLibWarn(DebugCategory.SKULL, "Preload skipped: rate limited");
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Preloading " + playerNames.length + " player skulls");
        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name != null && !name.isEmpty()) {
                int delay = i * config.getPreloadDelay();
                long delayTicks = delay / 50L;
                Tasks.build()
                        .run(() -> renderer.renderPlayerAsync(name))
                        .delay(delayTicks * 50, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .async()
                        .schedule();
            }
        }
    }

    public CompletableFuture<List<ItemStack>> batchPlayers(String... playerNames) {
        if (playerNames == null || playerNames.length == 0) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<ItemStack>> futures = new ArrayList<>();

        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name != null && !name.isEmpty()) {
                final int delay = i * config.getBatchDelay();

                CompletableFuture<ItemStack> delayed = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }).thenCompose(ignored -> renderer.renderPlayerAsync(name));

                futures.add(delayed);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    public boolean isPlayerCached(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        String key = playerName.toLowerCase();
        return cache.getPlayer(key) != null;
    }

    public String getStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Skull System Stats:\n");
        stats.append("Cache: ").append(cache.getStats()).append("\n");
        stats.append("Executor: ").append(executor.getStats()).append("\n");
        if (persistence != null) {
            stats.append("Persistent Cache: ").append(persistence.size()).append(" players\n");
        }
        stats.append("Initialized: ").append(initialized);
        return stats.toString();
    }
}
