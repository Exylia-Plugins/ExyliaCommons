package net.exylia.commons.v2.hologram.core;

import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.hologram.cache.HologramCacheManager;
import net.exylia.commons.v2.hologram.exception.HologramException;
import net.exylia.commons.v2.hologram.listener.ChunkListener;
import net.exylia.commons.v2.hologram.listener.HologramListener;
import net.exylia.commons.v2.hologram.model.*;
import net.exylia.commons.v2.yaml.api.Yaml;
import net.exylia.commons.v2.yaml.repository.YamlRepository;
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
import java.util.Set;
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
    private YamlRepository<HologramEntity> repository;

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

        DebugAPI.logLibInfo("HologramManager initialized");
    }

    private void initializePersistence() {
        try {
            if (isYamlAvailable()) {
                Yaml.registerEntity(HologramEntity.class);
                this.repository = Yaml.getRepository(HologramEntity.class);
                loadPersistentHolograms();
                DebugAPI.logLibInfo("Hologram persistence enabled (YAML)");
            } else {
                DebugAPI.logLibInfo("Hologram persistence disabled (YAML not available)");
            }
        } catch (Exception e) {
            DebugAPI.logLibError("Failed to initialize persistence: " + e.getMessage());
            this.repository = null;
        }
    }

    private boolean isYamlAvailable() {
        try {
            Class.forName("net.exylia.commons.v2.yaml.api.Yaml");
            return Yaml.isInitialized();
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
                    DebugAPI.logLibInfo("Loading " + entities.size() + " persistent holograms...");

                    entities.forEach(entity -> {
                        try {
                            Hologram hologram = entity.toHologram(plugin);
                            registry.register(hologram);
                            cacheManager.cache(hologram);
                            visibilityManager.getSpatialChunkManager().addHologram(hologram);
                            updateScheduler.scheduleUpdate(hologram);

                            Tasks.at(hologram.getLocation(), hologram::spawn);
                        } catch (Exception e) {
                            DebugAPI.logLibError("Failed to load hologram " + entity.getId() + ": " + e.getMessage());
                        }
                    });

                    DebugAPI.logLibInfo("Loaded " + entities.size() + " persistent holograms");
                })
                .exceptionally(ex -> {
                    DebugAPI.logLibError("Failed to load persistent holograms: " + ex.getMessage());
                    return null;
                });
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new HologramManager(plugin);
                Yaml.initialize();
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
            double viewDistance,
            boolean enabled,
            net.exylia.commons.v2.placeholders.context.PlaceholderContext placeholderContext
    ) {
        return Tasks.run(() -> factory.create(
                        id, location, lines, properties, config,
                        persistent, perPlayer, visibilityCondition, viewDistance, enabled, placeholderContext
                )).thenApply(r -> r.getValue().orElse(null))
                .thenCompose(hologram -> {
                    registry.register(hologram);
                    cacheManager.cache(hologram);
                    visibilityManager.getSpatialChunkManager().addHologram(hologram);
                    updateScheduler.scheduleUpdate(hologram);

                    if (persistent) {
                        return saveHologramAsync(hologram)
                                .thenApply(v -> hologram);
                    }
                    return CompletableFuture.completedFuture(hologram);
                })
                .thenCompose(this::spawnHologramAsync)
                .whenComplete((hologram, ex) -> {
                    if (ex != null) {
                        DebugAPI.logLibError("Failed to create hologram: " + ex.getMessage());
                    } else {
                        DebugAPI.logLibDebug("Hologram created: " + id);
                    }
                });
    }

    private CompletableFuture<Hologram> spawnHologramAsync(Hologram hologram) {
        return CompletableFuture.supplyAsync(() -> {
            Tasks.at(hologram.getLocation(), hologram::spawn);
            return hologram;
        });
    }

    public CompletableFuture<Boolean> removeHologramAsync(String id) {
        final boolean[] persistent = {false};

        return Tasks.run(() -> {
            Optional<Hologram> opt = registry.get(id);
            if (opt.isEmpty()) return false;

            Hologram hologram = opt.get();
            persistent[0] = hologram.isPersistent();

            cacheManager.invalidate(id);
            visibilityManager.getSpatialChunkManager().removeHologram(hologram);
            updateScheduler.unscheduleUpdate(id);
            Tasks.at(hologram.getLocation(), hologram::despawn);
            registry.unregister(id);

            DebugAPI.logLibDebug("Hologram removed: " + id);
            return true;
        }).thenCompose(r -> {
            boolean removed = r.getValue().orElse(false);
            if (!removed) return CompletableFuture.completedFuture(false);
            if (persistent[0]) {
                return deleteHologramAsync(id).thenApply(v -> true);
            }
            return CompletableFuture.completedFuture(true);
        });
    }

    public Optional<Hologram> getHologram(String id) {
        return cacheManager.get(id).or(() -> registry.get(id));
    }

    public Collection<Hologram> getAllHolograms() {
        return registry.getAll();
    }

    public List<Hologram> getHologramsNearby(Location location, double radius) {
        Set<String> nearbyIds = visibilityManager.getSpatialChunkManager()
                .getNearbyHologramIds(location, radius);

        return nearbyIds.stream()
                .map(this::getHologram)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void removeAllHolograms() {
        registry.getAll().forEach(hologram -> {
            Tasks.at(hologram.getLocation(), hologram::despawn);
        });

        registry.clear();
        cacheManager.invalidateAll();
    }

    public void removeAllHologramsSync() {
        registry.getAll().forEach(Hologram::despawn);
        registry.clear();
        cacheManager.invalidateAll();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new HologramListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new ChunkListener(this), plugin);
    }

    private void startPeriodicTasks() {
        updateTask = Tasks.asyncTimer(() -> updateScheduler.updateAll(), 0L, 1L);

        cleanupTask = Tasks.asyncTimer(this::cleanup, 0L, 20L * 10);
    }

    private void cleanup() {
        registry.getAll().forEach(hologram ->
                hologram.getPlayerViewerIds().forEach(playerId -> {
                    if (Bukkit.getPlayer(playerId) == null) {
                        hologram.cleanupPlayer(playerId);
                    }
                }));

        cacheManager.cleanup();
    }

    public void reload() {
        DebugAPI.logLibInfo("Reloading HologramManager...");

        registry.getAll().forEach(hologram ->
                Tasks.at(hologram.getLocation(), () -> {
                    hologram.despawn();
                    hologram.spawn();
                })
        );

        cacheManager.invalidateAll();
        registry.getAll().forEach(cacheManager::cache);

        DebugAPI.logLibInfo("HologramManager reloaded");
    }

    public void shutdown() {
        shutdown(false);
    }

    public void shutdown(boolean isServerShutdown) {
        DebugAPI.logLibInfo("Shutting down HologramManager...");

        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        if (!isServerShutdown) {
            registry.getAll().forEach(hologram -> {
                try {
                    hologram.despawn();
                } catch (Exception e) {
                    DebugAPI.logLibError("Error despawning hologram " + hologram.getId() + ": " + e.getMessage());
                }
            });
        }

        updateScheduler.clear();
        visibilityManager.getDistanceTracker().clear();
        visibilityManager.getSpatialChunkManager().clear();

        registry.clear();
        cacheManager.invalidateAll();

        synchronized (LOCK) {
            instance = null;
        }

        DebugAPI.logLibInfo("HologramManager shutdown complete");
    }

    private CompletableFuture<Void> saveHologramAsync(Hologram hologram) {
        if (repository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return Tasks.db(() -> {
                    HologramEntity entity = HologramEntity.fromHologram(hologram);
                    repository.save(entity);
                    DebugAPI.logLibInfo("Hologram saved: " + hologram.getId());
                    return null;
                })
                .thenApply(r -> (Void) null)
                .exceptionally(ex -> {
                    DebugAPI.logLibError("Failed to save hologram " + hologram.getId() + ": " + ex.getMessage());
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
                .thenRun(() -> DebugAPI.logLibInfo("Hologram deleted from YAML: " + id))
                .exceptionally(ex -> {
                    DebugAPI.logLibError("Failed to delete hologram " + id + ": " + ex.getMessage());
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
