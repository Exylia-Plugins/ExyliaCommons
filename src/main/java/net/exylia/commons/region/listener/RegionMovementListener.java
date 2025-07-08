package net.exylia.commons.region.listener;

import net.exylia.commons.region.RegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener que maneja el movimiento de jugadores para detectar entrada/salida de regiones
 * Con prioridad alta para asegurar que se ejecute antes que otros plugins
 */
public class RegionMovementListener implements Listener {
    private final RegionManager regionManager;
    private final JavaPlugin plugin;

    public RegionMovementListener(RegionManager regionManager, JavaPlugin plugin) {
        this.regionManager = regionManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Optimización: Solo procesar si realmente se movió a un bloque diferente
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ() && from.getWorld().equals(to.getWorld())) {
            return;
        }

        // Si el movimiento fue cancelado por otro plugin, verificar si nuestras regiones lo permiten
        boolean movementAllowed = regionManager.processPlayerMovement(event.getPlayer(), from, to);

        // Si nuestro sistema permite el movimiento y fue cancelado, descancelar
        if (movementAllowed && event.isCancelled()) {
            event.setCancelled(false);
        } else if (!movementAllowed) {
            // Si nuestro sistema no permite el movimiento, cancelar
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Tratar el teletransporte como movimiento instantáneo
        boolean teleportAllowed = regionManager.processPlayerMovement(event.getPlayer(), from, to);

        // Si nuestro sistema permite el teletransporte y fue cancelado, descancelar
        if (teleportAllowed && event.isCancelled()) {
            event.setCancelled(false);
        } else if (!teleportAllowed) {
            // Si nuestro sistema no permite el teletransporte, cancelar
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        // Simular movimiento desde una ubicación "vacía" para detectar regiones al conectarse
        Location emptyLocation = new Location(location.getWorld(), 0, -1000, 0);

        // Programar después de un tick para asegurar que el jugador esté completamente cargado
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> regionManager.processPlayerMovement(player, emptyLocation, player.getLocation()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpiar datos del jugador
        regionManager.cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        // Limpiar datos del jugador
        regionManager.cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Simular salida del mundo anterior
        Location oldWorldLocation = new Location(event.getFrom(), 0, 0, 0);
        Location newWorldLocation = player.getLocation();

        regionManager.processPlayerMovement(player, oldWorldLocation, newWorldLocation);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Programar verificación de regiones después del respawn
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location respawnLocation = event.getRespawnLocation();
            Location deathLocation = new Location(respawnLocation.getWorld(), 0, -500, 0); // Ubicación temporal
            regionManager.processPlayerMovement(player, deathLocation, player.getLocation());
        }, 1L);
    }
}