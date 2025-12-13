package net.exylia.commons.v2.region.selection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SelectionListener implements Listener {
    private final JavaPlugin plugin;
    private final SelectionManager selectionManager;

    public SelectionListener(JavaPlugin plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!selectionManager.isWand(item)) {
            return;
        }

        event.setCancelled(true);

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() == Material.AIR) {
            return;
        }

        Location location = clickedBlock.getLocation();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setPos1(player, location);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setPos2(player, location);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        selectionManager.cleanup(event.getPlayer());
    }
}
