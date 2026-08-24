package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ItemInputMenu extends MenuBase {

    private final Consumer<Map<Integer, ItemStack>> onCloseCallback;
    private final Map<Integer, ItemStack> pendingEditableItems = new ConcurrentHashMap<>();

    public ItemInputMenu(Player player, MenuData menuData) {
        this(player, menuData, null);
    }

    public ItemInputMenu(Player player, MenuData menuData, Consumer<Map<Integer, ItemStack>> onCloseCallback) {
        super(player, menuData);
        this.onCloseCallback = onCloseCallback;
    }

    public boolean isEditableSlot(int slot) {
        return menuData.getEditableSlots().contains(slot);
    }

    public Map<Integer, ItemStack> getEditableItems() {
        Map<Integer, ItemStack> result = new LinkedHashMap<>();
        if (inventory == null) return result;
        for (int slot : menuData.getEditableSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                result.put(slot, item.clone());
            }
        }
        return result;
    }

    public void setEditableItem(int slot, ItemStack itemStack) {
        if (!isEditableSlot(slot)) return;

        ItemStack stack = itemStack == null || itemStack.getType() == Material.AIR
                ? null
                : itemStack.clone();

        if (stack == null) {
            pendingEditableItems.remove(slot);
        } else {
            pendingEditableItems.put(slot, stack.clone());
        }

        runOnPlayerThread(() -> {
            if (inventory == null || slot < 0 || slot >= inventory.getSize()) return;
            inventory.setItem(slot, stack);
        });
    }

    public void setEditableItems(Map<Integer, ItemStack> items) {
        if (items == null || items.isEmpty()) return;
        items.forEach(this::setEditableItem);
    }

    public void updateEditableItems() {
    }

    @Override
    protected Inventory createInventory() {
        String processedTitle = processTitle(menuData.getTitle());
        return Bukkit.createInventory(null, menuData.getSize(), ColorAPI.parse(processedTitle));
    }

    @Override
    protected void populateItems() {
        applyFillersExcludingEditable();
        applyStaticItems();
        if (!pendingEditableItems.isEmpty()) {
            runOnPlayerThread(() -> pendingEditableItems.forEach((slot, item) -> {
                if (inventory != null && slot >= 0 && slot < menuData.getSize() && isEditableSlot(slot)) {
                    inventory.setItem(slot, item.clone());
                }
            }));
        }
    }

    private void applyFillersExcludingEditable() {
        java.util.Set<Integer> occupied = new java.util.HashSet<>(itemsBySlot.keySet());
        occupied.addAll(menuData.getEditableSlots());

        for (int slot = 0; slot < menuData.getSize(); slot++) {
            if (occupied.contains(slot)) continue;
            net.exylia.commons.v2.items.model.ItemData filler = getFillerForSlot(slot);
            if (filler != null) setItem(slot, filler);
        }

        applyCustomFillersExcludingEditable();
    }

    private void applyCustomFillersExcludingEditable() {
        if (!menuData.hasCustomFillers()) return;
        for (net.exylia.commons.v2.ui.model.FillerData fillerData : menuData.getCustomFillers()) {
            if (fillerData.getItemData() == null || fillerData.getSlots().isEmpty()) continue;
            for (Integer slot : fillerData.getSlots()) {
                if (slot >= 0 && slot < menuData.getSize() && !isEditableSlot(slot)) {
                    setItem(slot, fillerData.getItemData());
                }
            }
        }
    }

    @Override
    protected void handleClickInternal(int slot, ClickType clickType) {
    }

    @Override
    protected void cleanup() {
        pendingEditableItems.clear();
        if (onCloseCallback != null) {
            Map<Integer, ItemStack> collected = getEditableItems();
            onCloseCallback.accept(collected);
        }
        super.cleanup();
    }
}
