// ==================== MENU CLICK EVENT ====================

package net.exylia.commons.ui.events;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.function.Consumer;

/**
 * Event representing a click on a menu item
 */
public class MenuClickEvent {

    private final Player player;
    private final Menu menu;
    private final MenuItem item;
    private final int slot;
    private final ClickType clickType;
    private boolean cancelled = false;

    public MenuClickEvent(Player player, Menu menu, MenuItem item, int slot, ClickType clickType) {
        this.player = player;
        this.menu = menu;
        this.item = item;
        this.slot = slot;
        this.clickType = clickType;
    }

    // ==================== GETTERS ====================

    public Player getPlayer() { return player; }
    public Menu getMenu() { return menu; }
    public MenuItem getItem() { return item; }
    public int getSlot() { return slot; }
    public ClickType getClickType() { return clickType; }

    // ==================== EVENT CONTROL ====================

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    // ==================== CONVENIENCE METHODS ====================

    public boolean isLeftClick() {
        return clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;
    }

    public boolean isRightClick() {
        return clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT;
    }

    public boolean isShiftClick() {
        return clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;
    }

    public boolean isMiddleClick() {
        return clickType == ClickType.MIDDLE;
    }

    /**
     * Updates the item at this slot
     * @param newItem The new item
     */
    public void updateItem(MenuItem newItem) {
        menu.setItem(slot, newItem);
    }

    /**
     * Closes the menu
     */
    public void closeMenu() {
        menu.close();
    }

    /**
     * Opens the parent menu if available
     */
    public void openParentMenu() {
        if (menu.getParentMenu() != null) {
            menu.getParentMenu().open(player, menu.getContext());
        } else {
            menu.close();
        }
    }
}