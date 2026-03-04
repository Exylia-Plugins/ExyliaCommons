package net.exylia.commons.v2.region.blocks;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerBlockTracker {
    private static PlayerBlockTracker instance;

    private final JavaPlugin plugin;
    private final Map<String, Set<BlockPosition>> regionBlocks;
    private final Map<UUID, Set<BlockPosition>> playerBlocks;

    private PlayerBlockTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regionBlocks = new ConcurrentHashMap<>();
        this.playerBlocks = new ConcurrentHashMap<>();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new PlayerBlockTracker(plugin);
        }
    }

    public static PlayerBlockTracker getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerBlockTracker has not been initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public void trackBlock(String regionId, UUID playerId, Location location) {
        BlockPosition position = new BlockPosition(location);

        regionBlocks.computeIfAbsent(regionId, k -> ConcurrentHashMap.newKeySet()).add(position);
        playerBlocks.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(position);
    }

    public boolean isPlayerPlacedBlock(String regionId, Location location) {
        Set<BlockPosition> blocks = regionBlocks.get(regionId);
        if (blocks == null) {
            return false;
        }

        return blocks.contains(new BlockPosition(location));
    }

    public boolean removeBlock(String regionId, Location location) {
        BlockPosition position = new BlockPosition(location);
        boolean removed = false;

        Set<BlockPosition> regionBlockSet = regionBlocks.get(regionId);
        if (regionBlockSet != null) {
            removed = regionBlockSet.remove(position);
            if (regionBlockSet.isEmpty()) {
                regionBlocks.remove(regionId);
            }
        }

        if (removed) {
            playerBlocks.values().forEach(set -> set.remove(position));
            playerBlocks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        return removed;
    }

    public void clearRegionBlocks(String regionId) {
        Set<BlockPosition> removed = regionBlocks.remove(regionId);
        if (removed == null || removed.isEmpty()) {
            return;
        }

        playerBlocks.values().forEach(set -> set.removeAll(removed));
        playerBlocks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public Set<BlockPosition> getPlayerBlocks(UUID playerId) {
        Set<BlockPosition> blocks = playerBlocks.get(playerId);
        return blocks != null ? new HashSet<>(blocks) : Collections.emptySet();
    }

    public Set<BlockPosition> getRegionBlocks(String regionId) {
        Set<BlockPosition> blocks = regionBlocks.get(regionId);
        return blocks != null ? new HashSet<>(blocks) : Collections.emptySet();
    }

    public void clearPlayerBlocks(UUID playerId) {
        Set<BlockPosition> removed = playerBlocks.remove(playerId);
        if (removed == null || removed.isEmpty()) {
            return;
        }

        regionBlocks.values().forEach(set -> set.removeAll(removed));
        regionBlocks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public int getRegionBlockCount(String regionId) {
        Set<BlockPosition> blocks = regionBlocks.get(regionId);
        return blocks != null ? blocks.size() : 0;
    }

    public TrackerStats getStats() {
        int totalRegions = regionBlocks.size();
        int totalPlayers = playerBlocks.size();
        int totalBlocks = regionBlocks.values().stream().mapToInt(Set::size).sum();

        return new TrackerStats(totalRegions, totalPlayers, totalBlocks);
    }

    public void clearAll() {
        regionBlocks.clear();
        playerBlocks.clear();
    }

    public void cleanup() {
        clearAll();
        synchronized (PlayerBlockTracker.class) {
            if (instance == this) {
                instance = null;
            }
        }
    }

    @Getter
    public static class BlockPosition {
        private final String world;
        private final int x;
        private final int y;
        private final int z;

        public BlockPosition(Location location) {
            this.world = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
        }

        public BlockPosition(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            BlockPosition that = (BlockPosition) obj;
            return x == that.x && y == that.y && z == that.z && Objects.equals(world, that.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }

        @Override
        public String toString() {
            return String.format("%s:%d,%d,%d", world, x, y, z);
        }
    }

    @Getter
    public static class TrackerStats {
        private final int totalRegions;
        private final int totalPlayers;
        private final int totalBlocks;

        public TrackerStats(int totalRegions, int totalPlayers, int totalBlocks) {
            this.totalRegions = totalRegions;
            this.totalPlayers = totalPlayers;
            this.totalBlocks = totalBlocks;
        }

        public double getAverageBlocksPerRegion() {
            return totalRegions > 0 ? (double) totalBlocks / totalRegions : 0.0;
        }

        @Override
        public String toString() {
            return String.format("TrackerStats{regions=%d, players=%d, blocks=%d, avg=%.1f}",
                    totalRegions, totalPlayers, totalBlocks, getAverageBlocksPerRegion());
        }
    }
}
