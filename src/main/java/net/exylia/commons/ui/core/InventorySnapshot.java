package net.exylia.commons.ui.core;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Objects;

@Getter
public class InventorySnapshot {

    private final ItemStack[] storageContents;
    private final ItemStack[] armorContents;
    private final ItemStack[] extraContents;
    private final ItemStack offHandItem;
    private final int heldItemSlot;
    private final long timestamp;

    private InventorySnapshot(ItemStack[] storageContents, ItemStack[] armorContents,
                             ItemStack[] extraContents, ItemStack offHandItem,
                             int heldItemSlot) {
        this.storageContents = cloneArray(storageContents);
        this.armorContents = cloneArray(armorContents);
        this.extraContents = cloneArray(extraContents);
        this.offHandItem = offHandItem != null ? offHandItem.clone() : null;
        this.heldItemSlot = heldItemSlot;
        this.timestamp = System.currentTimeMillis();
    }

    public static InventorySnapshot capture(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        PlayerInventory inventory = player.getInventory();
        return new InventorySnapshot(
            inventory.getStorageContents(),
            inventory.getArmorContents(),
            inventory.getExtraContents(),
            inventory.getItemInOffHand(),
            inventory.getHeldItemSlot()
        );
    }

    public void restore(Player player) {
        restore(player, true);
    }

    public void restore(Player player, boolean clearFirst) {
        Objects.requireNonNull(player, "Player cannot be null");

        PlayerInventory inventory = player.getInventory();

        if (clearFirst) {
            inventory.clear();
        }

        inventory.setStorageContents(cloneArray(storageContents));
        inventory.setArmorContents(cloneArray(armorContents));
        inventory.setExtraContents(cloneArray(extraContents));
        inventory.setItemInOffHand(offHandItem != null ? offHandItem.clone() : null);
        inventory.setHeldItemSlot(heldItemSlot);

        player.updateInventory();
    }

    public boolean hasExpired(long maxAgeMillis) {
        return (System.currentTimeMillis() - timestamp) > maxAgeMillis;
    }

    public int getTotalItems() {
        int count = 0;

        count += countNonNullItems(storageContents);
        count += countNonNullItems(armorContents);
        count += countNonNullItems(extraContents);
        if (offHandItem != null) count++;

        return count;
    }

    private static ItemStack[] cloneArray(ItemStack[] array) {
        if (array == null) return new ItemStack[0];

        ItemStack[] cloned = new ItemStack[array.length];
        for (int i = 0; i < array.length; i++) {
            cloned[i] = array[i] != null ? array[i].clone() : null;
        }
        return cloned;
    }

    private int countNonNullItems(ItemStack[] array) {
        if (array == null) return 0;
        return (int) Arrays.stream(array)
            .filter(Objects::nonNull)
            .filter(item -> !item.getType().isAir())
            .count();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventorySnapshot that)) return false;

        return heldItemSlot == that.heldItemSlot &&
               Arrays.equals(storageContents, that.storageContents) &&
               Arrays.equals(armorContents, that.armorContents) &&
               Arrays.equals(extraContents, that.extraContents) &&
               Objects.equals(offHandItem, that.offHandItem);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(offHandItem, heldItemSlot);
        result = 31 * result + Arrays.hashCode(storageContents);
        result = 31 * result + Arrays.hashCode(armorContents);
        result = 31 * result + Arrays.hashCode(extraContents);
        return result;
    }
}
