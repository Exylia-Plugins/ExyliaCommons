package net.exylia.commons.v2.hologram.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ChunkListener implements Listener {
    private final HologramManager manager;
    private final Set<String> pendingRespawns = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        manager.getAllHolograms().stream()
                .filter(h -> isInChunk(h.getLocation(), chunk))
                .filter(h -> h.getConfig().isSpawnOnChunkLoad())
                .forEach(hologram -> {
                    if (!hologram.isSpawned()) {
                        Tasks.at(hologram.getLocation(), () -> {
                            hologram.spawn();
                            pendingRespawns.remove(hologram.getId());
                        });
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        manager.getAllHolograms().stream()
                .filter(h -> isInChunk(h.getLocation(), chunk))
                .filter(h -> h.getConfig().isRemoveOnChunkUnload())
                .forEach(hologram -> {
                    if (hologram.isSpawned()) {
                        Tasks.at(hologram.getLocation(), () -> {
                            hologram.despawn();
                            pendingRespawns.add(hologram.getId());
                        });
                    }
                });
    }

    private boolean isInChunk(Location location, Chunk chunk) {
        if (location.getWorld() == null || !location.getWorld().equals(chunk.getWorld())) {
            return false;
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        return chunkX == chunk.getX() && chunkZ == chunk.getZ();
    }

    public boolean isPendingRespawn(String hologramId) {
        return pendingRespawns.contains(hologramId);
    }

    public void clearPendingRespawns() {
        pendingRespawns.clear();
    }
}
