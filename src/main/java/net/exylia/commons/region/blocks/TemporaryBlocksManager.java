package net.exylia.commons.region.blocks;

import lombok.Getter;
import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.events.TemporaryBlockRemovedEvent;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class TemporaryBlocksManager {
    private static TemporaryBlocksManager instance;

    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final ConcurrentMap<String, BukkitTask> scheduledRemovals;  
    private final ConcurrentMap<String, TemporaryBlock> temporaryBlocks;  

    private static final int DEFAULT_REMOVAL_SECONDS = 30;

    private TemporaryBlocksManager(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.scheduledRemovals = new ConcurrentHashMap<>();
        this.temporaryBlocks = new ConcurrentHashMap<>();
    }

    public static void initialize(JavaPlugin plugin, RegionManager regionManager) {
        if (instance == null) {
            instance = new TemporaryBlocksManager(plugin, regionManager);
        }
    }

    public static TemporaryBlocksManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TemporaryBlocksManager no ha sido inicializado");
        }
        return instance;
    }

    public void scheduleBlockRemoval(Region region, Location location, Material material, UUID playerId) {
        if (!region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
            return;  
        }

        String locationKey = getLocationKey(location);

        BukkitTask existingTask = scheduledRemovals.get(locationKey);
        if (existingTask != null) {
            existingTask.cancel();
        }

        int removalSeconds = getRemovalTime(region);

        TemporaryBlock tempBlock = new TemporaryBlock(
                location.clone(),
                material,
                System.currentTimeMillis() + (removalSeconds * 1000L),
                playerId,  
                region.getFlagValue(RegionFlag.RE_GIVE_BLOCKS)  
        );

        temporaryBlocks.put(locationKey, tempBlock);

        BukkitTask removalTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeTemporaryBlock(locationKey, location);
            }
        }.runTaskLater(plugin, removalSeconds * 20L);  

        scheduledRemovals.put(locationKey, removalTask);
    }

    public void cancelBlockRemoval(Location location) {
        String locationKey = getLocationKey(location);

        BukkitTask task = scheduledRemovals.remove(locationKey);
        if (task != null) {
            task.cancel();
        }

        temporaryBlocks.remove(locationKey);
    }

    public boolean isTemporaryBlock(Location location) {
        String locationKey = getLocationKey(location);
        return temporaryBlocks.containsKey(locationKey);
    }

    public TemporaryBlock getTemporaryBlock(Location location) {
        String locationKey = getLocationKey(location);
        return temporaryBlocks.get(locationKey);
    }
    
    private void removeTemporaryBlock(String locationKey, Location location) {
        try {
             
            TemporaryBlock temporaryBlock = temporaryBlocks.get(locationKey);
            if (temporaryBlock == null) {
                logInternalWarn("Intento de remover bloque temporal inexistente: " + locationKey);
                return;
            }

            String regionId = null;
            List<Region> regions = regionManager.getRegionsAt(location);
            if (!regions.isEmpty()) {
                regionId = regions.get(0).getId();
            }

            long placementTime = temporaryBlock.getRemovalTime() - (getRemovalTime(regions.isEmpty() ? null : regions.get(0)) * 1000L);

            TemporaryBlockRemovedEvent event = new TemporaryBlockRemovedEvent(
                    location,
                    temporaryBlock.getMaterial(),
                    regionId,
                    placementTime
            );

            plugin.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                logInternalDebug("Remoción de bloque temporal cancelada por evento: " + locationKey);
                return;
            }

            if (temporaryBlock.isShouldReGiveBlock() && temporaryBlock.getPlayerId() != null) {
                giveBlockBackToPlayer(temporaryBlock);
            }

            location.getBlock().setType(Material.AIR);

            scheduledRemovals.remove(locationKey);
            temporaryBlocks.remove(locationKey);

            if (regionId != null) {
                PlayerBlockTracker.getInstance().removePlayerBlock(regionId, location);
            }

        } catch (Exception e) {
            logInternalWarn("Error removiendo bloque temporal en " + locationKey + ": " + e.getMessage());

            scheduledRemovals.remove(locationKey);
            temporaryBlocks.remove(locationKey);
        }
    }

    private void giveBlockBackToPlayer(TemporaryBlock temporaryBlock) {
        UUID playerId = temporaryBlock.getPlayerId();
        if (playerId == null) {
            logInternalDebug("No se puede devolver bloque: ID de jugador nulo");
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            logInternalDebug(String.format(
                    "No se puede devolver bloque a jugador offline: %s (Material: %s)",
                    playerId, temporaryBlock.getMaterial().name()
            ));
            return;
        }

        ItemStack blockItem = new ItemStack(temporaryBlock.getMaterial(), 1);

        if (player.getInventory().firstEmpty() != -1) {
             
            player.getInventory().addItem(blockItem);

            logInternalDebug(String.format(
                    "Bloque devuelto al inventario: %s recibió %s",
                    player.getName(), temporaryBlock.getMaterial().name()
            ));
        } else {
             
            Location dropLocation = player.getLocation();
            dropLocation.getWorld().dropItemNaturally(dropLocation, blockItem);

            logInternalDebug(String.format(
                    "Bloque dropeado por falta de espacio: %s - %s en %s",
                    player.getName(), temporaryBlock.getMaterial().name(), dropLocation
            ));
        }
    }

    private int getRemovalTime(Region region) {
        if (region == null) {
            return DEFAULT_REMOVAL_SECONDS;
        }
        Integer customTime = region.getMetadata("temporary-blocks-seconds", Integer.class);
        return customTime != null ? customTime : DEFAULT_REMOVAL_SECONDS;
    }

    private String getLocationKey(Location location) {
        return String.format("%s:%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public void shutdown() {
        DebugUtils.logInternalInfo("Cerrando TemporaryBlocksManager...");

        scheduledRemovals.values().forEach(BukkitTask::cancel);
        scheduledRemovals.clear();

        temporaryBlocks.clear();

        DebugUtils.logInternalInfo("TemporaryBlocksManager cerrado");
    }

    public int cancelReGiveForPlayer(UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        int canceledCount = 0;

        for (TemporaryBlock tempBlock : temporaryBlocks.values()) {
             
            if (playerId.equals(tempBlock.getPlayerId()) && tempBlock.isShouldReGiveBlock()) {
                 
                tempBlock.cancelReGive();
                canceledCount++;
            }
        }
        return canceledCount;
    }

    public int cancelReGiveForPlayer(Player player) {
        return cancelReGiveForPlayer(player.getUniqueId());
    }

    public int getPendingReGiveCount(UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        return (int) temporaryBlocks.values().stream()
                .filter(block -> playerId.equals(block.getPlayerId()) && block.isShouldReGiveBlock())
                .count();
    }

    public int getPendingReGiveCount(Player player) {
        return getPendingReGiveCount(player.getUniqueId());
    }

    public TemporaryBlocksStats getStats() {
        int activeRemovals = scheduledRemovals.size();
        int totalBlocks = temporaryBlocks.size();
        int blocksWithReGive = (int) temporaryBlocks.values().stream()
                .filter(TemporaryBlock::isShouldReGiveBlock)
                .count();

        return new TemporaryBlocksStats(activeRemovals, totalBlocks, blocksWithReGive);
    }

    @Getter
    public static class TemporaryBlock {
        private final Location location;
        private final Material material;
        private final long removalTime;
        private final UUID playerId;
        private boolean shouldReGiveBlock;  

        public TemporaryBlock(Location location, Material material, long removalTime) {
            this(location, material, removalTime, null, false);
        }

        public TemporaryBlock(Location location, Material material, long removalTime, UUID playerId, boolean shouldReGiveBlock) {
            this.location = location;
            this.material = material;
            this.removalTime = removalTime;
            this.playerId = playerId;
            this.shouldReGiveBlock = shouldReGiveBlock;
        }

        public void cancelReGive() {
            this.shouldReGiveBlock = false;
        }

        public void enableReGive() {
            this.shouldReGiveBlock = true;
        }

        public long getTimeUntilRemoval() {
            return Math.max(0, removalTime - System.currentTimeMillis());
        }

        public int getSecondsUntilRemoval() {
            return (int) (getTimeUntilRemoval() / 1000);
        }

        @Override
        public String toString() {
            return String.format("TemporaryBlock{material=%s, location=%s, seconds_left=%d, player=%s, reGive=%b}",
                    material.name(), location, getSecondsUntilRemoval(),
                    playerId != null ? playerId.toString() : "unknown", shouldReGiveBlock);
        }
    }

    @Getter
    public static class TemporaryBlocksStats {
        private final int activeRemovals;
        private final int totalBlocks;
        private final int blocksWithReGive;  

        public TemporaryBlocksStats(int activeRemovals, int totalBlocks, int blocksWithReGive) {
            this.activeRemovals = activeRemovals;
            this.totalBlocks = totalBlocks;
            this.blocksWithReGive = blocksWithReGive;
        }

        @Override
        public String toString() {
            return String.format("TemporaryBlocksStats{active_removals=%d, total_blocks=%d, with_regive=%d}",
                    activeRemovals, totalBlocks, blocksWithReGive);
        }
    }
}
