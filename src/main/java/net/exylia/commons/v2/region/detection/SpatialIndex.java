package net.exylia.commons.v2.region.detection;

import lombok.Getter;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialIndex {
    private static final int CHUNK_SIZE = 16;

    private final Map<World, Map<Long, Set<Region>>> chunkIndex;
    private volatile int totalRegions = 0;

    public SpatialIndex() {
        this.chunkIndex = new ConcurrentHashMap<>();
    }

    public void addRegion(Region region) {
        if (region == null || !region.isValid()) {
            return;
        }

        World world = region.getWorld();
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        int minChunkX = min.getBlockX() >> 4;
        int maxChunkX = max.getBlockX() >> 4;
        int minChunkZ = min.getBlockZ() >> 4;
        int maxChunkZ = max.getBlockZ() >> 4;

        Map<Long, Set<Region>> worldIndex = chunkIndex.computeIfAbsent(world, k -> new ConcurrentHashMap<>());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(chunkX, chunkZ);
                worldIndex.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(region);
            }
        }

        totalRegions++;
    }

    public void removeRegion(Region region) {
        if (region == null || !region.isValid()) {
            return;
        }

        World world = region.getWorld();
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) return;

        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        int minChunkX = min.getBlockX() >> 4;
        int maxChunkX = max.getBlockX() >> 4;
        int minChunkZ = min.getBlockZ() >> 4;
        int maxChunkZ = max.getBlockZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(chunkX, chunkZ);
                Set<Region> regionSet = worldIndex.get(chunkKey);

                if (regionSet != null) {
                    regionSet.remove(region);
                    if (regionSet.isEmpty()) {
                        worldIndex.remove(chunkKey);
                    }
                }
            }
        }

        if (worldIndex.isEmpty()) {
            chunkIndex.remove(world);
        }

        totalRegions--;
    }

    public List<Region> getRegionsAt(Location location) {
        if (location == null) {
            return Collections.emptyList();
        }

        Map<Long, Set<Region>> worldIndex = chunkIndex.get(location.getWorld());
        if (worldIndex == null) {
            return Collections.emptyList();
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        long chunkKey = getChunkKey(chunkX, chunkZ);

        Set<Region> candidateRegions = worldIndex.get(chunkKey);
        if (candidateRegions == null || candidateRegions.isEmpty()) {
            return Collections.emptyList();
        }

        Region single = null;
        List<Region> result = null;

        for (Region region : candidateRegions) {
            if (region.contains(location)) {
                if (single == null && result == null) {
                    single = region;
                } else {
                    if (result == null) {
                        result = new ArrayList<>(4);
                        result.add(single);
                        single = null;
                    }
                    result.add(region);
                }
            }
        }

        if (result != null) {
            result.sort((r1, r2) -> r2.getPriority().getLevel() - r1.getPriority().getLevel());
            return result;
        }
        if (single != null) {
            return Collections.singletonList(single);
        }
        return Collections.emptyList();
    }

    public Set<Region> getRegionsInArea(Location corner1, Location corner2) {
        if (corner1 == null || corner2 == null || !corner1.getWorld().equals(corner2.getWorld())) {
            return Collections.emptySet();
        }

        Map<Long, Set<Region>> worldIndex = chunkIndex.get(corner1.getWorld());
        if (worldIndex == null) {
            return Collections.emptySet();
        }

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        Set<Region> result = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(chunkX, chunkZ);
                Set<Region> chunkRegions = worldIndex.get(chunkKey);

                if (chunkRegions != null) {
                    result.addAll(chunkRegions);
                }
            }
        }

        return result;
    }

    public Set<Region> getRegionsInWorld(World world) {
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) {
            return Collections.emptySet();
        }

        Set<Region> result = new HashSet<>();
        for (Set<Region> chunkRegions : worldIndex.values()) {
            result.addAll(chunkRegions);
        }

        return result;
    }

    public boolean hasRegionsInChunk(World world, int chunkX, int chunkZ) {
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) return false;

        long chunkKey = getChunkKey(chunkX, chunkZ);
        Set<Region> regions = worldIndex.get(chunkKey);
        return regions != null && !regions.isEmpty();
    }

    public int getRegionCountInChunk(World world, int chunkX, int chunkZ) {
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) return 0;

        long chunkKey = getChunkKey(chunkX, chunkZ);
        Set<Region> regions = worldIndex.get(chunkKey);
        return regions != null ? regions.size() : 0;
    }

    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public IndexStats getStats() {
        int totalWorldsIndexed = chunkIndex.size();
        int totalChunksInUse = 0;
        int maxRegionsPerChunk = 0;
        int totalRegionInstances = 0;

        for (Map<Long, Set<Region>> worldIndex : chunkIndex.values()) {
            totalChunksInUse += worldIndex.size();

            for (Set<Region> regionSet : worldIndex.values()) {
                int chunkRegionCount = regionSet.size();
                totalRegionInstances += chunkRegionCount;
                maxRegionsPerChunk = Math.max(maxRegionsPerChunk, chunkRegionCount);
            }
        }

        return new IndexStats(totalRegions, totalWorldsIndexed, totalChunksInUse, maxRegionsPerChunk, totalRegionInstances);
    }

    public void clear() {
        chunkIndex.clear();
        totalRegions = 0;
    }

    public void rebuild(Collection<Region> regions) {
        clear();
        for (Region region : regions) {
            if (region != null && region.isValid()) {
                addRegion(region);
            }
        }
    }

    @Getter
    public static class IndexStats {
        private final int totalRegions;
        private final int totalWorldsIndexed;
        private final int totalChunksInUse;
        private final int maxRegionsPerChunk;
        private final int totalRegionInstances;

        public IndexStats(int totalRegions, int totalWorldsIndexed, int totalChunksInUse, int maxRegionsPerChunk, int totalRegionInstances) {
            this.totalRegions = totalRegions;
            this.totalWorldsIndexed = totalWorldsIndexed;
            this.totalChunksInUse = totalChunksInUse;
            this.maxRegionsPerChunk = maxRegionsPerChunk;
            this.totalRegionInstances = totalRegionInstances;
        }

        public double getAverageRegionsPerChunk() {
            return totalChunksInUse > 0 ? (double) totalRegionInstances / totalChunksInUse : 0.0;
        }

        public double getEfficiencyFactor() {
            return totalRegions > 0 ? (double) totalRegions / totalRegionInstances : 0.0;
        }

        @Override
        public String toString() {
            return String.format("IndexStats{regions=%d, worlds=%d, chunks=%d, max_per_chunk=%d, instances=%d, avg=%.2f, efficiency=%.2f}",
                totalRegions, totalWorldsIndexed, totalChunksInUse, maxRegionsPerChunk, totalRegionInstances, getAverageRegionsPerChunk(), getEfficiencyFactor());
        }
    }
}
