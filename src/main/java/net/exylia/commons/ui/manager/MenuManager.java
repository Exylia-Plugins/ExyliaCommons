package net.exylia.commons.ui.manager;

import lombok.Getter;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.EditableMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager implements Listener {

    private static MenuManager instance;
    @Getter
    private static JavaPlugin plugin;

    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
    @Getter
    private boolean initialized = false;

    private MenuManager() {}

    public static void initialize(JavaPlugin javaPlugin) {
        if (instance == null) {
            instance = new MenuManager();
            plugin = javaPlugin;

            Bukkit.getPluginManager().registerEvents(instance, plugin);
            instance.initialized = true;
        }
    }

    public static MenuManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MenuManager not initialized");
        }
        return instance;
    }

    public static void registerMenu(Player player, Menu menu) {
        getInstance().openMenus.put(player.getUniqueId(), menu);
    }

    public static Menu getOpenMenu(Player player) {
        return getInstance().openMenus.get(player.getUniqueId());
    }

    public static boolean hasMenuOpen(Player player) {
        return getInstance().openMenus.containsKey(player.getUniqueId());
    }

    public static void closeMenu(Player player) {
        Menu menu = getInstance().openMenus.get(player.getUniqueId());
        if (menu != null) {
            menu.close();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        if (menu instanceof EditableMenu editableMenu) {
            handleEditableMenuClick(event, player, editableMenu);
            return;
        }

        handleRegularMenuClick(event, player, menu);
    }

    private void handleRegularMenuClick(InventoryClickEvent event, Player player, Menu menu) {
        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();
        MenuItem item = menu.getItem(slot);

        if (item != null) {
            MenuClickEvent clickEvent = new MenuClickEvent(
                    player, menu, item, slot, event.getClick()
            );

            menu.handleClick(clickEvent);
        }
    }

    private void handleEditableMenuClick(InventoryClickEvent event, Player player, EditableMenu menu) {
        int slot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory == topInventory) {
            if (menu.isSlotEditable(slot)) {
                handleEditableSlotClick(event, menu, slot);
            } else {
                event.setCancelled(true);
                MenuItem item = menu.getItem(slot);
                if (item != null) {
                    MenuClickEvent clickEvent = new MenuClickEvent(
                            player, menu, item, slot, event.getClick()
                    );
                    menu.handleClick(clickEvent);
                }
            }
        } else if (clickedInventory == player.getInventory()) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                    handleShiftClickToEditable(event, menu);
                }
            }
        }
    }

    private void handleEditableSlotClick(InventoryClickEvent event, EditableMenu menu, int slot) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            syncEditableSlotState(menu, slot);
        }, 1L);
    }

    private void syncEditableSlotState(EditableMenu menu, int slot) {
        if (menu.getInventory() == null) return;

        ItemStack currentItem = menu.getInventory().getItem(slot);

        if (currentItem == null || currentItem.getType().isAir()) {
            menu.setEditableItem(slot, null);
        } else {
            menu.setEditableItem(slot, currentItem.clone());
        }
    }

    private void handleShiftClickToEditable(InventoryClickEvent event, EditableMenu menu) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        for (int slot : menu.getEditableSlots()) {
            ItemStack currentItem = menu.getInventory().getItem(slot);

            if (currentItem == null || currentItem.getType().isAir()) {
                event.setCancelled(true);

                ItemStack toPlace = clickedItem.clone();
                menu.getInventory().setItem(slot, toPlace);
                menu.setEditableItem(slot, toPlace);

                event.setCurrentItem(null);
                return;
            }

            if (currentItem.isSimilar(clickedItem)) {
                int maxStack = currentItem.getMaxStackSize();
                int currentAmount = currentItem.getAmount();
                int clickedAmount = clickedItem.getAmount();
                int spaceAvailable = maxStack - currentAmount;

                if (spaceAvailable > 0) {
                    event.setCancelled(true);

                    int toTransfer = Math.min(spaceAvailable, clickedAmount);

                    ItemStack newItem = currentItem.clone();
                    newItem.setAmount(currentAmount + toTransfer);
                    menu.getInventory().setItem(slot, newItem);
                    menu.setEditableItem(slot, newItem);

                    if (toTransfer >= clickedAmount) {
                        event.setCurrentItem(null);
                    } else {
                        ItemStack remaining = clickedItem.clone();
                        remaining.setAmount(clickedAmount - toTransfer);
                        event.setCurrentItem(remaining);
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        if (menu instanceof EditableMenu editableMenu) {
            handleEditableMenuDrag(event, editableMenu);
        } else {
            event.setCancelled(true);
        }
    }

    private void handleEditableMenuDrag(InventoryDragEvent event, EditableMenu menu) {
        Inventory topInventory = event.getView().getTopInventory();
        int topSize = topInventory.getSize();

        boolean affectsMenuSlots = event.getRawSlots().stream()
                .anyMatch(slot -> slot < topSize);

        if (!affectsMenuSlots) {
            return;
        }

        boolean affectsNonEditableSlots = event.getRawSlots().stream()
                .filter(slot -> slot < topSize)
                .anyMatch(slot -> !menu.isSlotEditable(slot));

        if (affectsNonEditableSlots) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int slot : event.getRawSlots()) {
                if (slot < topSize && menu.isSlotEditable(slot)) {
                    syncEditableSlotState(menu, slot);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu != null) {
            menu.handleClose();
            openMenus.remove(player.getUniqueId());
        }
    }

    public Map<UUID, Menu> getOpenMenus() {
        return new ConcurrentHashMap<>(openMenus);
    }
}