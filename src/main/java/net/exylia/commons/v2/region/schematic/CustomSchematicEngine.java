package net.exylia.commons.v2.region.schematic;
import net.exylia.commons.v2.debug.api.DebugAPI;

import net.exylia.commons.v2.region.model.Region;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


final class CustomSchematicEngine {

    private final File folder;
    private final Map<String, CustomSchematic> loadedSchematics;

    CustomSchematicEngine(File folder) {
        this.folder = folder;
        this.loadedSchematics = new ConcurrentHashMap<>();
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
                    int[] data = new int[volume];
                    List<String> palette = new ArrayList<>();
                    Map<String, Integer> paletteLookup = new HashMap<>();
                    processSnapshotsAsync(snapshots, data, palette, paletteLookup,
                        chunksX, totalChunks, bounds, width, length, height,
                        world.getName(), volume, schematicName, type, future, onProgress);
                }
            });
        }
    }

    private void processSnapshotsAsync(ChunkSnapshot[] snapshots, int[] data, List<String> palette,
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
                                data[SchematicMath.toIndex(baseX + bx, ly, baseZ + bz, width, length)] = pi;
                            }
                        }
                    }

                    float progress = 0.5f + (float) (ci + 1) / totalChunks * 0.5f;
                    int milestone = (int) (progress * 4);
                    if (milestone > lastMilestone) {
                        lastMilestone = milestone;
                        DebugAPI.logLibInfo("Saving schematic '" + schematicName + "': " + (int) (progress * 100) + "%");
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
                DebugAPI.logLibInfo("Custom schematic saved: " + schematicName + " (" + volume + " blocks, type=" + type + ")");
                future.complete(true);
            } catch (Exception e) {
                DebugAPI.logLibDebug("Error saving custom schematic " + schematicName + ": " + e.getMessage());
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
                int[] data = new int[volume];

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
                    data[index] = pi;
                    processed++;

                    float progress = (float) processed / volume;
                    int milestone = (int) (progress * 4);
                    if (milestone > lastMilestone) {
                        lastMilestone = milestone;
                        DebugAPI.logLibInfo("Saving schematic '" + schematicName + "': " + (int) (progress * 100) + "%");
                    }
                    if (onProgress != null) onProgress.accept(progress);
                }

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
                DebugAPI.logLibInfo("Custom schematic saved: " + schematicName + " (" + volume + " blocks, type=" + type + ")");
                future.complete(true);
            } catch (Exception e) {
                DebugAPI.logLibDebug("Error saving custom schematic " + schematicName + ": " + e.getMessage());
                future.complete(false);
            }
        });
    }

    CompletableFuture<CustomSchematic> load(String schematicName) {
        CustomSchematic cached = loadedSchematics.get(schematicName);
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
                DebugAPI.logLibDebug("Error loading custom schematic " + schematicName + ": " + e.getMessage());
                future.complete(null);
            }
        });
        return future;
    }

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

        BlockData[] paletteData = new BlockData[schematic.palette().size()];
        for (int i = 0; i < schematic.palette().size(); i++) {
            paletteData[i] = Bukkit.createBlockData(schematic.palette().get(i));
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
        int[] lastMilestone = {-1};

        pasteNextChunk(chunkCoords, 0, world, schematic, paletteData,
            baseX, baseY, baseZ, width, height, length, volume,
            schematicName, placed, lastMilestone, future, onProgress, onChunkEnter);
        return future;
    }

    private void pasteNextChunk(List<int[]> chunkCoords, int chunkIndex, World world,
                                CustomSchematic schematic, BlockData[] paletteData,
                                int baseX, int baseY, int baseZ,
                                int width, int height, int length, int volume,
                                String schematicName, AtomicInteger placed, int[] lastMilestone,
                                CompletableFuture<Boolean> future, Consumer<Float> onProgress,
                                java.util.function.BiConsumer<Integer, Integer> onChunkEnter) {
        if (chunkIndex >= chunkCoords.size()) {
            DebugAPI.logLibDebug("Custom schematic pasted: " + schematicName);
            future.complete(true);
            return;
        }

        int cx = chunkCoords.get(chunkIndex)[0];
        int cz = chunkCoords.get(chunkIndex)[1];

        world.getChunkAtAsync(cx, cz).thenAccept(chunk -> {
            Location regionLoc = new Location(world, (cx << 4) + 8, baseY, (cz << 4) + 8);
            Tasks.runOnLocation(regionLoc, () -> {
                if (onChunkEnter != null) onChunkEnter.accept(cx, cz);

                int chunkMinX = cx << 4;
                int chunkMinZ = cz << 4;
                int startX = Math.max(0, chunkMinX - baseX);
                int endX = Math.min(width, chunkMinX + 16 - baseX);
                int startZ = Math.max(0, chunkMinZ - baseZ);
                int endZ = Math.min(length, chunkMinZ + 16 - baseZ);

                int count = 0;
                for (int lx = startX; lx < endX; lx++) {
                    for (int lz = startZ; lz < endZ; lz++) {
                        for (int ly = 0; ly < height; ly++) {
                            int idx = SchematicMath.toIndex(lx, ly, lz, width, length);
                            int pi = schematic.data()[idx];
                            if (pi >= 0 && pi < paletteData.length) {
                                world.getBlockAt(baseX + lx, baseY + ly, baseZ + lz)
                                    .setBlockData(paletteData[pi], false);
                            }
                            count++;
                        }
                    }
                }

                int done = placed.addAndGet(count);
                float progress = (float) done / volume;
                int milestone = (int) (progress * 4);
                if (milestone > lastMilestone[0]) {
                    lastMilestone[0] = milestone;
                    DebugAPI.logLibInfo("Pasting schematic '" + schematicName + "': " + (int) (progress * 100) + "%");
                }
                if (onProgress != null) onProgress.accept(progress);

                pasteNextChunk(chunkCoords, chunkIndex + 1, world, schematic, paletteData,
                    baseX, baseY, baseZ, width, height, length, volume,
                    schematicName, placed, lastMilestone, future, onProgress, onChunkEnter);
            });
        });
    }

    boolean delete(String schematicName) {
        File file = schematicFile(schematicName);
        if (!file.exists()) {
            loadedSchematics.remove(schematicName);
            return false;
        }

        try {
            Files.delete(file.toPath());
            loadedSchematics.remove(schematicName);
            DebugAPI.logLibInfo("Schematic deleted: " + schematicName);
            return true;
        } catch (IOException e) {
            DebugAPI.logLibDebug("Error deleting custom schematic " + schematicName + ": " + e.getMessage());
            return false;
        }
    }

    boolean exists(String schematicName) {
        return schematicFile(schematicName).exists();
    }

    void unload(String schematicName) {
        loadedSchematics.remove(schematicName);
    }

    void unloadAll() {
        loadedSchematics.clear();
    }

    int loadedCount() {
        return loadedSchematics.size();
    }

    private File schematicFile(String name) {
        return new File(folder, name + ".xschem");
    }
}
