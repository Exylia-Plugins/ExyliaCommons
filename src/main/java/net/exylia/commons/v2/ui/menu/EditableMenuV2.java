package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.ui.model.*;
import net.exylia.commons.v2.ui.refresh.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Getter
@Setter
public class EditableMenuV2 extends MenuV2 {
    private int[] editableSlots;
    private final Map<Integer, ItemStack> editableItems = new ConcurrentHashMap<>();
    private MenuItemV2 editableSlotFiller;

    private BiConsumer<Player, ItemStack> onItemPlaced;
    private BiConsumer<Player, ItemStack> onItemRemoved;

    private boolean allowItemRemoval = true;
    private boolean allowItemPlacement = true;

    public EditableMenuV2(String title, int rows, int[] editableSlots) {
        super(UUID.randomUUID().toString(), MenuType.EDITABLE);
        this.rawTitle = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
        this.editableSlots = editableSlots != null ? editableSlots.clone() : new int[0];
        initializeRefreshStrategy();
    }

    private void initializeRefreshStrategy() {
        this.refreshStrategy = switch (refreshMode) {
            case DISABLED -> new DisabledRefreshStrategy();
            case SLOT_ONLY -> new SlotOnlyRefreshStrategy();
            case SMART -> new SmartRefreshStrategy();
            case FULL -> new FullRefreshStrategy();
        };
    }

    @Override
    public void open(Player player, MenuContext context) {
        this.viewer = player;
        this.context = context != null ? context : MenuContext.create(player);

        enrichContext();
        processTitle();
        createInventory();
        applyFillers();
        populateInventory();

        player.openInventory(inventory);
        this.state = MenuState.OPEN;

        handleOpen();

        if (dynamicUpdates) {
            startDynamicUpdates();
        }
    }

    @Override
    protected void enrichContext() {
        super.enrichContext();

        context.getPlaceholderContext()
            .put("editable_slots_count", editableSlots.length)
            .put("editable_items_count", editableItems.size())
            .put("editable_empty_slots", editableSlots.length - editableItems.size());
    }

    @Override
    public void populateInventory() {
        if (inventory == null) {
            return;
        }

        items.forEach((slot, item) -> {
            if (!isSlotEditable(slot)) {
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
            }
        });

        populateEditableSlots();
    }

    private void populateEditableSlots() {
        for (int slot : editableSlots) {
            if (slot < 0 || slot >= size) {
                continue;
            }

            if (editableItems.containsKey(slot)) {
                inventory.setItem(slot, editableItems.get(slot));
            } else if (editableSlotFiller != null) {
                inventory.setItem(slot, editableSlotFiller.build(viewer, context.getPlaceholderContext()));
            }
        }
    }

    public boolean isSlotEditable(int slot) {
        for (int editableSlot : editableSlots) {
            if (editableSlot == slot) {
                return true;
            }
        }
        return false;
    }

    public void handleItemPlaced(Player player, int slot, ItemStack item) {
        if (!isSlotEditable(slot) || !allowItemPlacement) {
            return;
        }

        if (item != null && item.getType().isAir()) {
            item = null;
        }

        if (item != null) {
            editableItems.put(slot, item.clone());

            if (onItemPlaced != null) {
                onItemPlaced.accept(player, item);
            }
        } else {
            handleItemRemoved(player, slot);
        }
    }

    public void handleItemRemoved(Player player, int slot) {
        if (!isSlotEditable(slot) || !allowItemRemoval) {
            return;
        }

        ItemStack removed = editableItems.remove(slot);

        if (removed != null && onItemRemoved != null) {
            onItemRemoved.accept(player, removed);
        }

        if (editableSlotFiller != null && state == MenuState.OPEN) {
            inventory.setItem(slot, editableSlotFiller.build(viewer, context.getPlaceholderContext()));
        }
    }

    public void setEditableItems(Map<Integer, ItemStack> items) {
        editableItems.clear();
        if (items != null) {
            items.forEach((slot, item) -> {
                if (isSlotEditable(slot) && item != null) {
                    editableItems.put(slot, item.clone());
                }
            });
        }

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public Map<Integer, ItemStack> getEditableSnapshot() {
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        editableItems.forEach((slot, item) -> snapshot.put(slot, item.clone()));
        return snapshot;
    }

    public void clearEditableItems() {
        editableItems.clear();

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public ItemStack[] getEditableItemsArray() {
        ItemStack[] array = new ItemStack[editableSlots.length];

        for (int i = 0; i < editableSlots.length; i++) {
            int slot = editableSlots[i];
            array[i] = editableItems.getOrDefault(slot, null);
        }

        return array;
    }

    public void setEditableItemsArray(ItemStack[] items) {
        editableItems.clear();

        if (items != null) {
            for (int i = 0; i < Math.min(items.length, editableSlots.length); i++) {
                if (items[i] != null && !items[i].getType().isAir()) {
                    editableItems.put(editableSlots[i], items[i].clone());
                }
            }
        }

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public void syncEditableItems() {
        if (inventory == null || state != MenuState.OPEN) {
            return;
        }

        for (int slot : editableSlots) {
            if (slot < 0 || slot >= size) {
                continue;
            }

            ItemStack current = inventory.getItem(slot);

            if (current == null || current.getType().isAir()) {
                editableItems.remove(slot);
            } else {
                editableItems.put(slot, current.clone());
            }
        }
    }

    public EditableMenuV2 setEditableSlotFiller(MenuItemV2 filler) {
        this.editableSlotFiller = filler;
        return this;
    }

    public EditableMenuV2 setOnItemPlaced(BiConsumer<Player, ItemStack> callback) {
        this.onItemPlaced = callback;
        return this;
    }

    public EditableMenuV2 setOnItemRemoved(BiConsumer<Player, ItemStack> callback) {
        this.onItemRemoved = callback;
        return this;
    }

    public EditableMenuV2 setAllowItemRemoval(boolean allow) {
        this.allowItemRemoval = allow;
        return this;
    }

    public EditableMenuV2 setAllowItemPlacement(boolean allow) {
        this.allowItemPlacement = allow;
        return this;
    }
}
