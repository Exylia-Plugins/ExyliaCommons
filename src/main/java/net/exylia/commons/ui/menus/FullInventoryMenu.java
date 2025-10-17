package net.exylia.commons.ui.menus;

import lombok.Getter;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.InventorySnapshot;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FullInventoryMenu extends Menu {

    private static final Map<UUID, InventorySnapshot> playerSnapshots = new ConcurrentHashMap<>();
    private static final int PLAYER_INVENTORY_SIZE = 36;
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 8;
    private static final int MAIN_INVENTORY_START = 9;
    private static final int MAIN_INVENTORY_END = 35;

    @Getter
    private final Set<Integer> editablePlayerSlots = new HashSet<>();
    private final Map<Integer, MenuItem> playerInventoryItems = new ConcurrentHashMap<>();
    private final Map<Integer, ItemStack> editableItems = new HashMap<>();

    @Getter
    private boolean allowPlayerInventoryInteraction = false;
    @Getter
    private boolean allowHotbarSwap = false;
    @Getter
    private boolean allowDropItems = false;
    @Getter
    private boolean restoreInventoryOnClose = true;
    @Getter
    private boolean clearPlayerInventoryOnOpen = true;

    public FullInventoryMenu(String title, int rows) {
        super(title, rows);
    }

    public FullInventoryMenu(String title, int rows, ExyliaContext context) {
        super(title, rows, context);
    }

    public FullInventoryMenu setAllowPlayerInventoryInteraction(boolean allow) {
        this.allowPlayerInventoryInteraction = allow;
        return this;
    }

    public FullInventoryMenu setAllowHotbarSwap(boolean allow) {
        this.allowHotbarSwap = allow;
        return this;
    }

    public FullInventoryMenu setAllowDropItems(boolean allow) {
        this.allowDropItems = allow;
        return this;
    }

    public FullInventoryMenu setRestoreInventoryOnClose(boolean restore) {
        this.restoreInventoryOnClose = restore;
        return this;
    }

    public FullInventoryMenu setClearPlayerInventoryOnOpen(boolean clear) {
        this.clearPlayerInventoryOnOpen = clear;
        return this;
    }

    public FullInventoryMenu setPlayerSlotItem(int playerSlot, MenuItem item) {
        if (isValidPlayerSlot(playerSlot)) {
            playerInventoryItems.put(playerSlot, item);

            if (isOpen() && viewer != null) {
                updatePlayerSlot(playerSlot);
            }
        }
        return this;
    }

    public FullInventoryMenu removePlayerSlotItem(int playerSlot) {
        playerInventoryItems.remove(playerSlot);
        if (isOpen() && viewer != null) {
            updatePlayerSlot(playerSlot);
        }
        return this;
    }

    public FullInventoryMenu clearPlayerSlotItems() {
        playerInventoryItems.clear();
        if (isOpen() && viewer != null) {
            populatePlayerInventory();
        }
        return this;
    }

    public FullInventoryMenu addEditablePlayerSlot(int slot) {
        if (isValidPlayerSlot(slot)) {
            editablePlayerSlots.add(slot);
        }
        return this;
    }

    public FullInventoryMenu addEditablePlayerSlots(int... slots) {
        for (int slot : slots) {
            addEditablePlayerSlot(slot);
        }
        return this;
    }

    public FullInventoryMenu addEditablePlayerSlotRange(int start, int end) {
        for (int i = start; i <= end && i < PLAYER_INVENTORY_SIZE; i++) {
            addEditablePlayerSlot(i);
        }
        return this;
    }

    public FullInventoryMenu removeEditablePlayerSlot(int slot) {
        editablePlayerSlots.remove(slot);
        editableItems.remove(slot);
        return this;
    }

    public FullInventoryMenu clearEditablePlayerSlots() {
        editablePlayerSlots.clear();
        editableItems.clear();
        return this;
    }

    public boolean isPlayerSlotEditable(int slot) {
        return editablePlayerSlots.contains(slot);
    }

    public boolean setEditablePlayerItem(int slot, ItemStack item) {
        if (!editablePlayerSlots.contains(slot)) {
            return false;
        }

        if (item == null || item.getType().isAir()) {
            editableItems.remove(slot);
        } else {
            editableItems.put(slot, item.clone());
        }

        return true;
    }

    public ItemStack getEditablePlayerItem(int slot) {
        ItemStack item = editableItems.get(slot);
        return item != null ? item.clone() : null;
    }

    public Map<Integer, ItemStack> getEditablePlayerItems() {
        Map<Integer, ItemStack> result = new HashMap<>();
        editableItems.forEach((slot, item) -> result.put(slot, item.clone()));
        return result;
    }

    @Override
    public void open(Player player, ExyliaContext additionalContext) {
        capturePlayerInventory(player);

        super.open(player, additionalContext);

        if (clearPlayerInventoryOnOpen) {
            player.getInventory().clear();
        }

        populatePlayerInventory();
    }

    @Override
    protected void onClose() {
        if (viewer != null) {
            if (restoreInventoryOnClose) {
                restorePlayerInventory(viewer);
            } else {
                clearNonEditablePlayerSlots();
            }
            playerSnapshots.remove(viewer.getUniqueId());
        }

        super.onClose();
    }

    private void clearNonEditablePlayerSlots() {
        if (viewer == null) return;

        PlayerInventory playerInv = viewer.getInventory();

        for (int i = 0; i < PLAYER_INVENTORY_SIZE; i++) {
            if (!editablePlayerSlots.contains(i)) {
                playerInv.setItem(i, null);
            }
        }
    }

    protected void populatePlayerInventory() {
        if (viewer == null || !isOpen()) return;

        PlayerInventory playerInv = viewer.getInventory();

        for (int i = 0; i < PLAYER_INVENTORY_SIZE; i++) {
            updatePlayerSlot(i);
        }
    }

    protected void updatePlayerSlot(int playerSlot) {
        if (viewer == null || !isValidPlayerSlot(playerSlot)) return;

        PlayerInventory playerInv = viewer.getInventory();

        if (editablePlayerSlots.contains(playerSlot)) {
            ItemStack editableItem = editableItems.get(playerSlot);
            playerInv.setItem(playerSlot, editableItem);
            return;
        }

        MenuItem menuItem = playerInventoryItems.get(playerSlot);
        if (menuItem != null) {
            ExyliaContext itemContext = prepareItemContext(menuItem);
            menuItem.withContext(itemContext);
            playerInv.setItem(playerSlot, menuItem.buildProcessed(viewer));
        } else {
            playerInv.setItem(playerSlot, null);
        }
    }

    public MenuItem getPlayerSlotItem(int playerSlot) {
        return playerInventoryItems.get(playerSlot);
    }

    public Map<Integer, MenuItem> getPlayerInventoryItems() {
        return new HashMap<>(playerInventoryItems);
    }

    private void capturePlayerInventory(Player player) {
        InventorySnapshot snapshot = InventorySnapshot.capture(player);
        playerSnapshots.put(player.getUniqueId(), snapshot);
    }

    private void restorePlayerInventory(Player player) {
        InventorySnapshot snapshot = playerSnapshots.get(player.getUniqueId());
        if (snapshot != null) {
            Schedulers.sync(() -> {
                snapshot.restore(player);
            });
        }
    }

    public static void forceRestoreInventory(Player player) {
        InventorySnapshot snapshot = playerSnapshots.remove(player.getUniqueId());
        if (snapshot != null) {
            snapshot.restore(player);
        }
    }

    public static void clearSnapshot(Player player) {
        playerSnapshots.remove(player.getUniqueId());
    }

    public static boolean hasSnapshot(Player player) {
        return playerSnapshots.containsKey(player.getUniqueId());
    }

    protected static void cleanupExpiredSnapshots() {
        long maxAge = 5 * 60 * 1000;
        playerSnapshots.entrySet().removeIf(entry -> entry.getValue().hasExpired(maxAge));
    }

    protected boolean isValidPlayerSlot(int slot) {
        return slot >= 0 && slot < PLAYER_INVENTORY_SIZE;
    }

    public boolean isHotbarSlot(int slot) {
        return slot >= HOTBAR_START && slot <= HOTBAR_END;
    }

    public boolean isMainInventorySlot(int slot) {
        return slot >= MAIN_INVENTORY_START && slot <= MAIN_INVENTORY_END;
    }

    public int convertToPlayerSlot(int rawSlot) {
        int inventorySize = size;
        if (rawSlot >= inventorySize && rawSlot < inventorySize + PLAYER_INVENTORY_SIZE) {
            return rawSlot - inventorySize;
        }
        return -1;
    }

    public boolean isPlayerInventorySlot(int rawSlot) {
        return convertToPlayerSlot(rawSlot) != -1;
    }
}
