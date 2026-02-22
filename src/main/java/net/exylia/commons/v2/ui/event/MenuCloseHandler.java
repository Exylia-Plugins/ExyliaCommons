package net.exylia.commons.v2.ui.event;

import net.exylia.commons.v2.ui.core.MenuManager;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.packet.ContainerIdTracker;
import net.exylia.commons.v2.ui.pagination.PaginationTracker;
import net.exylia.commons.v2.ui.snapshot.AutoSnapshotHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class MenuCloseHandler implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Optional<MenuBase> menuOptional = MenuManager.getInstance().getActiveMenu(player);
        if (menuOptional.isEmpty()) {
            return;
        }

        MenuBase menu = menuOptional.get();

        if (!event.getInventory().equals(menu.getInventory())) {
            return;
        }

        if (menu.getState().get() == MenuState.OPEN) {
            menu.close();
            MenuManager.getInstance().getRegistry().unregister(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MenuManager manager = MenuManager.getInstance();

        Optional<MenuBase> menuOptional = manager.getActiveMenu(player);
        menuOptional.ifPresent(menu -> {
            menu.close();
            manager.getRegistry().unregister(player.getUniqueId());
        });

        manager.clearHistory(player);
        PaginationTracker.clearPlayer(player.getUniqueId());
        ContainerIdTracker.remove(player.getUniqueId());
        AutoSnapshotHandler.clearSnapshot(player);
    }
}
