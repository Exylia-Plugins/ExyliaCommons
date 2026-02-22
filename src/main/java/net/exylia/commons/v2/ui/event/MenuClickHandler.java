package net.exylia.commons.v2.ui.event;

import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.ui.core.MenuManager;
import net.exylia.commons.v2.ui.menu.ItemInputMenu;
import net.exylia.commons.v2.ui.menu.MenuBase;
import net.exylia.commons.v2.ui.model.MenuState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

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

        if (menu.getInventory() == null || menu.getState().get() != MenuState.OPEN) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().equals(menu.getInventory())) {
            int slot = event.getSlot();

            if (menu instanceof ItemInputMenu inputMenu && inputMenu.isEditableSlot(slot)) {
                Tasks.later(inputMenu::updateEditableItems, 1L);
                return;
            }

            event.setCancelled(true);
            menu.handleClick(slot, event.getClick());
        } else if (menu instanceof ItemInputMenu inputMenu) {
            handleItemInputPlayerClick(event, inputMenu, player);
        } else if (isOwnInventoryInteractionAllowed(menu, event)) {
            return;
        } else if (menu.getMenuData().isPlayerInventoryEnabled()) {
            handlePlayerInventoryClick(event, menu, player);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Optional<MenuBase> menuOptional = MenuManager.getInstance().getActiveMenu(player);
        if (menuOptional.isEmpty()) {
            return;
        }

        MenuBase menu = menuOptional.get();
        if (menu.getInventory() == null || menu.getState().get() != MenuState.OPEN) {
            event.setCancelled(true);
            return;
        }
        int menuSize = menu.getInventory().getSize();

        boolean affectsTopInventory = event.getRawSlots().stream()
                .anyMatch(s -> s < menuSize);

        if (!affectsTopInventory) {
            return;
        }

        if (menu instanceof ItemInputMenu inputMenu) {
            boolean allEditable = event.getRawSlots().stream()
                    .filter(s -> s < menuSize)
                    .allMatch(inputMenu::isEditableSlot);

            if (allEditable) {
                Tasks.later(inputMenu::updateEditableItems, 1L);
                return;
            }
        }

        event.setCancelled(true);
    }

    private void handleItemInputPlayerClick(InventoryClickEvent event, ItemInputMenu menu, Player player) {
        InventoryAction action = event.getAction();

        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) {
                event.setCancelled(true);
                return;
            }

            int targetSlot = -1;
            for (int editableSlot : menu.getMenuData().getEditableSlots()) {
                ItemStack slotItem = menu.getInventory().getItem(editableSlot);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    targetSlot = editableSlot;
                    break;
                }
            }

            if (targetSlot >= 0) {
                event.setCancelled(true);
                menu.getInventory().setItem(targetSlot, item.clone());
                player.getInventory().setItem(event.getSlot(), null);
                menu.updateEditableItems();
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF ||
            action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME ||
            action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE ||
            action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR ||
            action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT) {
            return;
        }

        event.setCancelled(true);
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

    private boolean isOwnInventoryInteractionAllowed(MenuBase menu, InventoryClickEvent event) {
        if (menu.getMenuData().getType().isFullInventoryMenu()) {
            return false;
        }

        if (event.getRawSlot() < menu.getInventory().getSize()) {
            return false;
        }

        InventoryAction action = event.getAction();
        return action != InventoryAction.MOVE_TO_OTHER_INVENTORY &&
               action != InventoryAction.COLLECT_TO_CURSOR;
    }
}
