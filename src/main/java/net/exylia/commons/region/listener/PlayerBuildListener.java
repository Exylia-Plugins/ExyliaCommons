package net.exylia.commons.region.listener;

import net.exylia.commons.region.RegionManager;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.List;

/**
 * Listener que maneja la construcción protegida con rastreo de bloques de jugador
 * Con prioridad alta para overridear otros plugins como WorldGuard
 */
public class PlayerBuildListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerBlockTracker blockTracker;
    private final RegionManager regionManager;

    public PlayerBuildListener(JavaPlugin plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.blockTracker = PlayerBlockTracker.getInstance();
        this.regionManager = regionManager;

        // Registrar el listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Obtener la región de mayor prioridad
        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0); // Mayor prioridad

        // Si la región tiene rastreo de bloques habilitado
        if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            // Registrar que el jugador colocó este bloque
            blockTracker.addPlayerBlock(region.getId(), location, event.getBlock().getType());

            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().fine("Bloque registrado: " + player.getName() + " colocó " +
                        event.getBlock().getType() + " en " + region.getId() + " " +
                        location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
            }

            // Si nuestro sistema permite la construcción, descancelar si fue cancelado
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Obtener la región de mayor prioridad
        List<Region> regions = regionManager.getRegionsAt(location);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0); // Mayor prioridad
        boolean shouldAllow = true;

        // Si la región tiene construcción solo de jugadores habilitada
        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            // Verificar si el bloque fue colocado por un jugador
            if (!blockTracker.isPlayerPlacedBlock(region.getId(), location)) {
                // Es un bloque original de la región, no permitir romper
                shouldAllow = false;
            }
        }

        // Aplicar resultado
        if (shouldAllow) {
            // Si nuestro sistema permite la acción, descancelar si fue cancelado
            if (event.isCancelled()) {
                event.setCancelled(false);
            }

            // Si la región tiene rastreo habilitado, remover el bloque del registro
            if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
                blockTracker.removePlayerBlock(region.getId(), location);

                if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                    plugin.getLogger().fine("Bloque removido del registro: " + player.getName() + " rompió bloque en " +
                            region.getId() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                }
            }
        } else {
            // Si nuestro sistema no permite la acción, cancelar
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        Location explosionLocation = event.getLocation();

        // Obtener regiones en la ubicación de la explosión
        List<Region> regions = regionManager.getRegionsAt(explosionLocation);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0); // Mayor prioridad

        // Si la región tiene construcción solo de jugadores habilitada
        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            // Filtrar bloques que pueden ser destruidos
            Iterator<Block> iterator = event.blockList().iterator();
            boolean hasAllowedBlocks = false;

            while (iterator.hasNext()) {
                Block block = iterator.next();
                Location blockLocation = block.getLocation();

                // Verificar si el bloque fue colocado por un jugador
                if (!blockTracker.isPlayerPlacedBlock(region.getId(), blockLocation)) {
                    // Es un bloque original de la región, no puede ser destruido
                    iterator.remove();
                } else {
                    // Es un bloque de jugador, puede ser destruido
                    hasAllowedBlocks = true;
                    // Removerlo del registro
                    if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
                        blockTracker.removePlayerBlock(region.getId(), blockLocation);
                    }
                }
            }

            // Si hay bloques permitidos para destruir y el evento fue cancelado, descancelar
            if (hasAllowedBlocks && event.isCancelled()) {
                event.setCancelled(false);
            }

            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().fine("Explosión filtrada en región " + region.getId() + ": " +
                        event.blockList().size() + " bloques pueden ser destruidos");
            }
        } else if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            // Solo rastreo, descancelar si fue cancelado y remover bloques del registro
            if (event.isCancelled()) {
                event.setCancelled(false);
            }

            for (Block block : event.blockList()) {
                blockTracker.removePlayerBlock(region.getId(), block.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location explosionLocation = event.getBlock().getLocation();

        // Obtener regiones en la ubicación de la explosión
        List<Region> regions = regionManager.getRegionsAt(explosionLocation);
        if (regions.isEmpty()) {
            return;
        }

        Region region = regions.get(0); // Mayor prioridad

        // Si la región tiene construcción solo de jugadores habilitada
        if (region.getFlagValue(RegionFlag.PLAYER_BUILD_ONLY)) {
            // Filtrar bloques que pueden ser destruidos
            Iterator<Block> iterator = event.blockList().iterator();
            boolean hasAllowedBlocks = false;

            while (iterator.hasNext()) {
                Block block = iterator.next();
                Location blockLocation = block.getLocation();

                // Verificar si el bloque fue colocado por un jugador
                if (!blockTracker.isPlayerPlacedBlock(region.getId(), blockLocation)) {
                    // Es un bloque original de la región, no puede ser destruido
                    iterator.remove();
                } else {
                    // Es un bloque de jugador, puede ser destruido
                    hasAllowedBlocks = true;
                    // Removerlo del registro
                    if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
                        blockTracker.removePlayerBlock(region.getId(), blockLocation);
                    }
                }
            }

            // Si hay bloques permitidos para destruir y el evento fue cancelado, descancelar
            if (hasAllowedBlocks && event.isCancelled()) {
                event.setCancelled(false);
            }

            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().fine("Explosión de bloque filtrada en región " + region.getId() + ": " +
                        event.blockList().size() + " bloques pueden ser destruidos");
            }
        } else if (region.getFlagValue(RegionFlag.TRACK_PLAYER_BLOCKS)) {
            // Solo rastreo, descancelar si fue cancelado y remover bloques del registro
            if (event.isCancelled()) {
                event.setCancelled(false);
            }

            for (Block block : event.blockList()) {
                blockTracker.removePlayerBlock(region.getId(), block.getLocation());
            }
        }
    }
}