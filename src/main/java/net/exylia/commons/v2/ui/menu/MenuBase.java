package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionSource;
import net.exylia.commons.v2.command.api.CommandAPI;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ItemsAPI;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.exception.MenuStateException;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.refresh.RefreshMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public abstract class MenuBase {

    protected final UUID menuId;
    protected final Player player;
    protected final MenuData menuData;
    protected final PlaceholderContext context;
    protected final Map<Integer, ProcessedItem> itemsBySlot;
    protected final AtomicReference<MenuState> state;

    protected Inventory inventory;
    protected ScheduledTask refreshTask;

    protected MenuBase(Player player, MenuData menuData) {
        this.menuId = UUID.randomUUID();
        this.player = player;
        this.menuData = menuData.copy();
        this.context = prepareContext(menuData);
        this.itemsBySlot = new ConcurrentHashMap<>();
        this.state = new AtomicReference<>(MenuState.CLOSED);
    }

    protected PlaceholderContext prepareContext(MenuData menuData) {
        return menuData.getContext()
                .copy()
                .withPlayer(player)
                .put("menu_id", menuId.toString())
                .put("menu_type", menuData.getType().name())
                .put("menu_title", menuData.getTitle())
                .put("menu_size", menuData.getSize());
    }

    protected abstract Inventory createInventory();

    protected abstract void populateItems();

    protected abstract void handleClickInternal(int slot, ClickType clickType);

    public CompletableFuture<Void> openAsync() {
        if (state.get() == MenuState.OPEN) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " already open for " + player.getName());
            return CompletableFuture.completedFuture(null);
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Opening menu " + menuId + " for " + player.getName());
        state.set(MenuState.TRANSITIONING);

        return CompletableFuture.runAsync(() -> {
            try {
                this.inventory = createInventory();
                populateItems();
                DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " inventory created and populated");
            } catch (Exception e) {
                state.set(MenuState.CLOSED);
                DebugAPI.logLibError(DebugCategory.UI, "Failed to populate menu " + menuId, e);
                throw new RuntimeException("Failed to populate menu items", e);
            }
        }).thenAcceptAsync(v -> Schedulers.sync(() -> {
            player.openInventory(inventory);
            state.set(MenuState.OPEN);
            scheduleRefresh();
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " opened successfully for " + player.getName());
        }));
    }

    public void open() {
        openAsync().join();
    }

    public void close() {
        if (state.get() == MenuState.CLOSED) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Closing menu " + menuId + " for " + player.getName());
        state.set(MenuState.CLOSED);
        player.closeInventory();
        cancelRefresh();
        cleanup();
        DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " closed successfully");
    }

    public void handleClick(int slot, ClickType clickType) {
        if (state.get() != MenuState.OPEN) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Click on menu " + menuId + " slot " + slot + " (type: " + clickType + ")");

        ProcessedItem item = itemsBySlot.get(slot);
        if (item != null) {
            executeItemActions(item, slot, clickType);
        }

        handleClickInternal(slot, clickType);

        if (menuData.getRefreshMode() != RefreshMode.DISABLED) {
            schedulePostClickRefresh();
        }
    }

    protected void executeItemActions(ProcessedItem item, int slot, ClickType clickType) {
        List<String> actionsToExecute = new ArrayList<>();

        if (clickType.isRightClick() && item.getRightClickActions() != null) {
            actionsToExecute.addAll(item.getRightClickActions());
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing " + actionsToExecute.size() + " right-click actions for slot " + slot);
        } else if (item.getClickActions() != null) {
            actionsToExecute.addAll(item.getClickActions());
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing " + actionsToExecute.size() + " click actions for slot " + slot);
        }

        for (String action : actionsToExecute) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing action: " + action);
            ActionContext actionContext = ActionContext.builder()
                    .player(player)
                    .source(ActionSource.MENU)
                    .data(Map.of(
                            "menu_id", menuId.toString(),
                            "slot", slot,
                            "click_type", clickType.name()
                    ))
                    .build();

            ActionAPI.executeAsync(action, actionContext);
        }

        if (item.getCommands() != null && !item.getCommands().isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing " + item.getCommands().size() + " commands for slot " + slot);
            for (String command : item.getCommands()) {
                DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing command: " + command);
                CommandAPI.execute(player, command, context);
            }
        }
    }

    protected void schedulePostClickRefresh() {
        Schedulers.syncLater(() -> {
            if (state.get() == MenuState.OPEN && inventory != null) {
                refreshInventory();
            }
        }, 1L);
    }

    protected void scheduleRefresh() {
        if (menuData.getRefreshMode() == RefreshMode.DISABLED) {
            return;
        }

        boolean hasDynamicItems = itemsBySlot.values().stream()
                .anyMatch(ProcessedItem::needsRefresh);

        if (!hasDynamicItems) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " has no dynamic items, skipping refresh schedule");
            return;
        }

        long interval = menuData.getRefreshInterval();
        DebugAPI.logLibDebug(DebugCategory.UI, "Scheduling refresh for menu " + menuId + " (mode: " + menuData.getRefreshMode() + ", interval: " + interval + ")");

        refreshTask = Schedulers.syncTimer(() -> {
            if (state.get() == MenuState.OPEN && inventory != null) {
                refreshInventory();
            } else {
                cancelRefresh();
            }
        }, interval, interval);
    }

    protected void cancelRefresh() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    protected void refreshInventory() {
        DebugAPI.logLibDebug(DebugCategory.UI, "Refreshing menu " + menuId + " (mode: " + menuData.getRefreshMode() + ")");
        switch (menuData.getRefreshMode()) {
            case FULL:
                fullRefresh();
                break;
            case SMART:
                smartRefresh();
                break;
            case SLOT_ONLY:
                slotOnlyRefresh();
                break;
            default:
                break;
        }
    }

    protected void fullRefresh() {
        Schedulers.async(() -> {
            populateItems();
            Schedulers.sync(this::updateInventoryDisplay);
        });
    }

    protected void smartRefresh() {
        List<Integer> slotsToRefresh = new ArrayList<>();

        for (Map.Entry<Integer, ProcessedItem> entry : itemsBySlot.entrySet()) {
            if (entry.getValue().needsRefresh()) {
                slotsToRefresh.add(entry.getKey());
            }
        }

        if (slotsToRefresh.isEmpty()) {
            return;
        }

        Schedulers.async(() -> {
            for (Integer slot : slotsToRefresh) {
                ProcessedItem oldItem = itemsBySlot.get(slot);
                if (oldItem != null) {
                    ItemData itemData = oldItem.getRawItemData();
                    ProcessedItem newItem = ItemsAPI.process(itemData, player, false);
                    itemsBySlot.put(slot, newItem);
                }
            }

            Schedulers.sync(this::updateInventoryDisplay);
        });
    }

    protected void slotOnlyRefresh() {
        smartRefresh();
    }

    protected void updateInventoryDisplay() {
        if (inventory == null) {
            return;
        }

        itemsBySlot.forEach((slot, item) -> {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item.getItemStack());
            }
        });
    }

    protected void setItem(int slot, ProcessedItem processedItem) {
        if (slot < 0 || slot >= menuData.getSize()) {
            DebugAPI.logLibWarn(DebugCategory.UI, "Menu " + menuId + " attempted to set item at invalid slot " + slot + " (size: " + menuData.getSize() + ")");
            return;
        }

        itemsBySlot.put(slot, processedItem);

        if (inventory != null) {
            inventory.setItem(slot, processedItem.getItemStack());
        }
    }

    protected void setItem(int slot, ItemData itemData) {
        PlaceholderContext mergedContext;

        if (itemData.getContext() != null) {
            mergedContext = this.context.copyAndMerge(itemData.getContext());
        } else {
            mergedContext = this.context;
        }

        ItemData enhancedItemData = itemData.toBuilder()
                .context(mergedContext)
                .build();

        ProcessedItem processedItem = ItemsAPI.process(enhancedItemData, player, false);

        boolean hasActions = (processedItem.getClickActions() != null && !processedItem.getClickActions().isEmpty())
                || (processedItem.getRightClickActions() != null && !processedItem.getRightClickActions().isEmpty());
        boolean hasCommands = processedItem.getCommands() != null && !processedItem.getCommands().isEmpty();

        if (hasActions || hasCommands) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " setting interactive item at slot " + slot +
                    " (actions: " + hasActions + ", commands: " + hasCommands + ")");
        }

        setItem(slot, processedItem);
    }

    protected void applyFillers() {
        Set<Integer> occupiedSlots = new HashSet<>(itemsBySlot.keySet());
        int fillerCount = 0;

        for (int slot = 0; slot < menuData.getSize(); slot++) {
            if (occupiedSlots.contains(slot)) {
                continue;
            }

            ItemData filler = getFillerForSlot(slot);
            if (filler != null) {
                setItem(slot, filler);
                fillerCount++;
            }
        }

        if (fillerCount > 0) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " applied " + fillerCount + " filler items");
        }
    }

    protected ItemData getFillerForSlot(int slot) {
        if (isBorderSlot(slot) && menuData.hasBorderFiller()) {
            return menuData.getBorderFiller();
        }

        return menuData.getGlobalFiller();
    }

    protected boolean isBorderSlot(int slot) {
        int rows = menuData.getRows();
        int cols = 9;

        int row = slot / cols;
        int col = slot % cols;

        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    protected void applyStaticItems() {
        if (menuData.getItems() == null || menuData.getItems().isEmpty()) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " applying " + menuData.getItems().size() + " static items");

        for (Map.Entry<String, ItemData> entry : menuData.getItems().entrySet()) {
            ItemData itemData = entry.getValue();

            if (itemData.getSlotConfig() != null) {
                try {
                    if (itemData.getSlotConfig().isSingle()) {
                        Integer slot = itemData.getSlotConfig().getSingleSlot();
                        if (slot != null) {
                            setItem(slot, itemData);
                        }
                    } else if (itemData.getSlotConfig().isMultiple()) {
                        List<Integer> slots = itemData.getSlotConfig().getMultiSlots();
                        if (slots != null) {
                            for (Integer slot : slots) {
                                setItem(slot, itemData);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Should never happen as we check isSingle/isMultiple before
                }
            }
        }
    }

    protected void cleanup() {
        cancelRefresh();
        itemsBySlot.clear();
    }

    public boolean isOpen() {
        return state.get() == MenuState.OPEN;
    }

    public void ensureOpen() {
        if (state.get() != MenuState.OPEN) {
            throw new MenuStateException(MenuState.OPEN, state.get());
        }
    }
}
