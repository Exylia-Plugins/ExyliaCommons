package net.exylia.commons.v2.region.schematic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


final class CustomSchematicEngine {

    private final File folder;
    private final Cache<String, CustomSchematic> loadedSchematics;

    CustomSchematicEngine(File folder) {
        this.folder = folder;
        this.loadedSchematics = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    CompletableFuture<Boolean> save(Region region, String schematicName, SchematicManager.SchematicType type, Consumer<Float> onProgress) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Location sourceMin = region.getMinimumPoint();
        Location sourceMax = region.getMaximumPoint();
        World world = sourceMin.getWorld();
        if (world == null || sourceMax.getWorld() == null || !Objects.equals(world.getUID(), sourceMax.getWorld().getUID())) {
            future.complete(false);
            return future;
        }

        SchematicBounds bounds = SchematicMath.computeBounds(world, sourceMin, sourceMax, type);
        int width = bounds.maxX() - bounds.minX() + 1;
        int height = bounds.maxY() - bounds.minY() + 1;
        int length = bounds.maxZ() - bounds.minZ() + 1;
        long volumeLong = (long) width * height * length;
        if (volumeLong <= 0 || volumeLong > Integer.MAX_VALUE) {
            future.complete(false);
            return future;
        }
        int volume = (int) volumeLong;

        if (type == SchematicManager.SchematicType.CHUNK) {
            saveChunk(world, bounds, width, height, length, volume, schematicName, type, future, onProgress);
            return future;
        }

        saveTickBased(world, bounds, width, height, length, volume, schematicName, type, future, onProgress);
        return future;
    }

    private void saveChunk(World world, SchematicBounds bounds, int width, int height, int length,
                           int volume, String schematicName, SchematicManager.SchematicType type,
                           CompletableFuture<Boolean> future, Consumer<Float> onProgress) {
        int chunksX = width / 16;
        int chunksZ = length / 16;
        int totalChunks = chunksX * chunksZ;
        int chunkMinX = bounds.minX() >> 4;
        int chunkMinZ = bounds.minZ() >> 4;

        ChunkSnapshot[] snapshots = new ChunkSnapshot[totalChunks];
        AtomicInteger snapshotsDone = new AtomicInteger(0);

        for (int ci = 0; ci < totalChunks; ci++) {
            int cx = chunkMinX + (ci % chunksX);
            int cz = chunkMinZ + (ci / chunksX);
            final int index = ci;

            world.getChunkAtAsync(cx, cz).thenAccept(chunk -> {
                snapshots[index] = chunk.getChunkSnapshot();
                int done = snapshotsDone.incrementAndGet();

                if (onProgress != null) {
                    onProgress.accept((float) done / totalChunks * 0.5f);
                }

                if (done >= totalChunks) {
                    short[] data = new short[volume];
                    List<String> palette = new ArrayList<>();
                    Map<String, Integer> paletteLookup = new HashMap<>();
                    processSnapshotsAsync(snapshots, data, palette, paletteLookup,
                        chunksX, totalChunks, bounds, width, length, height,
                        world.getName(), volume, schematicName, type, future, onProgress);
                }
            });
        }
    }

    private void processSnapshotsAsync(ChunkSnapshot[] snapshots, short[] data, List<String> palette,
                                        Map<String, Integer> paletteLookup, int chunksX, int totalChunks,
                                        SchematicBounds bounds, int width, int length, int height,
                                        String worldName, int volume, String schematicName,
                                        SchematicManager.SchematicType type,
                                        CompletableFuture<Boolean> future, Consumer<Float> onProgress) {
        Tasks.ioRun(() -> {
            try {
                int lastMilestone = -1;
                for (int ci = 0; ci < totalChunks; ci++) {
                    ChunkSnapshot snapshot = snapshots[ci];
                    snapshots[ci] = null;
                    int cx = ci % chunksX;
                    int cz = ci / chunksX;
                    int baseX = cx * 16;
                    int baseZ = cz * 16;

                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        int ly = y - bounds.minY();
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                String str = snapshot.getBlockData(bx, y, bz).getAsString();
                                int pi = paletteLookup.computeIfAbsent(str, k -> {
                                    int idx = palette.size();
                                    palette.add(k);
                                    return idx;
                                });
                                data[SchematicMath.toIndex(baseX + bx, ly, baseZ + bz, width, length)] = (short) pi;
                            }
                        }
                    }

                    float progress = 0.5f + (float) (ci + 1) / totalChunks * 0.5f;
                    int milestone = (int) (progress * 4);
                    if (milestone > lastMilestone) {
                        lastMilestone = milestone;
                        DebugAPI.logLibDebug("Saving schematic '" + schematicName + "': " + (int) (progress * 100) + "%");
                    }
                    if (onProgress != null) onProgress.accept(progress);
                }

                CustomSchematic schematic = new CustomSchematic(
                    worldName, width, height, length,
                    bounds.anchorX(), bounds.anchorY(), bounds.anchorZ(),
                    type, palette, data
                );
                loadedSchematics.put(schematicName, schematic);
                CustomSchematicCodec.write(schematicFile(schematicName), schematic);
                DebugAPI.logLibDebug("Custom schematic saved: " + schematicName + " (" + volume + " blocks, type=" + type + ")");
                future.complete(true);
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.REGION, "Error saving custom schematic " + schematicName + ": " + e.getMessage());
                future.complete(false);
            }
        });
    }

    private void saveTickBased(World world, SchematicBounds bounds, int width, int height, int length,
                                int volume, String schematicName, SchematicManager.SchematicType type,
                                CompletableFuture<Boolean> future, Consumer<Float> onProgress) {
        int minCX = bounds.minX() >> 4;
        int maxCX = bounds.maxX() >> 4;
        int minCZ = bounds.minZ() >> 4;
        int maxCZ = bounds.maxZ() >> 4;
        int totalChunks = (maxCX - minCX + 1) * (maxCZ - minCZ + 1);

        Map<Long, ChunkSnapshot> snapshotMap = new ConcurrentHashMap<>();
        AtomicInteger loaded = new AtomicInteger(0);

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                long key = SchematicMath.chunkKey(cx, cz);
                world.getChunkAtAsync(cx, cz).thenAccept(chunk -> {
                    snapshotMap.put(key, chunk.getChunkSnapshot());
                    if (loaded.incrementAndGet() >= totalChunks) {
                        processBlocksFromSnapshots(snapshotMap, bounds, width, height, length, volume,
                            world.getName(), schematicName, type, future, onProgress);
                    }
                });
            }
        }
    }

    private void processBlocksFromSnapshots(Map<Long, ChunkSnapshot> snapshots, SchematicBounds bounds,
                                             int width, int height, int length, int volume,
                                             String worldName, String schematicName,
                                             SchematicManager.SchematicType type,
                                             CompletableFuture<Boolean> future, Consumer<Float> onProgress) {
        Tasks.ioRun(() -> {
            try {
                List<String> palette = new ArrayList<>();
                Map<String, Integer> paletteLookup = new HashMap<>();
                short[] data = new short[volume];

                long maxCursor = SchematicMath.calculateMaxCursor(type, width, height, length);
                long processed = 0;
                int lastMilestone = -1;

                for (long cursor = 0; cursor < maxCursor; cursor++) {
                    SchematicCoordinate coordinate = SchematicMath.resolveCoordinate(type, cursor, width, height, length);
                    if (coordinate == null) continue;

                    int worldX = bounds.minX() + coordinate.x();
                    int worldY = bounds.minY() + coordinate.y();
                    int worldZ = bounds.minZ() + coordinate.z();

                    long chunkKey = SchematicMath.chunkKey(worldX >> 4, worldZ >> 4);
                    ChunkSnapshot snapshot = snapshots.get(chunkKey);
                    if (snapshot == null) continue;

                    String blockDataString = snapshot.getBlockData(worldX & 15, worldY, worldZ & 15).getAsString();
                    int pi = paletteLookup.computeIfAbsent(blockDataString, k -> {
                        int idx = palette.size();
                        palette.add(k);
                        return idx;
                    });

                    int index = SchematicMath.toIndex(coordinate.x(), coordinate.y(), coordinate.z(), width, length);
                    data[index] = (short) pi;
                    processed++;

                    float progress = (float) processed / volume;
                    int milestone = (int) (progress * 4);
                    if (milestone > lastMilestone) {
                        lastMilestone = milestone;
                        DebugAPI.logLibDebug("Saving schematic '" + schematicName + "': " + (int) (progress * 100) + "%");
                    }
                    if (onProgress != null) onProgress.accept(progress);
                }

                snapshots.clear();

                if (processed < volume) {
                    future.complete(false);
                    return;
                }

                CustomSchematic schematic = new CustomSchematic(
                    worldName, width, height, length,
                    bounds.anchorX(), bounds.anchorY(), bounds.anchorZ(),
                    type, palette, data
                );
                loadedSchematics.put(schematicName, schematic);
                CustomSchematicCodec.write(schematicFile(schematicName), schematic);
                DebugAPI.logLibDebug("Custom schematic saved: " + schematicName + " (" + volume + " blocks, type=" + type + ")");
                future.complete(true);
            } catch (Exception e) {
                DebugAPI.logLibDebug(DebugCategory.REGION, "Error saving custom schematic " + schematicName + ": " + e.getMessage());
                future.complete(false);
            }
        });
    }

    CompletableFuture<CustomSchematic> load(String schematicName) {
        CustomSchematic cached = loadedSchematics.getIfPresent(schematicName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        File file = schematicFile(schematicName);
        if (!file.exists()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<CustomSchematic> future = new CompletableFuture<>();
        Tasks.ioRun(() -> {
            try {
                CustomSchematic schematic = CustomSchematicCodec.read(file);
                loadedSchematics.put(schematicName, schematic);
                future.complete(schematic);
            } catch (Exception e) {
                DebugAPI.logLibDebug(DebugCategory.REGION, "Error loading custom schematic " + schematicName + ": " + e.getMessage());
                future.complete(null);
            }
        });
        return future;
    }

    private static final long PASTE_BUDGET_NS = 4_500_000L;

    CompletableFuture<Boolean> paste(String schematicName, CustomSchematic schematic, Location location,
                                      SchematicManager.SchematicType type, Consumer<Float> onProgress,
                                      java.util.function.BiConsumer<Integer, Integer> onChunkEnter) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        World world = location.getWorld();
        if (world == null) {
            future.complete(false);
            return future;
        }

        int width = schematic.width();
        int height = schematic.height();
        int length = schematic.length();
        long volumeLong = (long) width * height * length;
        if (volumeLong <= 0 || volumeLong > Integer.MAX_VALUE) {
            future.complete(false);
            return future;
        }
        int volume = (int) volumeLong;

        int baseX = location.getBlockX() - schematic.anchorX();
        int baseY = location.getBlockY() - schematic.anchorY();
        int baseZ = location.getBlockZ() - schematic.anchorZ();

        BlockData[] paletteData = schematic.getOrComputePaletteData();

        Object nmsLevel = null;
        Object[] nmsStates = null;
        if (NmsBlockHelper.AVAILABLE) {
            try {
                nmsLevel = NmsBlockHelper.getServerLevel(world);
                nmsStates = schematic.getOrComputeNmsStates();
            } catch (Throwable t) {
                DebugAPI.logLibDebug(DebugCategory.REGION, "NMS pre-computation failed, using fallback: " + t.getMessage());
            }
        }

        int minCX = baseX >> 4;
        int maxCX = (baseX + width - 1) >> 4;
        int minCZ = baseZ >> 4;
        int maxCZ = (baseZ + length - 1) >> 4;

        List<int[]> chunkCoords = new ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                chunkCoords.add(new int[]{cx, cz});
            }
        }

        AtomicInteger placed = new AtomicInteger(0);
        AtomicInteger lastMilestone = new AtomicInteger(-1);

        List<CompletableFuture<org.bukkit.Chunk>> chunkFutures = new ArrayList<>(chunkCoords.size());
        for (int[] coord : chunkCoords) {
            chunkFutures.add(world.getChunkAtAsync(coord[0], coord[1]));
        }

        final Object finalNmsLevel = nmsLevel;
        final Object[] finalNmsStates = nmsStates;
        CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).thenRun(() ->
            pasteChunkBatch(chunkCoords, 0, world, schematic, paletteData, finalNmsLevel, finalNmsStates,
                baseX, baseY, baseZ, width, height, length, volume,
                schematicName, placed, lastMilestone, future, onProgress, onChunkEnter)
        );

        return future;
    }

    private void pasteChunkBatch(List<int[]> chunkCoords, int startIndex, World world,
                                  CustomSchematic schematic, BlockData[] paletteData,
                                  Object nmsLevel, Object[] nmsStates,
                                  int baseX, int baseY, int baseZ,
                                  int width, int height, int length, int volume,
                                  String schematicName, AtomicInteger placed, AtomicInteger lastMilestone,
                                  CompletableFuture<Boolean> future, Consumer<Float> onProgress,
                                  java.util.function.BiConsumer<Integer, Integer> onChunkEnter) {
        if (startIndex >= chunkCoords.size()) {
            DebugAPI.logLibDebug(DebugCategory.REGION, "Custom schematic pasted: " + schematicName);
            future.complete(true);
            return;
        }

        int[] first = chunkCoords.get(startIndex);
        Location loc = new Location(world, (first[0] << 4) + 8, baseY, (first[1] << 4) + 8);

        Tasks.atLater(loc, () -> {
            long deadline = System.nanoTime() + PASTE_BUDGET_NS;
            int idx = startIndex;

            while (idx < chunkCoords.size()) {
                int cx = chunkCoords.get(idx)[0];
                int cz = chunkCoords.get(idx)[1];

                if (!TaskAPI.isRegionThread(world, cx, cz)) break;

                if (onChunkEnter != null) onChunkEnter.accept(cx, cz);
                processChunk(cx, cz, world, schematic, paletteData, nmsLevel, nmsStates,
                    baseX, baseY, baseZ, width, height, length, volume,
                    schematicName, placed, lastMilestone, onProgress);

                idx++;
                if (System.nanoTime() >= deadline) break;
            }

            pasteChunkBatch(chunkCoords, idx, world, schematic, paletteData, nmsLevel, nmsStates,
                baseX, baseY, baseZ, width, height, length, volume,
                schematicName, placed, lastMilestone, future, onProgress, onChunkEnter);
        }, 1);
    }

    private void processChunk(int cx, int cz, World world,
                               CustomSchematic schematic, BlockData[] paletteData,
                               Object nmsLevel, Object[] nmsStates,
                               int baseX, int baseY, int baseZ,
                               int width, int height, int length, int volume,
                               String schematicName, AtomicInteger placed,
                               AtomicInteger lastMilestone, Consumer<Float> onProgress) {
        int chunkMinX = cx << 4;
        int chunkMinZ = cz << 4;
        int startX = Math.max(0, chunkMinX - baseX);
        int endX = Math.min(width, chunkMinX + 16 - baseX);
        int startZ = Math.max(0, chunkMinZ - baseZ);
        int endZ = Math.min(length, chunkMinZ + 16 - baseZ);
        int count = (endX - startX) * (endZ - startZ) * height;

        if (NmsBlockHelper.AVAILABLE && nmsLevel != null && nmsStates != null) {
            try {
                Object mutablePos = NmsBlockHelper.createMutableBlockPos();
                for (int lx = startX; lx < endX; lx++) {
                    for (int lz = startZ; lz < endZ; lz++) {
                        for (int ly = 0; ly < height; ly++) {
                            int pi = schematic.data()[SchematicMath.toIndex(lx, ly, lz, width, length)] & 0xFFFF;
                            if (pi < nmsStates.length) {
                                NmsBlockHelper.setBlock(nmsLevel, mutablePos,
                                    baseX + lx, baseY + ly, baseZ + lz, nmsStates[pi]);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                DebugAPI.logLibDebug(DebugCategory.REGION, "NMS block set error in chunk " + cx + "," + cz + ": " + t.getMessage());
                fallbackProcessChunk(cx, cz, world, schematic, paletteData, baseX, baseY, baseZ, width, height, length);
            }
        } else {
            fallbackProcessChunk(cx, cz, world, schematic, paletteData, baseX, baseY, baseZ, width, height, length);
        }

        int done = placed.addAndGet(count);
        float progress = (float) done / volume;
        int milestone = (int) (progress * 4);
        int prev = lastMilestone.get();
        if (milestone > prev && lastMilestone.compareAndSet(prev, milestone)) {
            DebugAPI.logLibDebug("Pasting schematic '" + schematicName + "': " + (int) (progress * 100) + "%");
        }
        if (onProgress != null) onProgress.accept(progress);
    }

    private void fallbackProcessChunk(int cx, int cz, World world,
                                       CustomSchematic schematic, BlockData[] paletteData,
                                       int baseX, int baseY, int baseZ,
                                       int width, int height, int length) {
        int chunkMinX = cx << 4;
        int chunkMinZ = cz << 4;
        int startX = Math.max(0, chunkMinX - baseX);
        int endX = Math.min(width, chunkMinX + 16 - baseX);
        int startZ = Math.max(0, chunkMinZ - baseZ);
        int endZ = Math.min(length, chunkMinZ + 16 - baseZ);
        for (int lx = startX; lx < endX; lx++) {
            for (int lz = startZ; lz < endZ; lz++) {
                for (int ly = 0; ly < height; ly++) {
                    int pi = schematic.data()[SchematicMath.toIndex(lx, ly, lz, width, length)] & 0xFFFF;
                    if (pi < paletteData.length) {
                        world.getBlockAt(baseX + lx, baseY + ly, baseZ + lz)
                            .setBlockData(paletteData[pi], false);
                    }
                }
            }
        }
    }

    boolean delete(String schematicName) {
        File file = schematicFile(schematicName);
        if (!file.exists()) {
            loadedSchematics.invalidate(schematicName);
            return false;
        }

        try {
            Files.delete(file.toPath());
            loadedSchematics.invalidate(schematicName);
            DebugAPI.logLibDebug("Schematic deleted: " + schematicName);
            return true;
        } catch (IOException e) {
            DebugAPI.logLibDebug(DebugCategory.REGION, "Error deleting custom schematic " + schematicName + ": " + e.getMessage());
            return false;
        }
    }

    boolean exists(String schematicName) {
        return schematicFile(schematicName).exists();
    }

    void unload(String schematicName) {
        loadedSchematics.invalidate(schematicName);
    }

    void unloadAll() {
        loadedSchematics.invalidateAll();
    }

    int loadedCount() {
        return (int) loadedSchematics.estimatedSize();
    }

    private File schematicFile(String name) {
        return new File(folder, name + ".xschem");
    }
}
