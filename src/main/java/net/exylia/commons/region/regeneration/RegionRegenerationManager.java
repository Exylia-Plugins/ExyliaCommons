package net.exylia.commons.region.regeneration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class RegionRegenerationManager {
    private static RegionRegenerationManager instance;

    private final JavaPlugin plugin;
    private final File schemsFolder;

    private final Cache<String, Clipboard> clipboardCache;
    private final ConcurrentMap<String, Boolean> regeneratingRegions;
    private final ExecutorService asyncExecutor;

    private ScheduledTask memoryMonitor;
    private final AtomicBoolean isShuttingDown;

    private RegionRegenerationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schemsFolder = new File(plugin.getDataFolder(), "schematics");
        this.clipboardCache = Caffeine.newBuilder()
                .maximumWeight(1_000_000_000)
                .weigher((key, value) -> estimateClipboardSize((Clipboard) value))
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener((key, value, cause) -> {
                    if (value instanceof Clipboard) {
                        logInternalDebug("Clipboard evicted from cache: " + key + " (reason: " + cause + ")");
                    }
                })
                .build();
        this.regeneratingRegions = new ConcurrentHashMap<>();
        this.isShuttingDown = new AtomicBoolean(false);

        this.asyncExecutor = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, "ExyliaRegionRegen-Async");
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
        );

        if (!schemsFolder.exists()) {
            schemsFolder.mkdirs();
            logInternalDebug("Schematics directory created: " + schemsFolder.getPath());
        }

        startMemoryMonitor();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionRegenerationManager(plugin);
        }
    }

    public static RegionRegenerationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionRegenerationManager not initialized");
        }
        return instance;
    }

    private void startMemoryMonitor() {
        memoryMonitor = Schedulers.asyncTimer(() -> {
            checkMemoryAndCleanup();
        }, 200, 200);
    }

    private void checkMemoryAndCleanup() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long freeMemoryMB = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 1024 / 1024;

        if (freeMemoryMB < 1024) {
            logInternalDebug("Memory pressure detected (" + freeMemoryMB + "MB free, " + usedMemoryMB + "MB used), clearing cache");
            clipboardCache.invalidateAll();
            System.gc();
        }
    }

    private int estimateClipboardSize(Clipboard clipboard) {
        if (clipboard == null) return 100;
        try {
            com.sk89q.worldedit.regions.Region region = clipboard.getRegion();
            if (region == null) return 100;

            long volume = region.getVolume();
            int estimatedSize = (int) Math.min(Integer.MAX_VALUE, volume * 12);
            return Math.max(100, estimatedSize);
        } catch (Exception e) {
            return 50_000_000;
        }
    }

    private Clipboard saveRegionToClipboard(com.sk89q.worldedit.world.World weWorld, CuboidRegion region, BlockVector3 minVec) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            editSession.setFastMode(true);

            Clipboard clipboard = new BlockArrayClipboard(region);
            ForwardExtentCopy copy = new ForwardExtentCopy(weWorld, region, clipboard, minVec);
            copy.setCopyingEntities(false);
            Operations.complete(copy);

            return clipboard;
        } catch (Exception e) {
            DebugUtils.logInternalError("Error saving region to clipboard: " + e.getMessage());
            return null;
        }
    }

    private boolean executePaste(Clipboard clipboard, Location targetLocation) {
        World world = targetLocation.getWorld();
        if (world == null) {
            return false;
        }

        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 pastePosition = BlockVector3.at(
                targetLocation.getBlockX(),
                targetLocation.getBlockY(),
                targetLocation.getBlockZ()
        );

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            editSession.setFastMode(true);
            editSession.setReorderMode(EditSession.ReorderMode.FAST);

            Operation pasteOperation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pastePosition)
                    .copyEntities(false)
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(pasteOperation);
            editSession.flushQueue();
            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error during paste: " + e.getMessage());
            return false;
        }
    }

    private Clipboard loadOrGetClipboard(Region region) {
        String regionId = region.getId();
        File schematicFile = getSchematicFile(region);

        if (!schematicFile.exists()) {
            DebugUtils.logInternalError("Schematic file not found: " + schematicFile.getName());
            return null;
        }

        Clipboard cached = clipboardCache.getIfPresent(regionId);
        if (cached != null) {
            logInternalDebug("Using cached clipboard for region: " + regionId);
            return cached;
        }

        Clipboard clipboard = loadSchematicFromFile(schematicFile);
        if (clipboard != null) {
            clipboardCache.put(regionId, clipboard);
        }

        return clipboard;
    }

    private Clipboard loadSchematicFromFile(File schematicFile) {
        try (FileInputStream fis = new FileInputStream(schematicFile);
             ClipboardReader reader = BuiltInClipboardFormat.FAST.getReader(fis)) {
            Clipboard clipboard = reader.read();
            logInternalDebug("Loaded schematic from file: " + schematicFile.getName());
            return clipboard;
        } catch (IOException e) {
            DebugUtils.logInternalError("Error loading schematic " + schematicFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<Void> preloadClipboards(List<Region> regions) {
        return CompletableFuture.runAsync(() -> {
            logInternalDebug("Preloading " + regions.size() + " clipboards...");
            int loaded = 0;
            for (Region region : regions) {
                if (loadOrGetClipboard(region) != null) {
                    loaded++;
                }
            }
            logInternalDebug("Preloaded " + loaded + "/" + regions.size() + " clipboards");
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        String regionId = region.getId();

        if (regeneratingRegions.containsKey(regionId)) {
            logInternalDebug("Region already regenerating: " + regionId);
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(region);
        if (!schematicFile.exists()) {
            DebugUtils.logInternalError("No schematic found for region: " + regionId);
            return CompletableFuture.completedFuture(false);
        }

        regeneratingRegions.put(regionId, true);
        logInternalDebug("Regeneration task queued for region: " + regionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                cleanRegionEntities(region).join();
                PlayerBlockTracker.getInstance().clearRegionBlocks(regionId);

                Clipboard clipboard = loadOrGetClipboard(region);
                if (clipboard == null) {
                    return false;
                }

                return executePaste(clipboard, region.getMinimumPoint());
            } catch (Exception e) {
                DebugUtils.logInternalError("Error during regeneration of " + regionId + ": " + e.getMessage());
                return false;
            } finally {
                regeneratingRegions.remove(regionId);
            }
        }, asyncExecutor);
    }

    public CompletableFuture<List<Boolean>> regenerateMultipleRegions(List<Region> regions) {
        logInternalDebug("Batch regenerating " + regions.size() + " regions");

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (Region region : regions) {
            futures.add(regenerateRegion(region));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    public CompletableFuture<Boolean> pasteRegionSchematicAt(Region sourceRegion, Location targetLocation) {
        if (!sourceRegion.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(sourceRegion);
        if (!schematicFile.exists()) {
            return CompletableFuture.completedFuture(false);
        }

        logInternalDebug("Paste task queued for region: " + sourceRegion.getId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                Clipboard clipboard = loadOrGetClipboard(sourceRegion);
                if (clipboard == null) {
                    return false;
                }
                return executePaste(clipboard, targetLocation);
            } catch (Exception e) {
                DebugUtils.logInternalError("Error during paste operation: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> saveRegionSchematic(Region region) {
        if (!region.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        logInternalDebug("Save task queued for region: " + region.getId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(min.getWorld());
                BlockVector3 minVec = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
                BlockVector3 maxVec = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());

                CuboidRegion worldEditRegion = new CuboidRegion(weWorld, minVec, maxVec);

                Clipboard clipboard = saveRegionToClipboard(weWorld, worldEditRegion, minVec);
                if (clipboard == null) {
                    return false;
                }

                clipboard.setOrigin(minVec);

                File schematicFile = getSchematicFile(region);
                try (FileOutputStream fos = new FileOutputStream(schematicFile);
                     ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(fos)) {
                    writer.write(clipboard);
                    clipboardCache.put(region.getId(), clipboard);
                    logInternalDebug("Schematic saved: " + schematicFile.getName());
                    return true;
                }
            } catch (Exception e) {
                DebugUtils.logInternalError("Error saving schematic: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Integer> cleanRegionEntities(Region region) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Schedulers.sync(() -> {
            try {
                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                Location center = min.clone().add(max.toVector().subtract(min.toVector()).multiply(0.5));
                double radiusX = Math.abs(max.getX() - min.getX()) / 2;
                double radiusY = Math.abs(max.getY() - min.getY()) / 2;
                double radiusZ = Math.abs(max.getZ() - min.getZ()) / 2;

                int removed = 0;
                for (Entity entity : center.getWorld().getNearbyEntities(center, radiusX, radiusY, radiusZ)) {
                    if (!(entity instanceof Player) && shouldRemoveEntity(entity)) {
                        entity.remove();
                        removed++;
                    }
                }

                logInternalDebug("Cleaned " + removed + " entities in region " + region.getId());
                future.complete(removed);
            } catch (Exception e) {
                DebugUtils.logInternalError("Error cleaning entities: " + e.getMessage());
                future.complete(0);
            }
        });

        return future;
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
        }, asyncExecutor);
    }

    private boolean copyAreaDirectly(Location sourceMin, Location sourceMax, Location targetMin) {
        BlockVector3 sourceMinVec = BlockVector3.at(sourceMin.getBlockX(), sourceMin.getBlockY(), sourceMin.getBlockZ());
        BlockVector3 sourceMaxVec = BlockVector3.at(sourceMax.getBlockX(), sourceMax.getBlockY(), sourceMax.getBlockZ());

        com.sk89q.worldedit.world.World sourceWorld = BukkitAdapter.adapt(sourceMin.getWorld());
        CuboidRegion sourceRegion = new CuboidRegion(sourceWorld, sourceMinVec, sourceMaxVec);

        Clipboard clipboard = null;
        try {
            clipboard = saveRegionToClipboard(sourceWorld, sourceRegion, sourceMinVec);
            if (clipboard == null) {
                return false;
            }

            boolean success = executePaste(clipboard, targetMin);
            logInternalDebug("Direct copy completed: " + success);
            return success;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error in direct copy: " + e.getMessage());
            return false;
        } finally {
            if (clipboard != null) {
                logInternalDebug("Releasing temporary clipboard from direct copy");
            }
        }
    }

    public CompletableFuture<Boolean> deleteRegionSchematic(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            File schematicFile = getSchematicFile(region);
            String regionId = region.getId();

            clipboardCache.invalidate(regionId);

            if (schematicFile.exists()) {
                boolean deleted = schematicFile.delete();
                if (deleted) {
                    logInternalDebug("Schematic deleted for region: " + regionId);
                }
                return deleted;
            }

            return true;
        }, asyncExecutor);
    }

    public CompletableFuture<Integer> clearRegionPlayerBlocks(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            String regionId = region.getId();
            PlayerBlockTracker tracker = PlayerBlockTracker.getInstance();

            int blocksBefore = tracker.getPlayerBlocks(regionId).size();
            tracker.clearRegionBlocks(regionId);

            logInternalDebug("Cleared " + blocksBefore + " player blocks in region " + regionId);
            return blocksBefore;
        }, asyncExecutor);
    }

    public boolean hasSchematic(Region region) {
        return getSchematicFile(region).exists();
    }

    public boolean isRegenerating(Region region) {
        return regeneratingRegions.containsKey(region.getId());
    }

    public RegenerationStats getStats() {
        return new RegenerationStats(
                (int) clipboardCache.estimatedSize(),
                regeneratingRegions.size(),
                schemsFolder.listFiles() != null ? schemsFolder.listFiles().length : 0
        );
    }

    public void clearCache() {
        long size = clipboardCache.estimatedSize();
        clipboardCache.invalidateAll();
        logInternalDebug("Clipboard cache cleared: " + size + " entries");
        System.gc();
    }

    private boolean shouldRemoveEntity(Entity entity) {
        return entity.getType() != EntityType.PLAYER;
    }

    private File getSchematicFile(Region region) {
        return new File(schemsFolder, region.getId() + ".schem");
    }

    public void shutdown() {
        isShuttingDown.set(true);

        if (memoryMonitor != null) {
            memoryMonitor.cancel();
        }

        if (!regeneratingRegions.isEmpty()) {
            DebugUtils.logInternalInfo("Waiting for " + regeneratingRegions.size() + " active regenerations...");
            int attempts = 0;
            while (!regeneratingRegions.isEmpty() && attempts < 20) {
                try {
                    Thread.sleep(500);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        asyncExecutor.shutdown();

        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clearCache();
        regeneratingRegions.clear();

        logInternalDebug("RegionRegenerationManager shutdown complete");
    }

    @Getter
    public static class RegenerationStats {
        private final int cachedClipboards;
        private final int activeRegenerations;
        private final int totalSchematics;

        public RegenerationStats(int cachedClipboards, int activeRegenerations, int totalSchematics) {
            this.cachedClipboards = cachedClipboards;
            this.activeRegenerations = activeRegenerations;
            this.totalSchematics = totalSchematics;
        }
    }
}