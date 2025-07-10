// ==================== MENU MANAGER ====================

package net.exylia.commons.ui.manager;

import lombok.Getter;
import net.exylia.commons.ui.actions.ActionSource;
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

/**
 * Central manager for all menu operations
 */
public class MenuManager implements Listener {

    private static MenuManager instance;
    @Getter
    private static JavaPlugin plugin;

    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
    @Getter
    private boolean initialized = false;

    private MenuManager() {}

    /**
     * Initializes the menu manager
     * @param javaPlugin The plugin instance
     */
    public static void initialize(JavaPlugin javaPlugin) {
        if (instance == null) {
            instance = new MenuManager();
            plugin = javaPlugin;

            Bukkit.getPluginManager().registerEvents(instance, plugin);
            instance.initialized = true;
        }
    }

    /**
     * Gets the menu manager instance
     * @return The instance
     */
    public static MenuManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MenuManager not initialized");
        }
        return instance;
    }

    /**
     * Registers an open menu for a player
     * @param player The player
     * @param menu The menu
     */
    public static void registerMenu(Player player, Menu menu) {
        getInstance().openMenus.put(player.getUniqueId(), menu);
    }

    /**
     * Gets the open menu for a player
     * @param player The player
     * @return The menu or null
     */
    public static Menu getOpenMenu(Player player) {
        return getInstance().openMenus.get(player.getUniqueId());
    }

    /**
     * Checks if a player has a menu open
     * @param player The player
     * @return True if a menu is open
     */
    public static boolean hasMenuOpen(Player player) {
        return getInstance().openMenus.containsKey(player.getUniqueId());
    }

    /**
     * Closes the menu for a player
     * @param player The player
     */
    public static void closeMenu(Player player) {
        Menu menu = getInstance().openMenus.get(player.getUniqueId());
        if (menu != null) {
            menu.close();
        }
    }

    // ==================== EVENT HANDLING ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        // Handle editable menus specially
        if (menu instanceof EditableMenu editableMenu) {
            handleEditableMenuClick(event, player, editableMenu);
            return;
        }

        // Handle regular menu clicks
        handleRegularMenuClick(event, player, menu);
    }

    private void handleRegularMenuClick(InventoryClickEvent event, Player player, Menu menu) {
        event.setCancelled(true);

        // Only handle clicks in the menu inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();
        MenuItem item = menu.getItem(slot);

        if (item != null) {
            MenuClickEvent clickEvent = new MenuClickEvent(
                    player, menu, item, slot, event.getClick()
            );

            // Handle the click through the menu
            menu.handleClick(clickEvent);
        }
    }

    private void handleEditableMenuClick(InventoryClickEvent event, Player player, EditableMenu menu) {
        int slot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // Handle clicks in the menu inventory
        if (clickedInventory == topInventory) {
            if (menu.isSlotEditable(slot)) {
                handleEditableSlotClick(event, menu, slot);
            } else {
                // Handle as regular menu click for non-editable slots
                event.setCancelled(true);
                MenuItem item = menu.getItem(slot);
                if (item != null) {
                    MenuClickEvent clickEvent = new MenuClickEvent(
                            player, menu, item, slot, event.getClick()
                    );
                    menu.handleClick(clickEvent);
                }
            }
        }
        // Handle shift-clicks from player inventory
        else if (clickedInventory == player.getInventory()) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                handleShiftClickToEditable(event, menu);
            }
        }
        // Cancel other clicks
        else {
            event.setCancelled(true);
        }
    }

    private void handleEditableSlotClick(InventoryClickEvent event, EditableMenu menu, int slot) {
        event.setCancelled(true);

        ClickType clickType = event.getClick();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        switch (clickType) {
            case LEFT -> handleLeftClickEditable(menu, slot, cursor, current, event);
            case RIGHT -> handleRightClickEditable(menu, slot, cursor, current, event);
            case SHIFT_LEFT, SHIFT_RIGHT -> handleShiftClickEditable(menu, slot, current, event.getWhoClicked());
            case MIDDLE -> menu.setEditableItem(slot, null);
            default -> { /* Do nothing for other click types */ }
        }
    }

    private void handleLeftClickEditable(EditableMenu menu, int slot, ItemStack cursor, ItemStack current, InventoryClickEvent event) {
        if (cursor != null && !cursor.getType().isAir()) {
            // Place cursor item in slot
            menu.setEditableItem(slot, cursor);
            event.getWhoClicked().setItemOnCursor(current != null && !current.getType().isAir() ? current : null);
        } else if (current != null && !current.getType().isAir()) {
            // Take item from slot to cursor
            menu.setEditableItem(slot, null);
            event.getWhoClicked().setItemOnCursor(current);
        }
    }

    private void handleRightClickEditable(EditableMenu menu, int slot, ItemStack cursor, ItemStack current, InventoryClickEvent event) {
        if (cursor != null && !cursor.getType().isAir()) {
            // Place one item from cursor
            ItemStack singleItem = cursor.clone();
            singleItem.setAmount(1);
            menu.setEditableItem(slot, singleItem);

            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
            } else {
                event.getWhoClicked().setItemOnCursor(null);
            }
        } else if (current != null && !current.getType().isAir()) {
            // Take half of the stack
            int halfAmount = (int) Math.ceil(current.getAmount() / 2.0);
            ItemStack toTake = current.clone();
            toTake.setAmount(halfAmount);

            if (current.getAmount() > halfAmount) {
                ItemStack remaining = current.clone();
                remaining.setAmount(current.getAmount() - halfAmount);
                menu.setEditableItem(slot, remaining);
            } else {
                menu.setEditableItem(slot, null);
            }

            event.getWhoClicked().setItemOnCursor(toTake);
        }
    }

    private void handleShiftClickEditable(EditableMenu menu, int slot, ItemStack current, org.bukkit.entity.HumanEntity player) {
        if (current != null && !current.getType().isAir()) {
            // Move item to player inventory
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(current);

            if (leftover.isEmpty()) {
                menu.setEditableItem(slot, null);
            } else {
                ItemStack remaining = leftover.values().iterator().next();
                menu.setEditableItem(slot, remaining);
            }
        }
    }

    private void handleShiftClickToEditable(InventoryClickEvent event, EditableMenu menu) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        // Find first available editable slot
        for (int slot : menu.getEditableSlots()) {
            if (menu.getEditableItem(slot) == null) {
                event.setCancelled(true);
                menu.setEditableItem(slot, clickedItem.clone());
                event.setCurrentItem(null);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        // Handle editable menu drags
        if (menu instanceof EditableMenu editableMenu) {
            handleEditableMenuDrag(event, editableMenu);
        } else {
            // Cancel drags in regular menus
            event.setCancelled(true);
        }
    }

    private void handleEditableMenuDrag(InventoryDragEvent event, EditableMenu menu) {
        Inventory topInventory = event.getView().getTopInventory();
        int topSize = topInventory.getSize();

        // Check if drag affects menu slots
        boolean affectsMenuSlots = event.getRawSlots().stream()
                .anyMatch(slot -> slot < topSize);

        if (affectsMenuSlots) {
            // Check if all affected menu slots are editable
            boolean allEditable = event.getRawSlots().stream()
                    .filter(slot -> slot < topSize)
                    .allMatch(menu::isSlotEditable);

            if (!allEditable) {
                event.setCancelled(true);
            } else {
                // Allow drag and sync state
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (int slot : event.getRawSlots()) {
                        if (slot < topSize && menu.isSlotEditable(slot)) {
                            ItemStack item = topInventory.getItem(slot);
                            if (item != null && !item.getType().isAir()) {
                                menu.setEditableItem(slot, item);
                            } else {
                                menu.setEditableItem(slot, null);
                            }
                        }
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Menu menu = openMenus.remove(player.getUniqueId());
        if (menu != null) {
            // Call the package-private method through reflection or make it public
            // For now, we'll add a public method to handle this
            menu.handleClose();
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Gets all open menus
     * @return Map of player UUID to menu
     */
    public Map<UUID, Menu> getOpenMenus() {
        return new ConcurrentHashMap<>(openMenus);
    }
}
