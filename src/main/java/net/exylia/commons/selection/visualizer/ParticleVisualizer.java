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

/**
 * Maneja la visualización de selecciones mediante partículas
 */
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

    /**
     * Muestra partículas para una selección
     */
    public void showSelection(Player player, Selection selection) {
        if (!selection.isComplete()) {
            return;
        }

        // Limpiar partículas existentes para esta selección
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

                // Verificar límites de rendimiento
                long volume = selection.getVolume();
                if (volume > config.getMaxVolumeForVisualization()) {
                    showOutlineOnly(player, pos1, minX, maxX, minY, maxY, minZ, maxZ);
                } else {
                    showFullOutline(player, pos1, minX, maxX, minY, maxY, minZ, maxZ);
                }
            }
        };

        particleTask.runTaskTimer(plugin, 0, config.getUpdateInterval());

        // Guardar la tarea
        particleTasks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(selection.getSelectionId(), particleTask);
    }

    /**
     * Oculta las partículas de una selección específica
     */
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

    /**
     * Oculta todas las partículas de un jugador
     */
    public void clearAll(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, BukkitRunnable> playerTasks = particleTasks.remove(playerId);

        if (playerTasks != null) {
            playerTasks.values().forEach(BukkitRunnable::cancel);
        }
    }

    /**
     * Actualiza la visualización de una selección existente
     */
    public void updateSelection(Player player, Selection selection) {
        if (isSelectionVisible(player, selection.getSelectionId())) {
            showSelection(player, selection);
        }
    }

    /**
     * Verifica si una selección está siendo visualizada
     */
    public boolean isSelectionVisible(Player player, String selectionId) {
        return true;
//        Map<String, BukkitRunnable> playerTasks = particleTasks.get(player.getUniqueId());
//        return playerTasks != null && playerTasks.containsKey(selectionId);
    }

    /**
     * Limpia todas las tareas al descargar el plugin
     */
    public void cleanup() {
        particleTasks.values().forEach(playerTasks ->
                playerTasks.values().forEach(BukkitRunnable::cancel)
        );
        particleTasks.clear();
    }

    // ===== MÉTODOS PRIVADOS =====

    private void showFullOutline(Player player, Location world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Caras en Z (frente y atrás)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                spawnParticle(player, world, x, y, minZ);
                spawnParticle(player, world, x, y, maxZ);
            }
        }

        // Caras en X (izquierda y derecha)
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                spawnParticle(player, world, minX, y, z);
                spawnParticle(player, world, maxX, y, z);
            }
        }

        // Caras en Y (arriba y abajo)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                spawnParticle(player, world, x, minY, z);
                spawnParticle(player, world, x, maxY, z);
            }
        }
    }

    private void showOutlineOnly(Player player, Location world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Solo mostrar las aristas para selecciones muy grandes
        int step = Math.max(1, (maxX - minX) / 20); // Máximo 20 partículas por arista

        // Aristas en X
        for (int x = minX; x <= maxX; x += step) {
            spawnParticle(player, world, x, minY, minZ);
            spawnParticle(player, world, x, minY, maxZ);
            spawnParticle(player, world, x, maxY, minZ);
            spawnParticle(player, world, x, maxY, maxZ);
        }

        // Aristas en Y
        for (int y = minY; y <= maxY; y += step) {
            spawnParticle(player, world, minX, y, minZ);
            spawnParticle(player, world, minX, y, maxZ);
            spawnParticle(player, world, maxX, y, minZ);
            spawnParticle(player, world, maxX, y, maxZ);
        }

        // Aristas en Z
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