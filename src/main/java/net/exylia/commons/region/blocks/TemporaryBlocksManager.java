package net.exylia.commons.region.blocks;

import lombok.Getter;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manager para manejar bloques temporales que desaparecen automáticamente
 */
public class TemporaryBlocksManager {
    private static TemporaryBlocksManager instance;

    private final JavaPlugin plugin;
    private final ConcurrentMap<String, BukkitTask> scheduledRemovals; // location key -> removal task
    private final ConcurrentMap<String, TemporaryBlock> temporaryBlocks; // location key -> block info

    // Configuración por defecto
    private static final int DEFAULT_REMOVAL_SECONDS = 30;

    private TemporaryBlocksManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduledRemovals = new ConcurrentHashMap<>();
        this.temporaryBlocks = new ConcurrentHashMap<>();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new TemporaryBlocksManager(plugin);
        }
    }

    public static TemporaryBlocksManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TemporaryBlocksManager no ha sido inicializado");
        }
        return instance;
    }

    /**
     * Programa la remoción de un bloque temporal
     */
    public void scheduleBlockRemoval(Region region, Location location, Material material) {
        if (!region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
            return; // La región no tiene bloques temporales habilitados
        }

        String locationKey = getLocationKey(location);

        // Cancelar tarea anterior si existe
        BukkitTask existingTask = scheduledRemovals.get(locationKey);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Obtener tiempo de remoción desde metadata de la región
        int removalSeconds = getRemovalTime(region);

        // Crear información del bloque temporal
        TemporaryBlock tempBlock = new TemporaryBlock(
                location.clone(),
                material,
                System.currentTimeMillis() + (removalSeconds * 1000L)
        );

        temporaryBlocks.put(locationKey, tempBlock);

        // Programar remoción
        BukkitTask removalTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeTemporaryBlock(locationKey, location);
            }
        }.runTaskLater(plugin, removalSeconds * 20L); // convertir a ticks

        scheduledRemovals.put(locationKey, removalTask);

        if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine(String.format(
                    "Bloque temporal programado para remoción: %s en %s (%d segundos)",
                    material.name(), locationKey, removalSeconds
            ));
        }
    }

    /**
     * Cancela la remoción de un bloque temporal (cuando el jugador lo rompe manualmente)
     */
    public void cancelBlockRemoval(Location location) {
        String locationKey = getLocationKey(location);

        // Cancelar tarea
        BukkitTask task = scheduledRemovals.remove(locationKey);
        if (task != null) {
            task.cancel();
        }

        // Remover información
        temporaryBlocks.remove(locationKey);
    }

    /**
     * Verifica si un bloque es temporal
     */
    public boolean isTemporaryBlock(Location location) {
        String locationKey = getLocationKey(location);
        return temporaryBlocks.containsKey(locationKey);
    }

    /**
     * Obtiene información de un bloque temporal
     */
    public TemporaryBlock getTemporaryBlock(Location location) {
        String locationKey = getLocationKey(location);
        return temporaryBlocks.get(locationKey);
    }

    /**
     * Remueve un bloque temporal del mundo
     */
    private void removeTemporaryBlock(String locationKey, Location location) {
        try {
            // Remover bloque del mundo
            location.getBlock().setType(Material.AIR);

            // Limpiar registros
            scheduledRemovals.remove(locationKey);
            TemporaryBlock removedBlock = temporaryBlocks.remove(locationKey);

            // También remover del tracker de bloques de jugador si existe
            if (removedBlock != null) {
                // Notificar al PlayerBlockTracker que el bloque fue removido
                // (esto es importante para mantener la consistencia)
                plugin.getServer().getPluginManager().callEvent(
                        new org.bukkit.event.block.BlockBreakEvent(location.getBlock(), null)
                );
            }

            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().fine("Bloque temporal removido automáticamente: " + locationKey);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error removiendo bloque temporal en " + locationKey + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene el tiempo de remoción desde la metadata de la región
     */
    private int getRemovalTime(Region region) {
        Integer customTime = region.getMetadata("temporary-blocks-seconds", Integer.class);
        return customTime != null ? customTime : DEFAULT_REMOVAL_SECONDS;
    }

    /**
     * Genera clave única para una ubicación
     */
    private String getLocationKey(Location location) {
        return String.format("%s:%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Limpia todas las tareas pendientes y datos
     */
    public void shutdown() {
        plugin.getLogger().info("Cerrando TemporaryBlocksManager...");

        // Cancelar todas las tareas pendientes
        scheduledRemovals.values().forEach(BukkitTask::cancel);
        scheduledRemovals.clear();

        // Limpiar datos
        temporaryBlocks.clear();

        plugin.getLogger().info("TemporaryBlocksManager cerrado");
    }

    /**
     * Obtiene estadísticas del sistema
     */
    public TemporaryBlocksStats getStats() {
        int activeRemovals = scheduledRemovals.size();
        int totalBlocks = temporaryBlocks.size();

        return new TemporaryBlocksStats(activeRemovals, totalBlocks);
    }

    /**
     * Clase para información de bloques temporales
     */
    @Getter
    public static class TemporaryBlock {
        private final Location location;
        private final Material material;
        private final long removalTime; // timestamp cuando debe ser removido

        public TemporaryBlock(Location location, Material material, long removalTime) {
            this.location = location;
            this.material = material;
            this.removalTime = removalTime;
        }

        public long getTimeUntilRemoval() {
            return Math.max(0, removalTime - System.currentTimeMillis());
        }

        public int getSecondsUntilRemoval() {
            return (int) (getTimeUntilRemoval() / 1000);
        }

        @Override
        public String toString() {
            return String.format("TemporaryBlock{material=%s, location=%s, seconds_left=%d}",
                    material.name(), location, getSecondsUntilRemoval());
        }
    }

    /**
     * Clase para estadísticas
     */
    @Getter
    public static class TemporaryBlocksStats {
        private final int activeRemovals;
        private final int totalBlocks;

        public TemporaryBlocksStats(int activeRemovals, int totalBlocks) {
            this.activeRemovals = activeRemovals;
            this.totalBlocks = totalBlocks;
        }

        @Override
        public String toString() {
            return String.format("TemporaryBlocksStats{active_removals=%d, total_blocks=%d}",
                    activeRemovals, totalBlocks);
        }
    }
}