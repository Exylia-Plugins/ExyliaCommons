package net.exylia.commons.ui.builders;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.EditableMenu;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EditableMenuBuilder {

    private final EditableMenu menu;

    public EditableMenuBuilder(String title, int rows) {
        this.menu = new EditableMenu(title, rows, null);
    }

    public EditableMenuBuilder(String title, int rows, ExyliaContext context) {
        this.menu = new EditableMenu(title, rows, context);
    }

    public EditableMenuBuilder addEditableSlot(int slot) {
        menu.addEditableSlot(slot);
        return this;
    }

    public EditableMenuBuilder addEditableSlots(int... slots) {
        menu.addEditableSlots(slots);
        return this;
    }

    public EditableMenuBuilder addEditableSlots(Collection<Integer> slots) {
        slots.forEach(menu::addEditableSlot);
        return this;
    }

    public EditableMenuBuilder addEditableSlotRange(int start, int end) {
        menu.addEditableSlotRange(start, end);
        return this;
    }

    public EditableMenuBuilder editableSlotFiller(MenuItem filler) {
        for (int slot : menu.getEditableSlots()) {
            menu.setItem(slot, filler);
        }
        return this;
    }

    public EditableMenuBuilder setEditableItems(Map<Integer, ItemStack> items) {
        menu.setEditableItems(items);
        return this;
    }

    public EditableMenuBuilder onItemPlaced(BiConsumer<Integer, ItemStack> handler) {
        menu.setOnItemPlaced(handler);
        return this;
    }

    public EditableMenuBuilder onItemRemoved(BiConsumer<Integer, ItemStack> handler) {
        menu.setOnItemRemoved(handler);
        return this;
    }

    public EditableMenuBuilder onItemsChanged(Consumer<Map<Integer, ItemStack>> handler) {
        menu.setOnItemsChanged(handler);
        return this;
    }

    public EditableMenuBuilder globalFiller(MenuItem filler) {
        menu.setGlobalFiller(filler);
        return this;
    }

    public EditableMenuBuilder borderFiller(MenuItem filler) {
        menu.setBorderFiller(filler);
        return this;
    }

    public EditableMenuBuilder onClose(Consumer<org.bukkit.entity.Player> handler) {
        menu.setCloseHandler(handler);
        return this;
    }

    public EditableMenuBuilder dynamicUpdates(org.bukkit.plugin.java.JavaPlugin plugin, long interval) {
        menu.enableDynamicUpdates(plugin, interval);
        return this;
    }

    public EditableMenu build() {
        return menu;
    }
}
