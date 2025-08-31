package net.exylia.commons.item.expiration;

import net.exylia.commons.item.InteractiveItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener que intercepta interacciones con items para verificar su expiración
 */
public class ExpirationListener implements Listener {

    private final ExpirationManager expirationManager;

    public ExpirationListener() {
        this.expirationManager = ExpirationManager.getInstance();
    }

    /**
     * Verifica items expirados cuando un jugador se conecta
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Verificar inventario después de un pequeño delay para asegurar que esté completamente cargado
        if (expirationManager != null && expirationManager.isEnabled()) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExyliaCommons"), // Ajusta el nombre del plugin
                () -> expirationManager.checkPlayerInventoryForExpiredItems(event.getPlayer()),
                20L // 1 segundo de delay
            );
        }
    }

    /**
     * Intercepta interacciones con items (click derecho/izquierdo)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        if (!canUseExpiredItem(item, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Intercepta el consumo de items (comida, pociones, etc.)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        if (!canUseExpiredItem(item, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Intercepta la colocación de bloques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null) return;

        if (!canUseExpiredItem(item, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Intercepta clicks en inventarios
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item == null) return;

        // Verificar si el item está expirado y debería ser eliminado/transformado
        if (InteractiveItem.hasExpirationTime(item) && InteractiveItem.isItemStackExpired(item)) {
            // Forzar una verificación del inventario después de este click
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExyliaCommons"), // Ajusta el nombre del plugin
                () -> {
                    if (expirationManager != null) {
                        expirationManager.checkPlayerInventoryForExpiredItems(player);
                    }
                },
                1L // 1 tick de delay
            );
        }

        // Si es un item que no puede usarse, cancelar la acción
        if (!canUseExpiredItem(item, player)) {
            // No cancelamos completamente porque podría estar moviendo el item
            // pero sí verificamos si debería eliminarse
            return;
        }
    }

    /**
     * Actualiza placeholders cuando se abre un inventario
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Actualizar placeholders con un pequeño delay para asegurar que el inventario esté completamente abierto
        if (expirationManager != null && expirationManager.isUpdatePlaceholders()) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExyliaCommons"), // Ajusta el nombre del plugin
                () -> expirationManager.updatePlaceholdersForPlayer(player),
                3L // 3 ticks de delay
            );
        }
    }

    /**
     * Verifica si un item expirado puede usarse según su comportamiento
     */
    private boolean canUseExpiredItem(ItemStack item, Player player) {
        if (expirationManager == null) {
            return true;
        }

        return expirationManager.canUseItem(item, player);
    }
}