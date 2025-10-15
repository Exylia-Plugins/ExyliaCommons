package net.exylia.commons.selection.visualizer;

import lombok.Getter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleVisualizer {
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, BukkitRunnable>> particleTasks;
    @Getter
    private final ParticleConfig config;

    public ParticleVisualizer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.particleTasks = new ConcurrentHashMap<>();
        this.config = new ParticleConfig();
    }

    public ParticleVisualizer(JavaPlugin plugin, ParticleConfig config) {
        this.plugin = plugin;
        this.particleTasks = new ConcurrentHashMap<>();
        this.config = config;
    }

    public void showSelection(Player player, Selection selection) {
        if (!selection.isComplete()) {
            return;
        }

        clearSelection(player, selection.getSelectionId());

        Location pos1 = selection.getPos1();
        Location pos2 = selection.getPos2();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX()) + 1;
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY()) + 1;
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) + 1;

        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    removeTask(player.getUniqueId(), selection.getSelectionId());
                    return;
                }

                long volume = selection.getVolume();
                if (volume > config.getMaxVolumeForVisualization()) {
                    showOutlineOnly(player, pos1, minX, maxX, minY, maxY, minZ, maxZ);
                } else {
                    showFullOutline(player, pos1, minX, maxX, minY, maxY, minZ, maxZ);
                }
            }
        };

        particleTask.runTaskTimer(plugin, 0, config.getUpdateInterval());

        particleTasks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(selection.getSelectionId(), particleTask);
    }

    public void clearSelection(Player player, String selectionId) {
        UUID playerId = player.getUniqueId();
        Map<String, BukkitRunnable> playerTasks = particleTasks.get(playerId);

        if (playerTasks != null) {
            BukkitRunnable task = playerTasks.remove(selectionId);
            if (task != null) {
                task.cancel();
            }
        }
    }

    public void clearAll(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, BukkitRunnable> playerTasks = particleTasks.remove(playerId);

        if (playerTasks != null) {
            playerTasks.values().forEach(BukkitRunnable::cancel);
        }
    }

    public void updateSelection(Player player, Selection selection) {
        if (isSelectionVisible(player, selection.getSelectionId())) {
            showSelection(player, selection);
        }
    }

    public boolean isSelectionVisible(Player player, String selectionId) {
        return true;
 
    }

    public void cleanup() {
        particleTasks.values().forEach(playerTasks ->
                playerTasks.values().forEach(BukkitRunnable::cancel)
        );
        particleTasks.clear();
    }

    private void showFullOutline(Player player, Location world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
         
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                spawnParticle(player, world, x, y, minZ);
                spawnParticle(player, world, x, y, maxZ);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                spawnParticle(player, world, minX, y, z);
                spawnParticle(player, world, maxX, y, z);
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                spawnParticle(player, world, x, minY, z);
                spawnParticle(player, world, x, maxY, z);
            }
        }
    }

    private void showOutlineOnly(Player player, Location world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
         
        int step = Math.max(1, (maxX - minX) / 20);  

        for (int x = minX; x <= maxX; x += step) {
            spawnParticle(player, world, x, minY, minZ);
            spawnParticle(player, world, x, minY, maxZ);
            spawnParticle(player, world, x, maxY, minZ);
            spawnParticle(player, world, x, maxY, maxZ);
        }

        for (int y = minY; y <= maxY; y += step) {
            spawnParticle(player, world, minX, y, minZ);
            spawnParticle(player, world, minX, y, maxZ);
            spawnParticle(player, world, maxX, y, minZ);
            spawnParticle(player, world, maxX, y, maxZ);
        }

        for (int z = minZ; z <= maxZ; z += step) {
            spawnParticle(player, world, minX, minY, z);
            spawnParticle(player, world, minX, maxY, z);
            spawnParticle(player, world, maxX, minY, z);
            spawnParticle(player, world, maxX, maxY, z);
        }
    }

    private void spawnParticle(Player player, Location world, int x, int y, int z) {
        Location particleLoc = new Location(world.getWorld(), x, y, z);
        player.spawnParticle(
                config.getParticleType(),
                particleLoc,
                config.getParticleCount(),
                config.getOffsetX(),
                config.getOffsetY(),
                config.getOffsetZ(),
                config.getSpeed()
        );
    }

    private void removeTask(UUID playerId, String selectionId) {
        Map<String, BukkitRunnable> playerTasks = particleTasks.get(playerId);
        if (playerTasks != null) {
            playerTasks.remove(selectionId);
            if (playerTasks.isEmpty()) {
                particleTasks.remove(playerId);
            }
        }
    }
}
