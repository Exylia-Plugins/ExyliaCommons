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

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

/**
 * Manager para manejar bloques temporales que desaparecen automáticamente
 */
public class TemporaryBlocksManager {
    private static TemporaryBlocksManager instance;

    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final ConcurrentMap<String, BukkitTask> scheduledRemovals; // location key -> removal task
    private final ConcurrentMap<String, TemporaryBlock> temporaryBlocks; // location key -> block info

    // Configuración por defecto
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

        // NUEVO: Crear información del bloque temporal con información del jugador
        TemporaryBlock tempBlock = new TemporaryBlock(
                location.clone(),
                material,
                System.currentTimeMillis() + (removalSeconds * 1000L),
                playerId, // NUEVO: Guardar ID del jugador que colocó el bloque
                region.getFlagValue(RegionFlag.RE_GIVE_BLOCKS) // NUEVO: Verificar si debe devolver el bloque
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
    }

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
            // Obtener información del bloque antes de removerlo
            TemporaryBlock temporaryBlock = temporaryBlocks.get(locationKey);
            if (temporaryBlock == null) {
                plugin.getLogger().warning("Intento de remover bloque temporal inexistente: " + locationKey);
                return;
            }

            // Obtener la región para el evento
            String regionId = null;
            List<Region> regions = regionManager.getRegionsAt(location);
            if (!regions.isEmpty()) {
                regionId = regions.get(0).getId();
            }

            // Calcular tiempo de colocación (aproximado)
            long placementTime = temporaryBlock.getRemovalTime() - (getRemovalTime(regions.isEmpty() ? null : regions.get(0)) * 1000L);

            // Crear y disparar el evento personalizado ANTES de remover el bloque
            TemporaryBlockRemovedEvent event = new TemporaryBlockRemovedEvent(
                    location,
                    temporaryBlock.getMaterial(),
                    regionId,
                    placementTime
            );

            plugin.getServer().getPluginManager().callEvent(event);

            // Si el evento fue cancelado, no remover el bloque
            if (event.isCancelled()) {
                logInternalDebug(debug(), "Remoción de bloque temporal cancelada por evento: " + locationKey);
                return;
            }

            // NUEVO: Manejar devolución de bloque si RE_GIVE_BLOCKS está activo
            if (temporaryBlock.isShouldReGiveBlock() && temporaryBlock.getPlayerId() != null) {
                giveBlockBackToPlayer(temporaryBlock);
            }

            // Remover bloque del mundo
            location.getBlock().setType(Material.AIR);

            // Limpiar registros
            scheduledRemovals.remove(locationKey);
            temporaryBlocks.remove(locationKey);

            // Notificar al PlayerBlockTracker para mantener consistencia
            // Solo si tenemos una región válida
            if (regionId != null) {
                PlayerBlockTracker.getInstance().removePlayerBlock(regionId, location);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error removiendo bloque temporal en " + locationKey + ": " + e.getMessage());

            // Limpiar registros incluso si hay error, para evitar memory leaks
            scheduledRemovals.remove(locationKey);
            temporaryBlocks.remove(locationKey);
        }
    }

    private void giveBlockBackToPlayer(TemporaryBlock temporaryBlock) {
        UUID playerId = temporaryBlock.getPlayerId();
        if (playerId == null) {
            logInternalDebug(debug(), "No se puede devolver bloque: ID de jugador nulo");
            return;
        }

        // Verificar si el jugador está online
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            logInternalDebug(debug(), String.format(
                    "No se puede devolver bloque a jugador offline: %s (Material: %s)",
                    playerId, temporaryBlock.getMaterial().name()
            ));
            return;
        }

        // Crear ItemStack del material
        ItemStack blockItem = new ItemStack(temporaryBlock.getMaterial(), 1);

        // Intentar añadir al inventario
        if (player.getInventory().firstEmpty() != -1) {
            // Hay espacio en el inventario
            player.getInventory().addItem(blockItem);

            logInternalDebug(debug(), String.format(
                    "Bloque devuelto al inventario: %s recibió %s",
                    player.getName(), temporaryBlock.getMaterial().name()
            ));
        } else {
            // No hay espacio, dropearlo en la ubicación del jugador
            Location dropLocation = player.getLocation();
            dropLocation.getWorld().dropItemNaturally(dropLocation, blockItem);

            logInternalDebug(debug(), String.format(
                    "Bloque dropeado por falta de espacio: %s - %s en %s",
                    player.getName(), temporaryBlock.getMaterial().name(), dropLocation
            ));
        }
    }

    /**
     * Obtiene el tiempo de remoción desde la metadata de la región
     */
    private int getRemovalTime(Region region) {
        if (region == null) {
            return DEFAULT_REMOVAL_SECONDS;
        }
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
        DebugUtils.logInternalInfo("Cerrando TemporaryBlocksManager...");

        // Cancelar todas las tareas pendientes
        scheduledRemovals.values().forEach(BukkitTask::cancel);
        scheduledRemovals.clear();

        // Limpiar datos
        temporaryBlocks.clear();

        DebugUtils.logInternalInfo("TemporaryBlocksManager cerrado");
    }

    public int cancelReGiveForPlayer(UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        int canceledCount = 0;

        // Recorrer todos los bloques temporales
        for (TemporaryBlock tempBlock : temporaryBlocks.values()) {
            // Si el bloque pertenece al jugador y tiene re-give activo
            if (playerId.equals(tempBlock.getPlayerId()) && tempBlock.isShouldReGiveBlock()) {
                // Marcar que no debe devolver el bloque (modificar in-place)
                tempBlock.cancelReGive();
                canceledCount++;

                logInternalDebug(debug(), String.format(
                        "Re-give cancelado para bloque: %s de jugador %s en %s",
                        tempBlock.getMaterial().name(),
                        playerId.toString(),
                        tempBlock.getLocation()
                ));
            }
        }

        logInternalDebug(debug(), String.format(
                "Cancelados %d re-gives para jugador %s (bloques seguirán desapareciendo)",
                canceledCount, playerId.toString()
        ));

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

    /**
     * Obtiene estadísticas del sistema
     */
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
        private boolean shouldReGiveBlock; // CAMBIADO: Ya no es final para poder modificarlo

        // Constructor original para compatibilidad
        public TemporaryBlock(Location location, Material material, long removalTime) {
            this(location, material, removalTime, null, false);
        }

        // NUEVO: Constructor con información del jugador y RE_GIVE_BLOCKS
        public TemporaryBlock(Location location, Material material, long removalTime, UUID playerId, boolean shouldReGiveBlock) {
            this.location = location;
            this.material = material;
            this.removalTime = removalTime;
            this.playerId = playerId;
            this.shouldReGiveBlock = shouldReGiveBlock;
        }

        /**
         * NUEVO: Cancela la devolución de este bloque específico
         * El bloque seguirá desapareciendo normalmente
         */
        public void cancelReGive() {
            this.shouldReGiveBlock = false;
        }

        /**
         * NUEVO: Reactiva la devolución de este bloque (si es que se había cancelado)
         */
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
        private final int blocksWithReGive; // NUEVO

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