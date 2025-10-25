package net.exylia.commons.region.schematic;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BlockPlacer {
    private static final int CHUNKS_PER_BATCH = 16;

    public CompletableFuture<Void> placeSchematic(SchematicData data, Location baseLocation) {
        long startTime = System.currentTimeMillis();
        World world = baseLocation.getWorld();
        DebugUtils.logInternalDebug("[BlockPlacer] Starting schematic placement - Size: " + data.getWidth() + "x" + data.getHeight() + "x" + data.getLength());

        return ChunkPreloader.preloadChunks(data, baseLocation)
            .thenCompose(preloadedChunks -> {
                long preloadDuration = System.currentTimeMillis() - startTime;
                DebugUtils.logInternalDebug("[BlockPlacer] Chunk preload completed in " + preloadDuration + "ms");

                long groupStart = System.currentTimeMillis();
                Map<Chunk, List<BlockPlacement>> chunkMap = groupBlocksByChunk(data, baseLocation, preloadedChunks);
                long groupDuration = System.currentTimeMillis() - groupStart;
                DebugUtils.logInternalDebug("[BlockPlacer] Block grouping completed in " + groupDuration + "ms - Total blocks: " + chunkMap.values().stream().mapToInt(List::size).sum());

                List<Chunk> chunks = new ArrayList<>(chunkMap.keySet());
                DebugUtils.logInternalDebug("[BlockPlacer] Processing " + chunks.size() + " chunks");
                return processChunksAsync(chunks, chunkMap, world);
            });
    }

    private Map<Chunk, List<BlockPlacement>> groupBlocksByChunk(SchematicData data, Location baseLocation, Map<Integer, Chunk> preloadedChunks) {
        long groupStart = System.currentTimeMillis();
        Map<Integer, List<BlockPlacement>> chunkKeyMap = new HashMap<>();

        int baseX = baseLocation.getBlockX();
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ();

        DebugUtils.logInternalDebug("[BlockPlacer] Starting block grouping for " + (data.getWidth() * data.getHeight() * data.getLength()) + " blocks");

        for (int x = 0; x < data.getWidth(); x++) {
            int worldX = baseX + x;
            int chunkX = (worldX >> 4);

            for (int z = 0; z < data.getLength(); z++) {
                int worldZ = baseZ + z;
                int chunkZ = (worldZ >> 4);

                int chunkKey = chunkX << 16 | (chunkZ & 0xFFFF);
                List<BlockPlacement> placements = chunkKeyMap.computeIfAbsent(chunkKey, k -> new ArrayList<>());

                for (int y = 0; y < data.getHeight(); y++) {
                    short blockId = data.getBlockId(x, y, z);
                    if (blockId == 0) continue;

                    org.bukkit.block.data.BlockData blockData = data.getBlockData(x, y, z);
                    int worldY = baseY + y;

                    placements.add(new BlockPlacement(worldX, worldY, worldZ, blockId, blockData));
                }
            }
        }

        long conversionStart = System.currentTimeMillis();
        Map<Chunk, List<BlockPlacement>> chunkMap = new HashMap<>();

        for (Map.Entry<Integer, List<BlockPlacement>> entry : chunkKeyMap.entrySet()) {
            int chunkKey = entry.getKey();
            Chunk chunk = preloadedChunks.get(chunkKey);
            if (chunk != null) {
                chunkMap.put(chunk, entry.getValue());
            }
        }

        long conversionDuration = System.currentTimeMillis() - conversionStart;
        long totalDuration = System.currentTimeMillis() - groupStart;
        int totalBlocks = chunkMap.values().stream().mapToInt(List::size).sum();

        DebugUtils.logInternalDebug("[BlockPlacer] Block grouping processing: " + (totalDuration - conversionDuration) + "ms | Chunk conversion: " + conversionDuration + "ms | Total: " + totalDuration + "ms | Total blocks after skipping air: " + totalBlocks);

        return chunkMap;
    }

    private CompletableFuture<Void> processChunksAsync(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

        for (int i = 0; i < chunks.size(); i += CHUNKS_PER_BATCH) {
            final int startIdx = i;
            int end = Math.min(i + CHUNKS_PER_BATCH, chunks.size());
            List<Chunk> batch = new ArrayList<>(chunks.subList(startIdx, end));

            result = result.thenCompose(v -> processBatchSync(batch, chunkMap, world));
        }

        return result;
    }

    private CompletableFuture<Void> processBatchSync(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Schedulers.sync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                int totalBlocks = 0;
                int alreadyLoadedCount = 0;
                int needLoadCount = 0;

                DebugUtils.logInternalDebug("[BlockPlacer] Starting batch placement for " + chunks.size() + " chunks");

                for (Chunk chunk : chunks) {
                    List<BlockPlacement> placements = chunkMap.get(chunk);
                    if (placements == null) continue;

                    boolean isLoaded = chunk.isLoaded();
                    if (isLoaded) alreadyLoadedCount++;
                    else needLoadCount++;

                    long chunkStartTime = System.currentTimeMillis();
                    for (BlockPlacement placement : placements) {
                        Block block = world.getBlockAt(placement.x, placement.y, placement.z);
                        Material material = getMaterialById(placement.blockId);

                        if (material != null) {
                            try {
                                org.bukkit.block.data.BlockData blockData = placement.blockData;
                                if (blockData != null) {
                                    block.setBlockData(blockData, false);
                                } else {
                                    block.setType(material, false);
                                }
                            } catch (Exception e) {
                                block.setType(material, false);
                            }
                        }
                        totalBlocks++;
                    }
                    long chunkDuration = System.currentTimeMillis() - chunkStartTime;
                    DebugUtils.logInternalDebug("[BlockPlacer] Chunk (" + chunk.getX() + "," + chunk.getZ() + ") - Loaded: " + isLoaded + " | Time: " + chunkDuration + "ms | Blocks: " + placements.size());
                }

                long duration = System.currentTimeMillis() - startTime;
                DebugUtils.logInternalDebug("[BlockPlacer] === BATCH SUMMARY === | Already Loaded: " + alreadyLoadedCount + " | Need Load: " + needLoadCount + " | Total time: " + duration + "ms | Total blocks: " + totalBlocks);

                future.complete(null);

            } catch (Exception e) {
                DebugUtils.logInternalError("[BlockPlacer] Error placing blocks: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public void shutdown() {
    }

    private static Material getMaterialById(short blockId) {
        return MaterialRegistry.getMaterial(blockId);
    }

    private static class BlockPlacement {
        int x, y, z;
        short blockId;
        org.bukkit.block.data.BlockData blockData;

        BlockPlacement(int x, int y, int z, short blockId) {
            this(x, y, z, blockId, null);
        }

        BlockPlacement(int x, int y, int z, short blockId, org.bukkit.block.data.BlockData blockData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
            this.blockData = blockData;
        }
    }
}
