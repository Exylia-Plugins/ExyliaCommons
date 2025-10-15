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

public class ExpirationListener implements Listener {

    private final ExpirationManager expirationManager;

    public ExpirationListener() {
        this.expirationManager = ExpirationManager.getInstance();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
         
        if (expirationManager != null && expirationManager.isEnabled()) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExyliaCommons"),  
                () -> expirationManager.checkPlayerInventoryForExpiredItems(event.getPlayer()),
                20L  
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        if (!canUseExpiredItem(item, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        if (!canUseExpiredItem(item, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null) return;

        if (!canUseExpiredItem(item, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item == null) return;

        if (InteractiveItem.hasExpirationTime(item) && InteractiveItem.isItemStackExpired(item)) {
             
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExyliaCommons"),  
                () -> {
                    if (expirationManager != null) {
                        expirationManager.checkPlayerInventoryForExpiredItems(player);
                    }
                },
                1L  
            );
        }

        if (!canUseExpiredItem(item, player)) {
             
            return;
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        if (expirationManager != null && expirationManager.isUpdatePlaceholders()) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExyliaCommons"),  
                () -> expirationManager.updatePlaceholdersForPlayer(player),
                3L  
            );
        }
    }

    private boolean canUseExpiredItem(ItemStack item, Player player) {
        if (expirationManager == null) {
            return true;
        }

        return expirationManager.canUseItem(item, player);
    }
}
