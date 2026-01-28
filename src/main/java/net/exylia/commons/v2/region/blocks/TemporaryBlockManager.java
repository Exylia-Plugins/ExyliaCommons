package net.exylia.commons.v2.region.blocks;

import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemporaryBlockManager {
    private static TemporaryBlockManager instance;

    private final JavaPlugin plugin;
    private final Map<BlockKey, TemporaryBlock> temporaryBlocks;
    private final Set<ScheduledTask> activeTasks;

    private TemporaryBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.temporaryBlocks = new ConcurrentHashMap<>();
        this.activeTasks = ConcurrentHashMap.newKeySet();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new TemporaryBlockManager(plugin);
        }
    }

    public static TemporaryBlockManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TemporaryBlockManager has not been initialized");
        }
        return instance;
    }

    public void addTemporaryBlock(Location location, Player player, int seconds, boolean reGiveBlock) {
        Block block = location.getBlock();
        Material material = block.getType();
        BlockKey key = new BlockKey(location);

        TemporaryBlock tempBlock = new TemporaryBlock(location, player.getUniqueId(), material, System.currentTimeMillis(), seconds, reGiveBlock);
        temporaryBlocks.put(key, tempBlock);

        ScheduledTask task = Tasks.later(() -> {
            removeTemporaryBlock(location, player, reGiveBlock);
        }, 20L * seconds);

        activeTasks.add(task);
    }

    private void removeTemporaryBlock(Location location, Player player, boolean reGiveBlock) {
        Block block = location.getBlock();
        Material material = block.getType();

        block.setType(Material.AIR);

        BlockKey key = new BlockKey(location);
        temporaryBlocks.remove(key);

        if (reGiveBlock && player != null && player.isOnline()) {
            ItemStack item = new ItemStack(material, 1);
            player.getInventory().addItem(item);
        }
    }

    public boolean isTemporaryBlock(Location location) {
        BlockKey key = new BlockKey(location);
        return temporaryBlocks.containsKey(key);
    }

    public TemporaryBlock getTemporaryBlock(Location location) {
        BlockKey key = new BlockKey(location);
        return temporaryBlocks.get(key);
    }

    public void cancelBlockRemoval(Location location) {
        BlockKey key = new BlockKey(location);
        temporaryBlocks.remove(key);
    }

    public int clearAll() {
        int count = temporaryBlocks.size();

        for (TemporaryBlock tempBlock : temporaryBlocks.values()) {
            Location location = tempBlock.getLocation();
            Block block = location.getBlock();
            block.setType(Material.AIR);
        }

        temporaryBlocks.clear();

        for (ScheduledTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();

        return count;
    }

    public ManagerStats getStats() {
        return new ManagerStats(temporaryBlocks.size(), activeTasks.size());
    }

    public void shutdown() {
        clearAll();
    }

    private record BlockKey(String world, int x, int y, int z) {
        BlockKey(Location location) {
            this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    @Getter
    public static class TemporaryBlock {
        private final Location location;
        private final UUID playerId;
        private final Material material;
        private final long placedAt;
        private final int durationSeconds;
        private final boolean reGiveBlock;

        public TemporaryBlock(Location location, UUID playerId, Material material, long placedAt, int durationSeconds, boolean reGiveBlock) {
            this.location = location;
            this.playerId = playerId;
            this.material = material;
            this.placedAt = placedAt;
            this.durationSeconds = durationSeconds;
            this.reGiveBlock = reGiveBlock;
        }

        public long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - placedAt;
            long remaining = (durationSeconds * 1000L) - elapsed;
            return Math.max(0, remaining);
        }

        public boolean isExpired() {
            return getRemainingTime() == 0;
        }

        @Override
        public String toString() {
            return String.format("TemporaryBlock{%s, %s, remaining=%dms}", location, material, getRemainingTime());
        }
    }

    @Getter
    public static class ManagerStats {
        private final int activeBlocks;
        private final int activeTasks;

        public ManagerStats(int activeBlocks, int activeTasks) {
            this.activeBlocks = activeBlocks;
            this.activeTasks = activeTasks;
        }

        @Override
        public String toString() {
            return String.format("ManagerStats{blocks=%d, tasks=%d}", activeBlocks, activeTasks);
        }
    }
}
