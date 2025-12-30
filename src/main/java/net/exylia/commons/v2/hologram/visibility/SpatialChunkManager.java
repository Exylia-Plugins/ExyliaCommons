package net.exylia.commons.v2.hologram.visibility;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpatialChunkManager {
    private static final int CHUNK_SIZE = 32;

    private final Map<World, Map<ChunkKey, Set<String>>> worldChunks = new ConcurrentHashMap<>();

    public void addHologram(Hologram hologram) {
        Location loc = hologram.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        ChunkKey key = getChunkKey(loc);
        worldChunks
                .computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(hologram.getId());
    }

    public void removeHologram(Hologram hologram) {
        Location loc = hologram.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        ChunkKey key = getChunkKey(loc);
        Map<ChunkKey, Set<String>> chunks = worldChunks.get(world);
        if (chunks != null) {
            Set<String> chunkHolograms = chunks.get(key);
            if (chunkHolograms != null) {
                chunkHolograms.remove(hologram.getId());
                if (chunkHolograms.isEmpty()) {
                    chunks.remove(key);
                }
            }
        }
    }

    public Set<String> getNearbyHologramIds(Location location, double radius) {
        World world = location.getWorld();
        if (world == null) return Collections.emptySet();

        Map<ChunkKey, Set<String>> chunks = worldChunks.get(world);
        if (chunks == null) return Collections.emptySet();

        Set<String> nearbyIds = new HashSet<>();
        int radiusInChunks = (int) Math.ceil(radius / CHUNK_SIZE) + 1;
        ChunkKey center = getChunkKey(location);

        for (int x = -radiusInChunks; x <= radiusInChunks; x++) {
            for (int z = -radiusInChunks; z <= radiusInChunks; z++) {
                ChunkKey key = new ChunkKey(center.x + x, center.z + z);
                Set<String> chunkHolograms = chunks.get(key);
                if (chunkHolograms != null) {
                    nearbyIds.addAll(chunkHolograms);
                }
            }
        }

        return nearbyIds;
    }

    public void clear() {
        worldChunks.clear();
    }

    public void clearWorld(World world) {
        worldChunks.remove(world);
    }

    private ChunkKey getChunkKey(Location location) {
        int chunkX = (int) Math.floor(location.getX() / CHUNK_SIZE);
        int chunkZ = (int) Math.floor(location.getZ() / CHUNK_SIZE);
        return new ChunkKey(chunkX, chunkZ);
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ChunkKey {
        private final int x;
        private final int z;
    }
}
