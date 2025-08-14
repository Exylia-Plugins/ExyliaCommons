
// ==================== EDITABLE MENU ====================

package net.exylia.commons.ui.menus;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Menu that allows players to edit items by dragging from their inventory
 */
public class EditableMenu extends Menu {

    private final Set<Integer> editableSlots = new HashSet<>();
    private final Map<Integer, ItemStack> editableItems = new HashMap<>();

    // Event handlers
    private BiConsumer<Integer, ItemStack> onItemPlaced;
    private BiConsumer<Integer, ItemStack> onItemRemoved;
    private Consumer<Map<Integer, ItemStack>> onItemsChanged;

    public EditableMenu(String title, int rows, ExyliaContext context) {
        super(title, rows, context);
    }

    // ==================== EDITABLE SLOT MANAGEMENT ====================

    /**
     * Adds an editable slot
     * @param slot The slot to make editable
     * @return This menu for chaining
     */
    public EditableMenu addEditableSlot(int slot) {
        if (isValidSlot(slot)) {
            editableSlots.add(slot);
        }
        return this;
    }

    /**
     * Adds multiple editable slots
     * @param slots The slots to make editable
     * @return This menu for chaining
     */
    public EditableMenu addEditableSlots(int... slots) {
        for (int slot : slots) {
            addEditableSlot(slot);
        }
        return this;
    }

    /**
     * Adds a range of editable slots
     * @param start The start slot (inclusive)
     * @param end The end slot (inclusive)
     * @return This menu for chaining
     */
    public EditableMenu addEditableSlotRange(int start, int end) {
        for (int i = start; i <= end && i < size; i++) {
            addEditableSlot(i);
        }
        return this;
    }

    /**
     * Removes an editable slot
     * @param slot The slot to remove
     * @return This menu for chaining
     */
    public EditableMenu removeEditableSlot(int slot) {
        editableSlots.remove(slot);
        editableItems.remove(slot);
        return this;
    }

    /**
     * Clears all editable slots
     * @return This menu for chaining
     */
    public EditableMenu clearEditableSlots() {
        editableSlots.clear();
        editableItems.clear();
        return this;
    }

    /**
     * Checks if a slot is editable
     * @param slot The slot to check
     * @return True if the slot is editable
     */
    public boolean isSlotEditable(int slot) {
        return editableSlots.contains(slot);
    }

    // ==================== EDITABLE ITEM MANAGEMENT ====================

    /**
     * Sets an item in an editable slot
     * @param slot The slot
     * @param item The item (null to remove)
     * @return True if successful
     */
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

        // Update inventory display
        if (inventory != null && isOpen()) {
            updateSlot(slot);
        }

        // Notify general changes
        if (onItemsChanged != null) {
            onItemsChanged.accept(new HashMap<>(editableItems));
        }

        return true;
    }

    /**
     * Gets an editable item
     * @param slot The slot
     * @return The item or null
     */
    public ItemStack getEditableItem(int slot) {
        ItemStack item = editableItems.get(slot);
        return item != null ? item.clone() : null;
    }

    /**
     * Gets all editable items
     * @return Map of slot to item
     */
    public Map<Integer, ItemStack> getEditableItems() {
        Map<Integer, ItemStack> result = new HashMap<>();
        editableItems.forEach((slot, item) -> result.put(slot, item.clone()));
        return result;
    }

    /**
     * Sets multiple editable items
     * @param items Map of slot to item
     * @return This menu for chaining
     */
    public EditableMenu setEditableItems(Map<Integer, ItemStack> items) {
        // Clear current items
        for (int slot : editableSlots) {
            setEditableItem(slot, null);
        }

        // Set new items
        items.forEach(this::setEditableItem);
        return this;
    }

    /**
     * Clears all editable items
     * @return This menu for chaining
     */
    public EditableMenu clearEditableItems() {
        for (int slot : new HashSet<>(editableItems.keySet())) {
            setEditableItem(slot, null);
        }
        return this;
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Sets the item placed handler
     * @param handler The handler
     * @return This menu for chaining
     */
    public EditableMenu setOnItemPlaced(BiConsumer<Integer, ItemStack> handler) {
        this.onItemPlaced = handler;
        return this;
    }

    /**
     * Sets the item removed handler
     * @param handler The handler
     * @return This menu for chaining
     */
    public EditableMenu setOnItemRemoved(BiConsumer<Integer, ItemStack> handler) {
        this.onItemRemoved = handler;
        return this;
    }

    /**
     * Sets the items changed handler
     * @param handler The handler
     * @return This menu for chaining
     */
    public EditableMenu setOnItemsChanged(Consumer<Map<Integer, ItemStack>> handler) {
        this.onItemsChanged = handler;
        return this;
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    protected MenuItem getEffectiveItem(int slot) {
        // Check if this is an editable slot with an item
        if (editableSlots.contains(slot)) {
            ItemStack editableItem = editableItems.get(slot);
            if (editableItem != null) {
                // Create a MenuItem from the ItemStack
                return new MenuItem(editableItem.clone());
            }
            // Return null for empty editable slots (no filler)
            return null;
        }

        // Use parent logic for non-editable slots
        return super.getEffectiveItem(slot);
    }

    @Override
    protected void applyFillers() {
        // Apply global filler only to non-editable slots
        if (globalFiller != null) {
            for (int i = 0; i < size; i++) {
                if (!editableSlots.contains(i) && !items.containsKey(i)) {
                    items.put(i, globalFiller.clone());
                }
            }
        }

        // Apply border filler excluding editable slots
        if (borderFiller != null) {
            for (int i = 0; i < size; i++) {
                if (isBorderSlot(i) && !editableSlots.contains(i) && !items.containsKey(i)) {
                    items.put(i, borderFiller.clone());
                }
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Converts to player inventory array format
     * @return Array suitable for player inventory
     */
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

    /**
     * Loads from player inventory array format
     * @param items The item array
     * @return This menu for chaining
     */
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