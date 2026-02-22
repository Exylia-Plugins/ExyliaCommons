package net.exylia.commons.v2.region.blocks;

import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
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
    private final Map<BlockKey, ScheduledTask> activeTasks;

    private TemporaryBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.temporaryBlocks = new ConcurrentHashMap<>();
        this.activeTasks = new ConcurrentHashMap<>();
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

        ScheduledTask previousTask = activeTasks.remove(key);
        if (previousTask != null) {
            previousTask.cancel();
        }

        TemporaryBlock tempBlock = new TemporaryBlock(location, player.getUniqueId(), material, System.currentTimeMillis(), seconds, reGiveBlock);
        temporaryBlocks.put(key, tempBlock);

        final ScheduledTask[] taskRef = new ScheduledTask[1];
        taskRef[0] = Tasks.later(() -> {
            try {
                removeTemporaryBlock(key, tempBlock.playerId, tempBlock.material, tempBlock.reGiveBlock);
            } finally {
                activeTasks.remove(key, taskRef[0]);
            }
        }, 20L * seconds);

        activeTasks.put(key, taskRef[0]);
    }

    private void removeTemporaryBlock(BlockKey key, UUID playerId, Material originalMaterial, boolean reGiveBlock) {
        TemporaryBlock removed = temporaryBlocks.remove(key);
        if (removed == null) {
            return;
        }

        Location location = removed.location;
        Block block = location.getBlock();
        block.setType(Material.AIR);

        if (reGiveBlock) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }

            ItemStack item = new ItemStack(originalMaterial, 1);
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
        ScheduledTask task = activeTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    public int clearAll() {
        int count = temporaryBlocks.size();

        for (TemporaryBlock tempBlock : temporaryBlocks.values()) {
            Location location = tempBlock.location;
            Block block = location.getBlock();
            block.setType(Material.AIR);
        }

        temporaryBlocks.clear();

        for (ScheduledTask task : activeTasks.values()) {
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
        synchronized (TemporaryBlockManager.class) {
            if (instance == this) {
                instance = null;
            }
        }
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
