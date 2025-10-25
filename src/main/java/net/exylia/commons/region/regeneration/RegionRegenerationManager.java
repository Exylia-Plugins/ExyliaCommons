package net.exylia.commons.region.regeneration;

import lombok.Getter;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.schematic.SchematicAPI;
import net.exylia.commons.region.schematic.SchematicFormat;
import net.exylia.commons.region.schematic.SchematicData;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class RegionRegenerationManager {
    private static RegionRegenerationManager instance;

    private final JavaPlugin plugin;

    private RegionRegenerationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        SchematicAPI.initialize(plugin);
        DebugUtils.logInternalInfo("RegionRegenerationManager initialized (using SchematicAPI backend)");
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

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        String regionId = region.getId();

        if (SchematicAPI.isWorking(regionId)) {
            logInternalDebug("Region already regenerating: " + regionId);
            return CompletableFuture.completedFuture(false);
        }

        return cleanRegionEntities(region)
            .thenCompose(v -> {
                PlayerBlockTracker.getInstance().clearRegionBlocks(regionId);

                if (!SchematicAPI.exists(regionId)) {
                    logInternalDebug("No schematic found for " + regionId + ", auto-generating...");
                    return SchematicAPI.saveRegion(regionId,
                        region.getMinimumPoint().getBlock(),
                        region.getMaximumPoint().getBlock())
                        .thenCompose(saved -> {
                            if (saved) {
                                return SchematicAPI.regenerate(regionId, region.getMinimumPoint().getBlock());
                            }
                            return CompletableFuture.completedFuture(false);
                        });
                }

                return SchematicAPI.regenerate(regionId, region.getMinimumPoint().getBlock());
            });
    }

    public CompletableFuture<List<Boolean>> regenerateMultipleRegions(List<Region> regions) {
        logInternalDebug("Batch regenerating " + regions.size() + " regions");

        List<String> regionIds = new ArrayList<>();
        for (Region region : regions) {
            if (SchematicAPI.exists(region.getId())) {
                regionIds.add(region.getId());
            }
        }

        if (regionIds.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        return SchematicAPI.regenerateMultiple(regionIds, regions.get(0).getMinimumPoint().getBlock());
    }

    public CompletableFuture<Boolean> pasteRegionSchematicAt(Region sourceRegion, Location targetLocation) {
        if (!sourceRegion.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        String regionId = sourceRegion.getId();
        if (!SchematicAPI.exists(regionId)) {
            return CompletableFuture.completedFuture(false);
        }

        return SchematicAPI.pasteRegion(regionId, targetLocation);
    }

    public CompletableFuture<Boolean> saveRegionSchematic(Region region) {
        if (!region.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        return SchematicAPI.saveRegion(region.getId(), region.getMinimumPoint().getBlock(), region.getMaximumPoint().getBlock());
    }

    public CompletableFuture<Integer> cleanRegionEntities(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                double minX = Math.min(min.getX(), max.getX());
                double maxX = Math.max(min.getX(), max.getX());
                double minY = Math.min(min.getY(), max.getY());
                double maxY = Math.max(min.getY(), max.getY());
                double minZ = Math.min(min.getZ(), max.getZ());
                double maxZ = Math.max(min.getZ(), max.getZ());

                final java.util.List<Entity> entitiesToRemove = new java.util.ArrayList<>();
                for (Entity entity : min.getWorld().getEntities()) {
                    if (entity instanceof Player) continue;

                    Location loc = entity.getLocation();
                    if (loc.getX() >= minX && loc.getX() <= maxX &&
                        loc.getY() >= minY && loc.getY() <= maxY &&
                        loc.getZ() >= minZ && loc.getZ() <= maxZ) {

                        if (shouldRemoveEntity(entity)) {
                            entitiesToRemove.add(entity);
                        }
                    }
                }

                final int[] removed = {0};
                net.exylia.commons.async.Schedulers.sync(() -> {
                    for (Entity entity : entitiesToRemove) {
                        try {
                            entity.remove();
                            removed[0]++;
                        } catch (Exception e) {
                            DebugUtils.logInternalError("Error removing entity: " + e.getMessage());
                        }
                    }
                });

                logInternalDebug("Cleaned " + removed[0] + " entities in region " + region.getId());
                return removed[0];

            } catch (Exception e) {
                DebugUtils.logInternalError("Error cleaning entities: " + e.getMessage());
                return 0;
            }
        });
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
        try {
            SchematicData data = new SchematicData(
                sourceMax.getBlockX() - sourceMin.getBlockX() + 1,
                sourceMax.getBlockY() - sourceMin.getBlockY() + 1,
                sourceMax.getBlockZ() - sourceMin.getBlockZ() + 1
            );

            SchematicFormat.captureRegion(data, sourceMin.getBlock(), sourceMax.getBlock());
            SchematicFormat.pasteRegion(data, targetMin.getBlock());

            return true;

        } catch (Exception e) {
            DebugUtils.logInternalError("Error in direct copy: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> deleteRegionSchematic(Region region) {
        return SchematicAPI.delete(region.getId());
    }

    public CompletableFuture<Integer> clearRegionPlayerBlocks(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            String regionId = region.getId();
            PlayerBlockTracker tracker = PlayerBlockTracker.getInstance();

            int blocksBefore = tracker.getPlayerBlocks(regionId).size();
            tracker.clearRegionBlocks(regionId);

            logInternalDebug("Cleared " + blocksBefore + " player blocks in region " + regionId);
            return blocksBefore;
        });
    }

    public boolean hasSchematic(Region region) {
        return SchematicAPI.exists(region.getId());
    }

    public boolean isRegenerating(Region region) {
        return SchematicAPI.isWorking(region.getId());
    }

    public RegenerationStats getStats() {
        var internalStats = SchematicAPI.getStats();
        return new RegenerationStats(
            internalStats.cachedSchematics,
            internalStats.activeOperations,
            0,
            internalStats.activeTasks,
            internalStats.pendingTasks,
            internalStats.successCount,
            internalStats.failureCount,
            internalStats.averageDuration,
            0.0
        );
    }

    public void clearCache() {
        SchematicAPI.clearCache();
        logInternalDebug("Schematic cache cleared");
    }

    private boolean shouldRemoveEntity(Entity entity) {
        return entity.getType() != EntityType.PLAYER;
    }

    public void shutdown() {
        SchematicAPI.shutdown();
        DebugUtils.logInternalInfo("RegionRegenerationManager shutdown complete");
    }

    @Getter
    public static class RegenerationStats {
        private final int cachedClipboards;
        private final int activeRegenerations;
        private final int totalSchematics;
        private final int activeTasks;
        private final int pendingTasks;
        private final long successCount;
        private final long failureCount;
        private final long averageDuration;
        private final double cacheHitRate;

        public RegenerationStats(int cachedClipboards, int activeRegenerations, int totalSchematics,
                                int activeTasks, int pendingTasks, long successCount, long failureCount,
                                long averageDuration, double cacheHitRate) {
            this.cachedClipboards = cachedClipboards;
            this.activeRegenerations = activeRegenerations;
            this.totalSchematics = totalSchematics;
            this.activeTasks = activeTasks;
            this.pendingTasks = pendingTasks;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.averageDuration = averageDuration;
            this.cacheHitRate = cacheHitRate;
        }
    }
}
