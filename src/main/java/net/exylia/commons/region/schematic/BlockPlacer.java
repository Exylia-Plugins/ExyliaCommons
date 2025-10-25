package net.exylia.commons.region.schematic;

import lombok.Getter;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BlockPlacer {
    private static final int CHUNKS_PER_BATCH = 16;

    @Getter
    private BlockPlacementConfig config;
    private JavaPlugin plugin;

    public BlockPlacer() {
        this(null, BlockPlacementConfig.getDefault());
    }

    public BlockPlacer(JavaPlugin plugin) {
        this(plugin, BlockPlacementConfig.getDefault());
    }

    public BlockPlacer(JavaPlugin plugin, BlockPlacementConfig config) {
        this.plugin = plugin;
        this.config = config != null ? config : BlockPlacementConfig.getDefault();
    }

    public void setConfig(BlockPlacementConfig config) {
        this.config = config != null ? config : BlockPlacementConfig.getDefault();
    }

    public CompletableFuture<Void> placeSchematic(SchematicData data, Location baseLocation) {
        World world = baseLocation.getWorld();

        return ChunkPreloader.preloadChunks(data, baseLocation)
            .thenCompose(preloadedChunks -> {
                Map<Chunk, List<BlockPlacement>> chunkMap = groupBlocksByChunk(data, baseLocation, preloadedChunks);
                List<Chunk> chunks = new ArrayList<>(chunkMap.keySet());
                return processChunksAsync(chunks, chunkMap, world);
            });
    }

    private Map<Chunk, List<BlockPlacement>> groupBlocksByChunk(SchematicData data, Location baseLocation, Map<Integer, Chunk> preloadedChunks) {
        Map<Integer, List<BlockPlacement>> chunkKeyMap = new HashMap<>();

        int baseX = baseLocation.getBlockX();
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ();

        int width = data.getWidth();
        int height = data.getHeight();
        int length = data.getLength();

        for (int x = 0; x < width; x++) {
            int worldX = baseX + x;
            int chunkX = (worldX >> 4);

            for (int z = 0; z < length; z++) {
                int worldZ = baseZ + z;
                int chunkZ = (worldZ >> 4);

                int chunkKey = chunkX << 16 | (chunkZ & 0xFFFF);
                List<BlockPlacement> placements = chunkKeyMap.computeIfAbsent(chunkKey, k -> new ArrayList<>());

                for (int y = 0; y < height; y++) {
                    short blockId = data.getBlockId(x, y, z);
                    if (blockId == 0) continue;

                    org.bukkit.block.data.BlockData blockData = data.getBlockData(x, y, z);
                    int worldY = baseY + y;

                    placements.add(new BlockPlacement(worldX, worldY, worldZ, blockId, blockData));
                }
            }
        }

        Map<Chunk, List<BlockPlacement>> chunkMap = new HashMap<>();

        for (Map.Entry<Integer, List<BlockPlacement>> entry : chunkKeyMap.entrySet()) {
            int chunkKey = entry.getKey();
            Chunk chunk = preloadedChunks.get(chunkKey);
            if (chunk != null) {
                chunkMap.put(chunk, entry.getValue());
            }
        }

        return chunkMap;
    }

    private CompletableFuture<Void> processChunksAsync(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        return switch (config.getStrategy()) {
            case FAST -> processFastStrategy(chunks, chunkMap, world);
            case BATCH_WITH_YIELDS -> processBatchWithYieldsStrategy(chunks, chunkMap, world);
            case TICK_BASED -> processTickBasedStrategy(chunks, chunkMap, world);
            case HYBRID -> processHybridStrategy(chunks, chunkMap, world);
        };
    }

    private CompletableFuture<Void> processFastStrategy(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

        for (int i = 0; i < chunks.size(); i += CHUNKS_PER_BATCH) {
            final int startIdx = i;
            int end = Math.min(i + CHUNKS_PER_BATCH, chunks.size());
            List<Chunk> batch = new ArrayList<>(chunks.subList(startIdx, end));

            result = result.thenCompose(v -> processBatchSync(batch, chunkMap, world));
        }

        return result;
    }

    private CompletableFuture<Void> processBatchWithYieldsStrategy(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        Schedulers.runAsyncTask(() -> {
            try {
                int blocksPlaced = 0;
                int blockBatch = config.getBlocksPerBatch();

                for (Chunk chunk : chunks) {
                    List<BlockPlacement> placements = chunkMap.get(chunk);
                    if (placements == null) continue;

                    for (BlockPlacement placement : placements) {
                        placeBlockSync(world, placement);
                        blocksPlaced++;

                        if (blocksPlaced % blockBatch == 0) {
                            Thread.yield();
                        }
                    }
                }

                result.complete(null);
            } catch (Exception e) {
                DebugUtils.logInternalError("[BlockPlacer] Error in batch with yields: " + e.getMessage());
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    private CompletableFuture<Void> processTickBasedStrategy(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        if (plugin == null) {
            DebugUtils.logInternalWarn("[BlockPlacer] Tick-based strategy requires JavaPlugin, falling back to FAST");
            return processFastStrategy(chunks, chunkMap, world);
        }

        Schedulers.runAsyncTask(() -> {
            try {
                java.util.concurrent.atomic.AtomicInteger chunkIndex = new java.util.concurrent.atomic.AtomicInteger(0);
                int totalChunks = chunks.size();

                net.exylia.commons.async.ScheduledTask[] task = new net.exylia.commons.async.ScheduledTask[1];
                task[0] = Schedulers.syncTimer(() -> {
                    int idx = chunkIndex.getAndIncrement();
                    if (idx >= totalChunks) {
                        task[0].cancel();
                        result.complete(null);
                        return;
                    }

                    Chunk chunk = chunks.get(idx);
                    List<BlockPlacement> placements = chunkMap.get(chunk);
                    if (placements != null) {
                        for (BlockPlacement placement : placements) {
                            placeBlockSync(world, placement);
                        }
                    }
                }, config.getTicksBetweenBatches(), config.getTicksBetweenBatches());
            } catch (Exception e) {
                DebugUtils.logInternalError("[BlockPlacer] Error in tick-based strategy: " + e.getMessage());
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    private CompletableFuture<Void> processHybridStrategy(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        if (plugin == null) {
            DebugUtils.logInternalWarn("[BlockPlacer] Hybrid strategy requires JavaPlugin, falling back to FAST");
            return processFastStrategy(chunks, chunkMap, world);
        }

        Schedulers.runAsyncTask(() -> {
            try {
                java.util.concurrent.atomic.AtomicInteger chunkIndex = new java.util.concurrent.atomic.AtomicInteger(0);
                int totalChunks = chunks.size();
                int chunksPerTick = config.getTicksBetweenBatches();

                net.exylia.commons.async.ScheduledTask[] task = new net.exylia.commons.async.ScheduledTask[1];
                task[0] = Schedulers.syncTimer(() -> {
                    for (int i = 0; i < chunksPerTick; i++) {
                        int idx = chunkIndex.getAndIncrement();
                        if (idx >= totalChunks) {
                            task[0].cancel();
                            result.complete(null);
                            return;
                        }

                        Chunk chunk = chunks.get(idx);
                        List<BlockPlacement> placements = chunkMap.get(chunk);
                        if (placements != null) {
                            for (BlockPlacement placement : placements) {
                                placeBlockSync(world, placement);
                            }
                        }
                    }
                }, 1, 1);
            } catch (Exception e) {
                DebugUtils.logInternalError("[BlockPlacer] Error in hybrid strategy: " + e.getMessage());
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    private void placeBlockSync(World world, BlockPlacement placement) {
        Block block = world.getBlockAt(placement.x, placement.y, placement.z);
        org.bukkit.block.data.BlockData blockData = placement.blockData;

        if (blockData != null) {
            block.setBlockData(blockData, false);
        } else {
            Material material = MaterialRegistry.getMaterial(placement.blockId);
            if (material != null) {
                block.setType(material, false);
            }
        }
    }

    private CompletableFuture<Void> processBatchSync(List<Chunk> chunks, Map<Chunk, List<BlockPlacement>> chunkMap, World world) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Schedulers.sync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                int totalBlocks = 0;
                int alreadyLoadedCount = 0;
                int needLoadCount = 0;

                for (Chunk chunk : chunks) {
                    List<BlockPlacement> placements = chunkMap.get(chunk);
                    if (placements == null) continue;

                    boolean isLoaded = chunk.isLoaded();
                    if (isLoaded) alreadyLoadedCount++;
                    else needLoadCount++;

                    int placementSize = placements.size();
                    for (int i = 0; i < placementSize; i++) {
                        BlockPlacement placement = placements.get(i);
                        Block block = world.getBlockAt(placement.x, placement.y, placement.z);
                        org.bukkit.block.data.BlockData blockData = placement.blockData;

                        if (blockData != null) {
                            block.setBlockData(blockData, false);
                        } else {
                            Material material = MaterialRegistry.getMaterial(placement.blockId);
                            if (material != null) {
                                block.setType(material, false);
                            }
                        }
                        totalBlocks++;
                    }
                }

                long duration = System.currentTimeMillis() - startTime;

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
