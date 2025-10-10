package net.exylia.commons.ui.builders;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.FullInventoryMenu;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class FullInventoryMenuBuilder {

    private final String title;
    private final int rows;
    private ExyliaContext context;

    private final Map<Integer, MenuItem> items = new HashMap<>();
    private final Map<Integer, MenuItem> playerItems = new HashMap<>();

    private MenuItem globalFiller;
    private MenuItem borderFiller;

    private boolean allowPlayerInventoryInteraction = false;
    private boolean allowHotbarSwap = false;
    private boolean allowDropItems = false;
    private boolean restoreInventoryOnClose = true;
    private boolean clearPlayerInventoryOnOpen = true;

    private int[] editablePlayerSlots;

    public FullInventoryMenuBuilder(String title, int rows) {
        this.title = title;
        this.rows = rows;
    }

    public static FullInventoryMenuBuilder create(String title, int rows) {
        return new FullInventoryMenuBuilder(title, rows);
    }

    public FullInventoryMenuBuilder withContext(ExyliaContext context) {
        this.context = context;
        return this;
    }

    public FullInventoryMenuBuilder setItem(int slot, MenuItem item) {
        this.items.put(slot, item);
        return this;
    }

    public FullInventoryMenuBuilder setPlayerItem(int playerSlot, MenuItem item) {
        this.playerItems.put(playerSlot, item);
        return this;
    }

    public FullInventoryMenuBuilder setGlobalFiller(MenuItem filler) {
        this.globalFiller = filler;
        return this;
    }

    public FullInventoryMenuBuilder setBorderFiller(MenuItem filler) {
        this.borderFiller = filler;
        return this;
    }

    public FullInventoryMenuBuilder allowPlayerInventoryInteraction(boolean allow) {
        this.allowPlayerInventoryInteraction = allow;
        return this;
    }

    public FullInventoryMenuBuilder allowHotbarSwap(boolean allow) {
        this.allowHotbarSwap = allow;
        return this;
    }

    public FullInventoryMenuBuilder allowDropItems(boolean allow) {
        this.allowDropItems = allow;
        return this;
    }

    public FullInventoryMenuBuilder restoreInventoryOnClose(boolean restore) {
        this.restoreInventoryOnClose = restore;
        return this;
    }

    public FullInventoryMenuBuilder clearPlayerInventoryOnOpen(boolean clear) {
        this.clearPlayerInventoryOnOpen = clear;
        return this;
    }

    public FullInventoryMenuBuilder setEditablePlayerSlots(int... slots) {
        this.editablePlayerSlots = slots.clone();
        return this;
    }

    public FullInventoryMenuBuilder enableFullPlayerInteraction() {
        this.allowPlayerInventoryInteraction = true;
        this.allowHotbarSwap = true;
        this.allowDropItems = true;
        return this;
    }

    public FullInventoryMenuBuilder disableAllPlayerInteraction() {
        this.allowPlayerInventoryInteraction = false;
        this.allowHotbarSwap = false;
        this.allowDropItems = false;
        return this;
    }

    public FullInventoryMenu build() {
        FullInventoryMenu menu = context != null
            ? new FullInventoryMenu(title, rows, context)
            : new FullInventoryMenu(title, rows);

        items.forEach(menu::setItem);
        playerItems.forEach(menu::setPlayerSlotItem);

        if (globalFiller != null) {
            menu.setGlobalFiller(globalFiller);
        }

        if (borderFiller != null) {
            menu.setBorderFiller(borderFiller);
        }

        menu.setAllowPlayerInventoryInteraction(allowPlayerInventoryInteraction);
        menu.setAllowHotbarSwap(allowHotbarSwap);
        menu.setAllowDropItems(allowDropItems);
        menu.setRestoreInventoryOnClose(restoreInventoryOnClose);
        menu.setClearPlayerInventoryOnOpen(clearPlayerInventoryOnOpen);

        if (editablePlayerSlots != null && editablePlayerSlots.length > 0) {
            menu.addEditablePlayerSlots(editablePlayerSlots);
        }

        return menu;
    }
}
