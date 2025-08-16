package net.exylia.commons.ui.menus;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EditableMenu extends Menu {

    private final Set<Integer> editableSlots = new HashSet<>();
    private final Map<Integer, ItemStack> editableItems = new HashMap<>();

    private BiConsumer<Integer, ItemStack> onItemPlaced;
    private BiConsumer<Integer, ItemStack> onItemRemoved;
    private Consumer<Map<Integer, ItemStack>> onItemsChanged;

    public EditableMenu(String title, int rows, ExyliaContext context) {
        super(title, rows, context);
    }

    public EditableMenu addEditableSlot(int slot) {
        if (isValidSlot(slot)) {
            editableSlots.add(slot);
        }
        return this;
    }

    public EditableMenu addEditableSlots(int... slots) {
        for (int slot : slots) {
            addEditableSlot(slot);
        }
        return this;
    }

    public EditableMenu addEditableSlotRange(int start, int end) {
        for (int i = start; i <= end && i < size; i++) {
            addEditableSlot(i);
        }
        return this;
    }

    public EditableMenu removeEditableSlot(int slot) {
        editableSlots.remove(slot);
        editableItems.remove(slot);
        return this;
    }

    public EditableMenu clearEditableSlots() {
        editableSlots.clear();
        editableItems.clear();
        return this;
    }

    public boolean isSlotEditable(int slot) {
        return editableSlots.contains(slot);
    }

    public boolean setEditableItem(int slot, ItemStack item) {
        if (!editableSlots.contains(slot)) {
            return false;
        }

        ItemStack oldItem = editableItems.get(slot);

        if (item == null || item.getType().isAir()) {
            editableItems.remove(slot);

            if (onItemRemoved != null && oldItem != null) {
                onItemRemoved.accept(slot, oldItem);
            }
        } else {
            editableItems.put(slot, item.clone());

            if (onItemPlaced != null) {
                onItemPlaced.accept(slot, item.clone());
            }
        }

        if (onItemsChanged != null) {
            onItemsChanged.accept(new HashMap<>(editableItems));
        }

        return true;
    }

    public ItemStack getEditableItem(int slot) {
        ItemStack item = editableItems.get(slot);
        return item != null ? item.clone() : null;
    }

    public Map<Integer, ItemStack> getEditableItems() {
        Map<Integer, ItemStack> result = new HashMap<>();
        editableItems.forEach((slot, item) -> result.put(slot, item.clone()));
        return result;
    }

    public EditableMenu setEditableItems(Map<Integer, ItemStack> items) {
        for (int slot : editableSlots) {
            setEditableItem(slot, null);
        }

        items.forEach(this::setEditableItem);
        return this;
    }

    public EditableMenu clearEditableItems() {
        for (int slot : new HashSet<>(editableItems.keySet())) {
            setEditableItem(slot, null);
        }
        return this;
    }

    public EditableMenu setOnItemPlaced(BiConsumer<Integer, ItemStack> handler) {
        this.onItemPlaced = handler;
        return this;
    }

    public EditableMenu setOnItemRemoved(BiConsumer<Integer, ItemStack> handler) {
        this.onItemRemoved = handler;
        return this;
    }

    public EditableMenu setOnItemsChanged(Consumer<Map<Integer, ItemStack>> handler) {
        this.onItemsChanged = handler;
        return this;
    }

    @Override
    protected MenuItem getEffectiveItem(int slot) {
        if (editableSlots.contains(slot)) {
            ItemStack editableItem = editableItems.get(slot);
            if (editableItem != null) {
                return new MenuItem(editableItem.clone());
            }
            return null;
        }

        return super.getEffectiveItem(slot);
    }

    @Override
    protected void applyFillers() {
        if (globalFiller != null) {
            for (int i = 0; i < size; i++) {
                if (!editableSlots.contains(i) && !items.containsKey(i)) {
                    items.put(i, globalFiller.clone());
                }
            }
        }

        if (borderFiller != null) {
            for (int i = 0; i < size; i++) {
                if (isBorderSlot(i) && !editableSlots.contains(i) && !items.containsKey(i)) {
                    items.put(i, borderFiller.clone());
                }
            }
        }
    }

    @Override
    protected void populateInventory() {
        if (inventory == null) return;

        for (int i = 0; i < size; i++) {
            updateSlot(i);
        }
    }

    @Override
    protected void updateSlot(int slot) {
        if (inventory == null || !isValidSlot(slot)) return;

        if (editableSlots.contains(slot)) {
            ItemStack editableItem = editableItems.get(slot);
            inventory.setItem(slot, editableItem);
        } else {
            MenuItem item = getEffectiveItem(slot);
            if (item != null) {
                ExyliaContext itemContext = prepareItemContext(item);
                item.withContext(itemContext);
                inventory.setItem(slot, item.buildProcessed(viewer));
            } else {
                inventory.setItem(slot, null);
            }
        }
    }

    public ItemStack[] toPlayerInventoryArray() {
        ItemStack[] result = new ItemStack[36];

        for (Map.Entry<Integer, ItemStack> entry : editableItems.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < 36) {
                result[slot] = entry.getValue().clone();
            }
        }

        return result;
    }

    public EditableMenu loadFromPlayerInventoryArray(ItemStack[] items) {
        clearEditableItems();

        for (int i = 0; i < items.length && i < 36; i++) {
            ItemStack item = items[i];
            if (item != null && !item.getType().isAir() && editableSlots.contains(i)) {
                setEditableItem(i, item);
            }
        }

        return this;
    }

    public Set<Integer> getEditableSlots() {
        return new HashSet<>(editableSlots);
    }
}