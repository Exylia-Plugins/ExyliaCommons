package net.exylia.commons.v2.hologram.core;

import lombok.Getter;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.hologram.cache.HologramCacheManager;
import net.exylia.commons.v2.hologram.exception.HologramException;
import net.exylia.commons.v2.hologram.listener.ChunkListener;
import net.exylia.commons.v2.hologram.listener.HologramListener;
import net.exylia.commons.v2.hologram.model.*;
import net.exylia.commons.v2.database.api.Database;
import net.exylia.commons.v2.database.repository.Repository;
import net.exylia.commons.v2.hologram.persistence.HologramEntity;
import net.exylia.commons.v2.hologram.update.UpdateScheduler;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;
import net.exylia.commons.v2.hologram.visibility.VisibilityManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HologramManager {
    private static HologramManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final JavaPlugin plugin;
    private final HologramRegistry registry;
    private final HologramCacheManager cacheManager;
    private final UpdateScheduler updateScheduler;
    private final VisibilityManager visibilityManager;
    private final HologramFactory factory;
    private Repository<HologramEntity> repository;

    private ScheduledTask cleanupTask;
    private ScheduledTask updateTask;

    private HologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registry = new HologramRegistry();
        this.cacheManager = new HologramCacheManager();
        this.visibilityManager = new VisibilityManager(cacheManager);
        this.factory = new HologramFactory(plugin);
        this.updateScheduler = new UpdateScheduler(registry, cacheManager);

        initializePersistence();
        registerListeners();
        startPeriodicTasks();

        DebugUtils.logInternalInfo("HologramManager initialized");
    }

    private void initializePersistence() {
        try {
            if (isDatabaseAvailable()) {
                Database.registerEntity(HologramEntity.class);
                this.repository = Database.getRepository(HologramEntity.class);
                loadPersistentHolograms();
                DebugUtils.logInternalInfo("Hologram persistence enabled");
            } else {
                DebugUtils.logInternalInfo("Hologram persistence disabled (Database not available)");
            }
        } catch (Exception e) {
            DebugUtils.logInternalError("Failed to initialize persistence: " + e.getMessage());
            this.repository = null;
        }
    }

    private boolean isDatabaseAvailable() {
        try {
            Class.forName("net.exylia.commons.v2.database.api.Database");
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    private void loadPersistentHolograms() {
        if (repository == null) {
            return;
        }

        repository.findAllAsync()
                .thenAccept(entities -> {
                    DebugUtils.logInternalInfo("Loading " + entities.size() + " persistent holograms...");

                    entities.forEach(entity -> {
                        try {
                            Hologram hologram = entity.toHologram(plugin);
                            registry.register(hologram);
                            cacheManager.cache(hologram);

                            SchedulerManager.getInstance()
                                    .task(hologram::spawn)
                                    .at(hologram.getLocation())
                                    .schedule();
                        } catch (Exception e) {
                            DebugUtils.logInternalError("Failed to load hologram " + entity.getId() + ": " + e.getMessage());
                        }
                    });

                    DebugUtils.logInternalSuccess("Loaded " + entities.size() + " persistent holograms");
                })
                .exceptionally(ex -> {
                    DebugUtils.logInternalError("Failed to load persistent holograms: " + ex.getMessage());
                    return null;
                });
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new HologramManager(plugin);
            }
        }
    }

    public static HologramManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HologramManager not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public CompletableFuture<Hologram> createHologramAsync(
            String id,
            Location location,
            List<String> lines,
            HologramProperties properties,
            HologramConfig config,
            boolean persistent,
            boolean perPlayer,
            VisibilityCondition visibilityCondition,
            double viewDistance
    ) {
        return AsyncExecutor.getInstance()
                .supplyAsync(() -> factory.create(
                        id, location, lines, properties, config,
                        persistent, perPlayer, visibilityCondition, viewDistance
                ), false)
                .thenCompose(hologram -> {
                    registry.register(hologram);
                    cacheManager.cache(hologram);

                    if (persistent) {
                        return saveHologramAsync(hologram)
                                .thenApply(v -> hologram);
                    }
                    return CompletableFuture.completedFuture(hologram);
                })
                .thenCompose(this::spawnHologramAsync)
                .whenComplete((hologram, ex) -> {
                    if (ex != null) {
                        DebugUtils.logInternalError("Failed to create hologram: " + ex.getMessage());
                    } else {
                        DebugUtils.logInternalInfo("Hologram created: " + id);
                    }
                });
    }

    private CompletableFuture<Hologram> spawnHologramAsync(Hologram hologram) {
        return CompletableFuture.supplyAsync(() -> {
            SchedulerManager.getInstance()
                    .task(() -> hologram.spawn())
                    .at(hologram.getLocation())
                    .schedule();
            return hologram;
        });
    }

    public CompletableFuture<Boolean> removeHologramAsync(String id) {
        return AsyncExecutor.getInstance().supplyAsync(() -> {
            Optional<Hologram> opt = registry.get(id);
            if (opt.isEmpty()) {
                return false;
            }

            Hologram hologram = opt.get();

            cacheManager.invalidate(id);

            SchedulerManager.getInstance()
                    .task(hologram::despawn)
                    .at(hologram.getLocation())
                    .run();

            registry.unregister(id);

            if (hologram.isPersistent()) {
                deleteHologramAsync(id).join();
            }

            DebugUtils.logInternalInfo("Hologram removed: " + id);
            return true;
        }, false);
    }

    public Optional<Hologram> getHologram(String id) {
        return cacheManager.get(id).or(() -> registry.get(id));
    }

    public Collection<Hologram> getAllHolograms() {
        return registry.getAll();
    }

    public List<Hologram> getHologramsNearby(Location location, double radius) {
        return cacheManager.getNearby(location, radius);
    }

    public void removeAllHolograms() {
        registry.getAll().forEach(hologram -> {
            SchedulerManager.getInstance()
                    .task(hologram::despawn)
                    .at(hologram.getLocation())
                    .run();
        });

        registry.clear();
        cacheManager.invalidateAll();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new HologramListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new ChunkListener(this), plugin);
    }

    private void startPeriodicTasks() {
        updateTask = SchedulerManager.getInstance()
                .task(() -> updateScheduler.updateAll())
                .async()
                .periodTicks(1)
                .schedule();

        cleanupTask = SchedulerManager.getInstance()
                .task(this::cleanup)
                .async()
                .periodTicks(20 * 60)
                .schedule();
    }

    private void cleanup() {
        registry.getAll().forEach(hologram -> {
            if (hologram.isPerPlayer()) {
                hologram.getPlayerDisplays().keySet().removeIf(playerId ->
                        Bukkit.getPlayer(playerId) == null);
            }
        });

        cacheManager.cleanup();
    }

    public void reload() {
        DebugUtils.logInternalInfo("Reloading HologramManager...");

        registry.getAll().forEach(hologram -> {
            SchedulerManager.getInstance()
                    .task(hologram::despawn)
                    .at(hologram.getLocation())
                    .run();
        });

        cacheManager.invalidateAll();
        registry.clear();

        DebugUtils.logInternalSuccess("HologramManager reloaded");
    }

    public void shutdown() {
        DebugUtils.logInternalInfo("Shutting down HologramManager...");

        if (updateTask != null) {
            updateTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        registry.getAll().forEach(hologram -> {
            SchedulerManager.getInstance()
                    .task(hologram::despawn)
                    .at(hologram.getLocation())
                    .run();
        });

        registry.clear();
        cacheManager.invalidateAll();

        synchronized (LOCK) {
            instance = null;
        }

        DebugUtils.logInternalInfo("HologramManager shutdown complete");
    }

    private CompletableFuture<Void> saveHologramAsync(Hologram hologram) {
        if (repository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return AsyncExecutor.getInstance()
                .runAsync(() -> {
                    HologramEntity entity = HologramEntity.fromHologram(hologram);
                    repository.save(entity);
                    DebugUtils.logInternalInfo("Hologram saved: " + hologram.getId());
                }, true)
                .exceptionally(ex -> {
                    DebugUtils.logInternalError("Failed to save hologram " + hologram.getId() + ": " + ex.getMessage());
                    return null;
                });
    }

    private CompletableFuture<Void> deleteHologramAsync(String id) {
        if (repository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return repository.findByIdAsync(id)
                .thenCompose(optEntity -> {
                    if (optEntity.isPresent()) {
                        return repository.deleteAsync(optEntity.get());
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenRun(() -> DebugUtils.logInternalInfo("Hologram deleted from DB: " + id))
                .exceptionally(ex -> {
                    DebugUtils.logInternalError("Failed to delete hologram " + id + ": " + ex.getMessage());
                    return null;
                });
    }

    public HologramRegistry getRegistry() {
        return registry;
    }

    public HologramCacheManager getCacheManager() {
        return cacheManager;
    }

    public VisibilityManager getVisibilityManager() {
        return visibilityManager;
    }
}
