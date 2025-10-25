package net.exylia.commons.region.schematic;

import net.exylia.commons.async.Schedulers;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkPreloader {

    private static final int CHUNK_LOAD_TIMEOUT_MS = 30000;

    public static CompletableFuture<Map<Integer, Chunk>> preloadChunks(SchematicData data, Location baseLocation) {
        long startTotal = System.currentTimeMillis();
        World world = baseLocation.getWorld();
        if (world == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("World is null"));
        }

        long calcStart = System.currentTimeMillis();
        Set<ChunkCoordinates> chunkCoordinates = calculateChunksNeeded(data, baseLocation);
        long calcDuration = System.currentTimeMillis() - calcStart;
        DebugUtils.logInternalDebug("[ChunkPreloader] Calculated " + chunkCoordinates.size() + " chunks in " + calcDuration + "ms");

        if (chunkCoordinates.isEmpty()) {
            DebugUtils.logInternalDebug("[ChunkPreloader] No chunks to preload");
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        return preloadChunksAsync(world, chunkCoordinates, startTotal);
    }

    private static Set<ChunkCoordinates> calculateChunksNeeded(SchematicData data, Location baseLocation) {
        Set<ChunkCoordinates> chunks = new HashSet<>();

        int baseX = baseLocation.getBlockX();
        int baseZ = baseLocation.getBlockZ();

        int minChunkX = (baseX >> 4);
        int minChunkZ = (baseZ >> 4);
        int maxChunkX = ((baseX + data.getWidth() - 1) >> 4);
        int maxChunkZ = ((baseZ + data.getLength() - 1) >> 4);

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks.add(new ChunkCoordinates(x, z));
            }
        }

        return chunks;
    }

    private static CompletableFuture<Map<Integer, Chunk>> preloadChunksAsync(World world, Set<ChunkCoordinates> chunkCoordinates, long startTotal) {
        CompletableFuture<Map<Integer, Chunk>> future = new CompletableFuture<>();

        Schedulers.runAsyncTask(() -> {
            try {
                long asyncStartTime = System.currentTimeMillis();
                DebugUtils.logInternalDebug("[ChunkPreloader] Starting async chunk loading for " + chunkCoordinates.size() + " chunks");

                AtomicInteger loadedCount = new AtomicInteger(0);
                Map<ChunkCoordinates, CompletableFuture<Chunk>> chunkFutures = new ConcurrentHashMap<>();

                long queueStart = System.currentTimeMillis();
                for (ChunkCoordinates coord : chunkCoordinates) {
                    chunkFutures.put(coord, loadChunkAsync(world, coord.x, coord.z));
                }
                long queueDuration = System.currentTimeMillis() - queueStart;
                DebugUtils.logInternalDebug("[ChunkPreloader] Queued " + chunkCoordinates.size() + " chunk load tasks in " + queueDuration + "ms");

                CompletableFuture<Void> allChunksLoaded = CompletableFuture.allOf(
                    chunkFutures.values().toArray(new CompletableFuture[0])
                );

                try {
                    long waitStart = System.currentTimeMillis();
                    allChunksLoaded.get();
                    long waitDuration = System.currentTimeMillis() - waitStart;

                    Map<Integer, Chunk> chunkMap = new HashMap<>();
                    for (Map.Entry<ChunkCoordinates, CompletableFuture<Chunk>> entry : chunkFutures.entrySet()) {
                        ChunkCoordinates coord = entry.getKey();
                        Chunk chunk = entry.getValue().get();
                        int key = coord.x << 16 | (coord.z & 0xFFFF);
                        chunkMap.put(key, chunk);
                    }

                    loadedCount.set(chunkCoordinates.size());

                    long asyncDuration = System.currentTimeMillis() - asyncStartTime;
                    long totalDuration = System.currentTimeMillis() - startTotal;
                    DebugUtils.logInternalDebug("[ChunkPreloader] === CHUNK LOAD SUMMARY === | Wait time: " + waitDuration + "ms | Async phase: " + asyncDuration + "ms | Total: " + totalDuration + "ms | Chunks: " + loadedCount.get());

                    future.complete(chunkMap);

                } catch (Exception e) {
                    DebugUtils.logInternalError("[ChunkPreloader] Error waiting for chunks: " + e.getMessage());
                    future.completeExceptionally(e);
                }

            } catch (Exception e) {
                DebugUtils.logInternalError("[ChunkPreloader] Exception in preloadChunksAsync: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private static CompletableFuture<Chunk> loadChunkAsync(World world, int chunkX, int chunkZ) {
        CompletableFuture<Chunk> future = new CompletableFuture<>();

        Schedulers.sync(() -> {
            try {
                long chunkStartTime = System.currentTimeMillis();
                boolean isLoaded = world.isChunkLoaded(chunkX, chunkZ);
                long checkTime = System.currentTimeMillis() - chunkStartTime;

                Chunk chunk = world.getChunkAt(chunkX, chunkZ, true);
                long chunkDuration = System.currentTimeMillis() - chunkStartTime;

                DebugUtils.logInternalDebug("[ChunkPreloader] Chunk (" + chunkX + "," + chunkZ + ") - WasLoaded: " + isLoaded + " | Check: " + checkTime + "ms | Load: " + chunkDuration + "ms");

                future.complete(chunk);
            } catch (Exception e) {
                DebugUtils.logInternalError("[ChunkPreloader] Error loading chunk (" + chunkX + "," + chunkZ + "): " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private static class ChunkCoordinates {
        final int x;
        final int z;

        ChunkCoordinates(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkCoordinates that = (ChunkCoordinates) o;
            return x == that.x && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }
}
