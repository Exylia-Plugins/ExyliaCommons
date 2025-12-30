package net.exylia.commons.v2.ui.event;

import net.exylia.commons.v2.ui.core.MenuManager;
import net.exylia.commons.v2.ui.menu.MenuBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Optional;

public class MenuClickHandler implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Optional<MenuBase> menuOptional = MenuManager.getInstance().getActiveMenu(player);
        if (menuOptional.isEmpty()) {
            return;
        }

        MenuBase menu = menuOptional.get();

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().equals(menu.getInventory())) {
            event.setCancelled(true);

            int slot = event.getSlot();
            menu.handleClick(slot, event.getClick());
        } else if (menu.getMenuData().isPlayerInventoryEnabled()) {
            handlePlayerInventoryClick(event, menu, player);
        } else {
            event.setCancelled(true);
        }
    }

    private void handlePlayerInventoryClick(InventoryClickEvent event, MenuBase menu, Player player) {
        int rawSlot = event.getRawSlot();

        if (rawSlot < menu.getInventory().getSize()) {
            return;
        }

        if (menu.getMenuData().getAllowedPlayerSlots().isEmpty()) {
            return;
        }

        int playerInvSlot = event.getSlot();

        boolean isAllowed = menu.getMenuData().getAllowedPlayerSlots().contains(playerInvSlot);

        if (!isAllowed) {
            event.setCancelled(true);
        }
    }
}
