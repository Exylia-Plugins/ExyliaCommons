package net.exylia.commons.region.regeneration;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class RegionRegenerationManager {
    private static RegionRegenerationManager instance;

    private final JavaPlugin plugin;
    private final File schemsFolder;
    private final ConcurrentMap<String, Clipboard> clipboardCache;
    private final ConcurrentMap<String, Boolean> regeneratingRegions;

    // NUEVAS OPTIMIZACIONES PARA PASTE OPERATIONS
    private final Semaphore pasteOperationSemaphore;
    private final AtomicInteger activePasteOperations;
    private final Queue<PendingPasteOperation> pendingPasteQueue;
    private BukkitRunnable pasteProcessor;
    private BukkitRunnable cacheCleanupTask;

    private static final int MAX_CACHE_SIZE = 30;
    private static final boolean CACHE_ENABLED = true;
    private static final int MAX_CONCURRENT_PASTES = 4;
    private static final int PASTE_DELAY_TICKS = 3;
    private static final int CACHE_CLEANUP_INTERVAL_MINUTES = 10;
    private static final long CLIPBOARD_MAX_AGE_MINUTES = 30;
    private final ConcurrentMap<String, Long> clipboardCacheTimestamps;

    private RegionRegenerationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schemsFolder = new File(plugin.getDataFolder(), "schematics");
        this.clipboardCache = new ConcurrentHashMap<>();
        this.clipboardCacheTimestamps = new ConcurrentHashMap<>();
        this.regeneratingRegions = new ConcurrentHashMap<>();

        // Inicializar rate limiting para paste operations
        this.pasteOperationSemaphore = new Semaphore(MAX_CONCURRENT_PASTES);
        this.activePasteOperations = new AtomicInteger(0);
        this.pendingPasteQueue = new LinkedList<>();

        if (!schemsFolder.exists()) {
            boolean created = schemsFolder.mkdirs();
            if (created) {
                logInternalDebug("Directorio de schematics de regiones creado: " + schemsFolder.getPath());
            }
        }
        startPasteProcessor();
        startCacheCleanupTask();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionRegenerationManager(plugin);
        }
    }

    public static RegionRegenerationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionRegenerationManager no ha sido inicializado");
        }
        return instance;
    }

    private void startPasteProcessor() {
        pasteProcessor = new BukkitRunnable() {
            @Override
            public void run() {
                processNextPasteOperation();
            }
        };
        pasteProcessor.runTaskTimer(plugin, 1L, PASTE_DELAY_TICKS);
    }

    private void startCacheCleanupTask() {
        cacheCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredClipboards();
            }
        };
        cacheCleanupTask.runTaskTimerAsynchronously(plugin,
                20L * 60 * CACHE_CLEANUP_INTERVAL_MINUTES,
                20L * 60 * CACHE_CLEANUP_INTERVAL_MINUTES);
    }

    private void cleanupExpiredClipboards() {
        long currentTime = System.currentTimeMillis();
        long maxAge = CLIPBOARD_MAX_AGE_MINUTES * 60 * 1000;

        clipboardCacheTimestamps.entrySet().removeIf(entry -> {
            boolean expired = (currentTime - entry.getValue()) > maxAge;
            if (expired) {
                String regionId = entry.getKey();
                Clipboard removed = clipboardCache.remove(regionId);
                if (removed != null) {
                    logInternalDebug("Removed expired clipboard from cache: " + regionId);
                    try {
                        removed = null;
                    } catch (Exception e) {
                        DebugUtils.logInternalError("Error cleaning clipboard: " + e.getMessage());
                    }
                }
            }
            return expired;
        });

        // Ejecutar garbage collection si se limpiaron clipboards
        if (clipboardCache.size() < clipboardCacheTimestamps.size()) {
            System.gc();
        }
    }

    private void processNextPasteOperation() {
        if (pendingPasteQueue.isEmpty() || activePasteOperations.get() >= MAX_CONCURRENT_PASTES) {
            return;
        }

        PendingPasteOperation operation = pendingPasteQueue.poll();
        if (operation != null) {
            activePasteOperations.incrementAndGet();

            // Ejecutar paste de forma asíncrona
            CompletableFuture.runAsync(() -> {
                try {
                    boolean success = executePasteOperation(operation);
                    operation.future.complete(success);
                } catch (Exception e) {
                    operation.future.completeExceptionally(e);
                } finally {
                    activePasteOperations.decrementAndGet();
                }
            });
        }
    }

    private boolean executePasteOperation(PendingPasteOperation operation) {
        EditSession editSession = null;
        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(operation.targetLocation.getWorld());
            BlockVector3 pastePosition = BlockVector3.at(
                    operation.targetLocation.getBlockX(),
                    operation.targetLocation.getBlockY(),
                    operation.targetLocation.getBlockZ()
            );

            editSession = WorldEdit.getInstance().newEditSession(weWorld);
            editSession.setFastMode(true);
            editSession.setTickingWatchdog(false);

            ClipboardHolder holder = new ClipboardHolder(operation.clipboard);
            Operation pasteOperation = holder
                    .createPaste(editSession)
                    .to(pastePosition)
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(pasteOperation);

            logInternalDebug("Paste operation completed successfully for: " + operation.operationId);
            return true;

        } catch (WorldEditException e) {
            DebugUtils.logInternalError("WorldEdit error in paste operation " + operation.operationId + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            DebugUtils.logInternalError("Unexpected error in paste operation " + operation.operationId + ": " + e.getMessage());
            return false;
        } finally {
            if (editSession != null) {
                try {
                    editSession.close();
                    editSession = null;
                } catch (Exception e) {
                    DebugUtils.logInternalError("Error closing EditSession: " + e.getMessage());
                }
            }
            if (System.currentTimeMillis() % 10000 < 1000) {
                System.gc();
            }
        }
    }

    public CompletableFuture<Boolean> pasteRegionSchematicAt(Region sourceRegion, Location targetLocation) {
        if (!sourceRegion.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(sourceRegion);
        if (!schematicFile.exists()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Clipboard clipboard = clipboardCache.get(sourceRegion.getId());

                if (clipboard == null) {
                    clipboard = loadSchematicFromFile(schematicFile);
                    if (clipboard == null) {
                        return false;
                    }

                    if (CACHE_ENABLED) {
                        cacheClipboard(sourceRegion, clipboard);
                    }
                }
                CompletableFuture<Boolean> operationFuture = new CompletableFuture<>();
                PendingPasteOperation operation = new PendingPasteOperation(
                        "paste_" + sourceRegion.getId() + "_" + System.currentTimeMillis(),
                        clipboard,
                        targetLocation.clone(),
                        operationFuture
                );
                synchronized (pendingPasteQueue) {
                    pendingPasteQueue.offer(operation);
                }
                return operationFuture.join();

            } catch (Exception e) {
                DebugUtils.logInternalError("Error in pasteRegionSchematicAt: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        String regionId = region.getId();

        if (regeneratingRegions.containsKey(regionId)) {
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(region);
        if (!schematicFile.exists()) {
            return CompletableFuture.completedFuture(false);
        }

        regeneratingRegions.put(regionId, true);

        return CompletableFuture.supplyAsync(() -> {
            try {
                logInternalDebug("Regenerando región: " + region.getId());

                Clipboard clipboard = clipboardCache.get(regionId);

                if (clipboard == null) {
                    clipboard = loadSchematicFromFile(schematicFile);
                    if (clipboard == null) {
                        return false;
                    }

                    if (CACHE_ENABLED) {
                        cacheClipboard(region, clipboard);
                    }
                }

                return executeRegenerationOptimized(region, clipboard);

            } catch (Exception e) {
                DebugUtils.logInternalError("Error regenerando región " + region.getId() + ": " + e.getMessage());
                return false;
            } finally {
                regeneratingRegions.remove(regionId);
            }
        });
    }

    private boolean executeRegenerationOptimized(Region region, Clipboard clipboard) {
        try {
            cleanRegionEntities(region).join();
            PlayerBlockTracker.getInstance().clearRegionBlocks(region.getId());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Location targetLocation = region.getMinimumPoint();
            CompletableFuture<Boolean> pasteResult = pasteRegionSchematicAt(region, targetLocation);

            boolean success = pasteResult.join();
            if (success) {
                logInternalDebug("Región regenerada exitosamente: " + region.getId());
            } else {
                DebugUtils.logInternalError("Fallo en regeneración para región: " + region.getId());
            }

            return success;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error inesperado regenerando región " + region.getId() + ": " + e.getMessage());
            return false;
        }
    }
    public CompletableFuture<Integer> cleanRegionEntities(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    int removed = 0;
                    Location min = region.getMinimumPoint();
                    Location max = region.getMaximumPoint();

                    double minX = Math.min(min.getX(), max.getX());
                    double maxX = Math.max(min.getX(), max.getX());
                    double minY = Math.min(min.getY(), max.getY());
                    double maxY = Math.max(min.getY(), max.getY());
                    double minZ = Math.min(min.getZ(), max.getZ());
                    double maxZ = Math.max(min.getZ(), max.getZ());

                    for (Entity entity : min.getWorld().getEntities()) {
                        if (entity instanceof Player) continue;

                        Location loc = entity.getLocation();
                        if (loc.getX() >= minX && loc.getX() <= maxX &&
                                loc.getY() >= minY && loc.getY() <= maxY &&
                                loc.getZ() >= minZ && loc.getZ() <= maxZ) {

                            if (shouldRemoveEntity(entity)) {
                                entity.remove();
                                removed++;
                            }
                        }
                    }

                    logInternalDebug("Limpiadas " + removed + " entidades en región " + region.getId());
                    return removed;

                }).get();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error limpiando entidades en región " + region.getId() + ": " + e.getMessage());
                return 0;
            }
        });
    }

    private static class PendingPasteOperation {
        final String operationId;
        final Clipboard clipboard;
        final Location targetLocation;
        final CompletableFuture<Boolean> future;

        PendingPasteOperation(String operationId, Clipboard clipboard, Location targetLocation, CompletableFuture<Boolean> future) {
            this.operationId = operationId;
            this.clipboard = clipboard;
            this.targetLocation = targetLocation;
            this.future = future;
        }
    }

    public CompletableFuture<Boolean> saveRegionSchematic(Region region) {
        if (!region.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            EditSession editSession = null;
            Clipboard clipboard = null;
            try {
                logInternalDebug("Guardando schematic para región: " + region.getId());

                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(min.getWorld());
                BlockVector3 minVec = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
                BlockVector3 maxVec = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());

                CuboidRegion worldEditRegion = new CuboidRegion(weWorld, minVec, maxVec);

                editSession = WorldEdit.getInstance().newEditSession(weWorld);
                editSession.setFastMode(true);

                clipboard = new BlockArrayClipboard(worldEditRegion);
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, worldEditRegion, clipboard, minVec);
                copy.setCopyingEntities(false);
                Operations.complete(copy);

                File schematicFile = getSchematicFile(region);
                try (FileOutputStream fos = new FileOutputStream(schematicFile);
                     ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(fos)) {

                    writer.write(clipboard);

                    if (CACHE_ENABLED) {
                        cacheClipboard(region, clipboard);
                    }

                    logInternalDebug("Schematic guardado: " + schematicFile.getName());
                    return true;
                }

            } catch (Exception e) {
                DebugUtils.logInternalError("Error guardando schematic para región " + region.getId() + ": " + e.getMessage());
                return false;
            } finally {
                if (editSession != null) {
                    try {
                        editSession.close();
                        editSession = null;
                    } catch (Exception e) {
                        DebugUtils.logInternalError("Error closing EditSession in save: " + e.getMessage());
                    }
                }
            }
        });
    }

    public boolean hasSchematic(Region region) {
        return getSchematicFile(region).exists();
    }

    public boolean isRegenerating(Region region) {
        return regeneratingRegions.containsKey(region.getId());
    }

    public CompletableFuture<Boolean> deleteRegionSchematic(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            File schematicFile = getSchematicFile(region);
            String regionId = region.getId();

            clipboardCache.remove(regionId);

            if (schematicFile.exists()) {
                boolean deleted = schematicFile.delete();
                if (deleted) {
                    logInternalDebug("Schematic eliminado para región: " + region.getId());
                }
                return deleted;
            }

            return true;
        });
    }

    public RegenerationStats getStats() {
        return new RegenerationStats(
                clipboardCache.size(),
                regeneratingRegions.size(),
                schemsFolder.listFiles() != null ? schemsFolder.listFiles().length : 0,
                activePasteOperations.get(),
                pendingPasteQueue.size()
        );
    }

    public void clearCache() {
        int size = clipboardCache.size();
        for (Clipboard clipboard : clipboardCache.values()) {
            try {
                clipboard = null;
            } catch (Exception e) {
                DebugUtils.logInternalError("Error cleaning clipboard: " + e.getMessage());
            }
        }
        clipboardCache.clear();
        clipboardCacheTimestamps.clear();
        System.gc();

        logInternalDebug("Cache de clipboards limpiado: " + size + " elementos");
    }

    private Clipboard loadSchematicFromFile(File schematicFile) {
        try (FileInputStream fis = new FileInputStream(schematicFile);
             ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(fis)) {

            return reader.read();

        } catch (IOException e) {
            DebugUtils.logInternalError("Error cargando schematic " + schematicFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void cacheClipboard(Region region, Clipboard clipboard) {
        String regionId = region.getId();

        if (clipboardCache.size() >= MAX_CACHE_SIZE) {
            String oldestKey = clipboardCache.keySet().iterator().next();
            clipboardCache.remove(oldestKey);
        }
        clipboardCache.put(regionId, clipboard);
        clipboardCacheTimestamps.put(regionId, System.currentTimeMillis());
        logInternalDebug("Clipboard cacheado para región: " + region.getId());
    }

    public CompletableFuture<Integer> clearRegionPlayerBlocks(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            String regionId = region.getId();
            PlayerBlockTracker tracker = PlayerBlockTracker.getInstance();

            int blocksBefore = tracker.getPlayerBlocks(regionId).size();

            tracker.clearRegionBlocks(regionId);

            logInternalDebug("Limpiados " + blocksBefore + " bloques de jugador en región " + region.getId());
            return blocksBefore;
        });
    }

    private boolean shouldRemoveEntity(Entity entity) {
        EntityType type = entity.getType();
        return type != EntityType.PLAYER;
    }

    private File getSchematicFile(Region region) {
        return new File(schemsFolder, region.getId() + ".schem");
    }

    public void shutdown() {
        if (pasteProcessor != null) {
            pasteProcessor.cancel();
        }
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }
        if (!regeneratingRegions.isEmpty()) {
            DebugUtils.logInternalInfo("Esperando " + regeneratingRegions.size() + " regeneraciones...");
            int attempts = 0;
            while (!regeneratingRegions.isEmpty() && attempts < 10) {
                try {
                    Thread.sleep(500);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        for (PendingPasteOperation operation : pendingPasteQueue) {
            try {
                operation.future.cancel(true);
            } catch (Exception e) {
                DebugUtils.logInternalError("Error canceling paste operation: " + e.getMessage());
            }
        }
        clearCache();
        regeneratingRegions.clear();
        pendingPasteQueue.clear();
        System.gc();
    }

    @Getter
    public static class RegenerationStats {
        private final int cachedClipboards;
        private final int activeRegenerations;
        private final int totalSchematics;
        private final int activePasteOperations;
        private final int pendingPasteOperations;

        public RegenerationStats(int cachedClipboards, int activeRegenerations, int totalSchematics,
                                 int activePasteOperations, int pendingPasteOperations) {
            this.cachedClipboards = cachedClipboards;
            this.activeRegenerations = activeRegenerations;
            this.totalSchematics = totalSchematics;
            this.activePasteOperations = activePasteOperations;
            this.pendingPasteOperations = pendingPasteOperations;
        }
    }
    public CompletableFuture<Boolean> copyRegionStructure(Region sourceRegion, Location targetCenter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location sourceMin = sourceRegion.getMinimumPoint();
                Location sourceMax = sourceRegion.getMaximumPoint();

                Selection sourceSelection = sourceRegion.getSelection();
                Location sourceSelectionCenter = sourceSelection.getCenter();

                int offsetX = targetCenter.getBlockX() - sourceSelectionCenter.getBlockX();
                int offsetY = targetCenter.getBlockY() - sourceSelectionCenter.getBlockY();
                int offsetZ = targetCenter.getBlockZ() - sourceSelectionCenter.getBlockZ();

                Location targetMin = sourceMin.clone().add(offsetX, offsetY, offsetZ);
                targetMin.setWorld(targetCenter.getWorld());

                return copyAreaDirectly(sourceMin, sourceMax, targetMin);

            } catch (Exception e) {
                DebugUtils.logInternalError("Error copying region structure: " + e.getMessage());
                return false;
            }
        });
    }

    private boolean copyAreaDirectly(Location sourceMin, Location sourceMax, Location targetMin) {
        EditSession sourceSession = null;
        EditSession targetSession = null;
        Clipboard clipboard = null;
        try {
            com.sk89q.worldedit.world.World sourceWorld = BukkitAdapter.adapt(sourceMin.getWorld());
            com.sk89q.worldedit.world.World targetWorld = BukkitAdapter.adapt(targetMin.getWorld());

            BlockVector3 sourceMinVec = BlockVector3.at(sourceMin.getBlockX(), sourceMin.getBlockY(), sourceMin.getBlockZ());
            BlockVector3 sourceMaxVec = BlockVector3.at(sourceMax.getBlockX(), sourceMax.getBlockY(), sourceMax.getBlockZ());
            BlockVector3 targetMinVec = BlockVector3.at(targetMin.getBlockX(), targetMin.getBlockY(), targetMin.getBlockZ());

            CuboidRegion sourceRegion = new CuboidRegion(sourceWorld, sourceMinVec, sourceMaxVec);

            sourceSession = WorldEdit.getInstance().newEditSession(sourceWorld);
            targetSession = WorldEdit.getInstance().newEditSession(targetWorld);

            sourceSession.setFastMode(true);
            targetSession.setFastMode(true);

            clipboard = new BlockArrayClipboard(sourceRegion);
            ForwardExtentCopy copy = new ForwardExtentCopy(sourceSession, sourceRegion, clipboard, sourceMinVec);
            copy.setCopyingEntities(false);
            Operations.complete(copy);

            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation pasteOperation = holder
                    .createPaste(targetSession)
                    .to(targetMinVec)
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(pasteOperation);
            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("Unexpected error in direct copy: " + e.getMessage());
            return false;
        } finally {
            if (sourceSession != null) {
                try {
                    sourceSession.close();
                    sourceSession = null;
                } catch (Exception e) {
                    DebugUtils.logInternalError("Error closing source EditSession: " + e.getMessage());
                }
            }
            if (targetSession != null) {
                try {
                    targetSession.close();
                    targetSession = null;
                } catch (Exception e) {
                    DebugUtils.logInternalError("Error closing target EditSession: " + e.getMessage());
                }
            }
            if (clipboard != null) {
                try {
                    clipboard = null;
                } catch (Exception e) {
                    DebugUtils.logInternalError("Error cleaning clipboard: " + e.getMessage());
                }
            }
            System.gc();
        }
    }
}