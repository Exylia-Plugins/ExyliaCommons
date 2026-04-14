package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionSource;
import net.exylia.commons.v2.command.api.CommandAPI;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ItemsAPI;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.animation.AnimationExecutor;
import net.exylia.commons.v2.ui.animation.AnimationSettings;
import net.exylia.commons.v2.ui.exception.MenuStateException;
import net.exylia.commons.v2.ui.model.FillerData;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.packet.PacketEventsSupport;
import net.exylia.commons.v2.ui.refresh.RefreshMode;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public abstract class MenuBase {

    private static final long CLICK_PROTECTION_COOLDOWN_MILLIS = 150L;

    protected final UUID menuId;
    protected final Player player;
    protected final MenuData menuData;
    protected final PlaceholderContext context;
    protected final Map<Integer, ProcessedItem> itemsBySlot;
    protected final AtomicReference<MenuState> state;
    protected final AtomicBoolean animationCancelFlag;
    protected final AtomicBoolean suppressDisplay;
    protected final AtomicLong lastClickAtMillis;

    protected Inventory inventory;
    protected ScheduledTask refreshTask;
    protected String lastRenderedTitle;

    protected MenuBase(Player player, MenuData menuData) {
        this.menuId = UUID.randomUUID();
        this.player = player;
        this.menuData = menuData.copy();
        this.context = prepareContext(menuData);
        this.itemsBySlot = new ConcurrentHashMap<>();
        this.state = new AtomicReference<>(MenuState.CLOSED);
        this.animationCancelFlag = new AtomicBoolean(false);
        this.suppressDisplay = new AtomicBoolean(false);
        this.lastClickAtMillis = new AtomicLong(0L);
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

    protected void populateItemsWithoutDisplay() {
        suppressDisplay.set(true);
        try {
            populateItems();
        } finally {
            suppressDisplay.set(false);
        }
    }

    protected abstract void handleClickInternal(int slot, ClickType clickType);

    public CompletableFuture<Void> openAsync() {
        if (state.get() == MenuState.OPEN) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " already open for " + player.getName());
            return CompletableFuture.completedFuture(null);
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Opening menu " + menuId + " for " + player.getName());
        state.set(MenuState.TRANSITIONING);
        animationCancelFlag.set(false);

        CompletableFuture<Void> future = new CompletableFuture<>();

        Tasks.sync(() -> {
            this.inventory = createInventory();

            Tasks.run(() -> {
                try {
                    populateItemsWithoutDisplay();
                    DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " items prepared");

                    Tasks.sync(() -> {
                        try {
                            player.openInventory(inventory);
                            state.set(MenuState.OPEN);
                            lastRenderedTitle = computeCurrentTitle();
                            playOpenSounds();

                            AnimationSettings animSettings = menuData.getAnimationSettings();
                            if (animSettings != null && animSettings.hasOpenAnimation()) {
                                AnimationExecutor.execute(
                                        inventory,
                                        itemsBySlot,
                                        animSettings.getOpenAnimation(),
                                        animSettings.getSpeed(),
                                        animationCancelFlag
                                ).thenRun(() -> {
                                    scheduleRefresh();
                                    DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " opened with animation for " + player.getName());
                                    future.complete(null);
                                });
                            } else {
                                updateInventoryDisplay();
                                scheduleRefresh();
                                DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " opened successfully for " + player.getName());
                                future.complete(null);
                            }
                        } catch (Throwable t) {
                            state.set(MenuState.CLOSED);
                            cancelRefresh();
                            cleanup();
                            DebugAPI.logLibError(DebugCategory.UI, "Failed to display menu " + menuId + " (sync phase)", t);
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    state.set(MenuState.CLOSED);
                    cancelRefresh();
                    cleanup();
                    DebugAPI.logLibError(DebugCategory.UI, "Failed to populate menu " + menuId + " (async phase)", t);
                    future.completeExceptionally(t);
                }
            });
        });

        return future;
    }

    public void open() {
        if (state.get() == MenuState.OPEN) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " already open for " + player.getName());
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Opening menu " + menuId + " synchronously for " + player.getName());
        state.set(MenuState.TRANSITIONING);
        animationCancelFlag.set(false);

        this.inventory = createInventory();

        Tasks.run(() -> {
            try {
                populateItemsWithoutDisplay();
                DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " items prepared");

                Tasks.sync(() -> {
                    try {
                        player.openInventory(inventory);
                        state.set(MenuState.OPEN);
                        lastRenderedTitle = computeCurrentTitle();
                        playOpenSounds();

                        AnimationSettings animSettings = menuData.getAnimationSettings();
                        if (animSettings != null && animSettings.hasOpenAnimation()) {
                            AnimationExecutor.execute(
                                    inventory,
                                    itemsBySlot,
                                    animSettings.getOpenAnimation(),
                                    animSettings.getSpeed(),
                                    animationCancelFlag
                            ).thenRun(() -> {
                                scheduleRefresh();
                                DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " opened with animation for " + player.getName());
                            });
                        } else {
                            updateInventoryDisplay();
                            scheduleRefresh();
                            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " opened successfully for " + player.getName());
                        }
                    } catch (Throwable t) {
                        state.set(MenuState.CLOSED);
                        cancelRefresh();
                        cleanup();
                        DebugAPI.logLibError(DebugCategory.UI, "Failed to display menu " + menuId + " (sync phase)", t);
                    }
                });
            } catch (Throwable t) {
                state.set(MenuState.CLOSED);
                cancelRefresh();
                cleanup();
                DebugAPI.logLibError(DebugCategory.UI, "Failed to populate menu " + menuId + " (async phase)", t);
            }
        });
    }

    public void close() {
        if (state.get() == MenuState.CLOSED) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Closing menu " + menuId + " for " + player.getName());
        state.set(MenuState.CLOSED);
        animationCancelFlag.set(true);
        playCloseSounds();
        player.closeInventory();
        cancelRefresh();
        cleanup();
        DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " closed successfully");
    }

    public void prepareTransition() {
        if (state.get() == MenuState.CLOSED) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Preparing transition from menu " + menuId + " for " + player.getName());
        state.set(MenuState.CLOSED);
        animationCancelFlag.set(true);
        cancelRefresh();
        cleanup();
    }

    public void handleClick(int slot, ClickType clickType) {
        if (state.get() != MenuState.OPEN) {
            return;
        }

        if (!canProcessClick()) {
            return;
        }

        if (DebugAPI.isLibDebugEnabled()) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Click on menu " + menuId + " slot " + slot + " (type: " + clickType + ")");
        }

        ProcessedItem item = itemsBySlot.get(slot);
        if (item != null) {
            executeItemActions(item, slot, clickType);
        }

        handleClickInternal(slot, clickType);

        if (menuData.getRefreshMode() != RefreshMode.DISABLED) {
            if (menuData.getRefreshMode() == RefreshMode.ON_CLICK || menuData.getRefreshMode() == RefreshMode.SMART) {
                scheduleClickedSlotRefresh(slot);
            } else {
                schedulePostClickRefresh();
            }
        }
    }

    protected boolean canProcessClick() {
        long now = System.currentTimeMillis();
        long lastClick = lastClickAtMillis.get();

        if (now - lastClick < CLICK_PROTECTION_COOLDOWN_MILLIS) {
            return false;
        }

        lastClickAtMillis.set(now);
        return true;
    }

    protected void executeItemActions(ProcessedItem item, int slot, ClickType clickType) {
        ClickTypeGroup clickGroup = ClickTypeGroup.fromBukkit(clickType);
        boolean debug = DebugAPI.isLibDebugEnabled();

        if (debug) {
            DebugAPI.logLibDebug(DebugCategory.UI,
                "Menu " + menuId + " processing click: type=" + clickType +
                ", group=" + clickGroup + ", slot=" + slot);
        }

        boolean hasItemClickSounds = item.getRawItemData() != null &&
                                      item.getRawItemData().getClickSounds() != null &&
                                      !item.getRawItemData().getClickSounds().isEmpty();

        if (hasItemClickSounds) {
            playItemClickSounds(item.getRawItemData());
        } else if (menuData.hasClickSounds()) {
            playMenuClickSounds();
        }

        List<String> actionsToExecute = item.getActionsForClick(clickType);

        if (!actionsToExecute.isEmpty()) {
            if (debug) {
                DebugAPI.logLibDebug(DebugCategory.UI,
                    "Menu " + menuId + " executing " + actionsToExecute.size() +
                    " actions for click type " + clickGroup);
            }

            ActionContext actionContext = ActionContext.builder()
                .player(player)
                .source(ActionSource.MENU)
                .data(Map.of(
                    "menu_id", menuId.toString(),
                    "slot", slot,
                    "click_type", clickType.name(),
                    "click_group", clickGroup.name()
                ))
                .build();

            for (String action : actionsToExecute) {
                String processedAction = action;
                if (item.getRawItemData() != null && item.getRawItemData().getContext() != null) {
                    processedAction = Placeholders.process(action, player, item.getRawItemData().getContext());
                }

                if (debug) {
                    DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing action: " + processedAction);
                }

                ActionAPI.executeAsync(processedAction, actionContext);
            }
        }

        List<String> commandsToExecute = item.getCommandsForClick(clickType);

        if (!commandsToExecute.isEmpty()) {
            if (debug) {
                DebugAPI.logLibDebug(DebugCategory.UI,
                    "Menu " + menuId + " executing " + commandsToExecute.size() +
                    " commands for click type " + clickGroup);
            }

            for (String command : commandsToExecute) {
                if (debug) {
                    DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " executing command: " + command);
                }
                CommandAPI.execute(player, command, context);
            }
        }

        if (debug && actionsToExecute.isEmpty() && commandsToExecute.isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.UI,
                "Menu " + menuId + " no actions or commands matched click type " +
                clickGroup + " for slot " + slot);
        }
    }

    protected void schedulePostClickRefresh() {
        Tasks.later(() -> {
            if (state.get() == MenuState.OPEN && inventory != null) {
                refreshInventory();
            }
        }, menuData.getClickRefreshDelay());
    }

    protected void scheduleClickedSlotRefresh(int slot) {
        Tasks.later(() -> {
            if (state.get() == MenuState.OPEN && inventory != null) {
                refreshSlot(slot);
            }
        }, menuData.getClickRefreshDelay());
    }

    protected void refreshSlot(int slot) {
        ProcessedItem oldItem = itemsBySlot.get(slot);
        if (oldItem == null || !oldItem.needsRefresh()) {
            return;
        }

        DebugAPI.logLibDebug(DebugCategory.UI, "Refreshing slot " + slot + " in menu " + menuId);

        Tasks.run(() -> {
            ProcessedItem newItem = ItemsAPI.process(oldItem.getRawItemData(), player, false);
            ItemStack oldStack = oldItem.getItemStack();
            ItemStack newStack = newItem.getItemStack();
            itemsBySlot.put(slot, newItem);

            if (!Objects.equals(oldStack, newStack)) {
                Tasks.sync(() -> {
                    if (inventory != null && slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, newStack);
                    }
                });
            }
        });
    }

    protected void scheduleRefresh() {
        if (menuData.getRefreshMode() == RefreshMode.DISABLED || menuData.getRefreshMode() == RefreshMode.ON_CLICK) {
            return;
        }

        boolean hasDynamicItems = false;
        for (ProcessedItem item : itemsBySlot.values()) {
            if (item.needsRefresh()) {
                hasDynamicItems = true;
                break;
            }
        }

        boolean hasPaginationSupplier = menuData.getPaginationItemsSupplier() != null;

        if (!hasDynamicItems && !hasPaginationSupplier) {
            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " has no dynamic items, skipping refresh schedule");
            }
            return;
        }

        long interval = menuData.getRefreshInterval();
        if (DebugAPI.isLibDebugEnabled()) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Scheduling refresh for menu " + menuId + " (mode: " + menuData.getRefreshMode() + ", interval: " + interval + ")");
        }

        refreshTask = Tasks.timer(() -> {
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
        if (DebugAPI.isLibDebugEnabled()) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Refreshing menu " + menuId + " (mode: " + menuData.getRefreshMode() + ")");
        }
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
        Tasks.run(() -> {
            try {
                populateItemsWithoutDisplay();
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.UI, "Error populating menu " + menuId + " during fullRefresh", e);
            }
            Tasks.sync(() -> {
                if (state.get() != MenuState.OPEN || inventory == null) return;
                updateInventoryDisplay();
                refreshTitle();
            });
        });
    }

    protected void smartRefresh() {
        boolean hasDynamic = false;
        for (ProcessedItem item : itemsBySlot.values()) {
            if (item.needsRefresh()) {
                hasDynamic = true;
                break;
            }
        }

        if (!hasDynamic) {
            return;
        }

        Tasks.run(() -> {
            Map<Integer, ItemStack> updates = new HashMap<>();

            for (Map.Entry<Integer, ProcessedItem> entry : itemsBySlot.entrySet()) {
                ProcessedItem oldItem = entry.getValue();
                if (oldItem == null || !oldItem.needsRefresh()) continue;

                ProcessedItem newItem = ItemsAPI.process(oldItem.getRawItemData(), player, false);
                ItemStack oldStack = oldItem.getItemStack();
                ItemStack newStack = newItem.getItemStack();

                itemsBySlot.put(entry.getKey(), newItem);

                if (!Objects.equals(oldStack, newStack)) {
                    updates.put(entry.getKey(), newStack);
                }
            }

            Tasks.sync(() -> {
                if (state.get() != MenuState.OPEN || inventory == null) return;
                updates.forEach((slot, stack) -> {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, stack);
                    }
                });
                refreshTitle();
            });
        });
    }

    protected void refreshTitle() {
        if (inventory == null) {
            return;
        }
        String processedTitle = processTitle(menuData.getTitle());
        if (processedTitle.equals(lastRenderedTitle)) {
            return;
        }
        lastRenderedTitle = processedTitle;
        PacketEventsSupport.updateTitle(player, inventory, ColorAPI.parse(processedTitle));
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
                ItemStack newStack = item.getItemStack();
                ItemStack current = inventory.getItem(slot);
                if (!Objects.equals(current, newStack)) {
                    inventory.setItem(slot, newStack);
                }
            }
        });
    }

    protected void setItem(int slot, ProcessedItem processedItem) {
        if (slot < 0 || slot >= menuData.getSize()) {
            DebugAPI.logLibWarn(DebugCategory.UI, "Menu " + menuId + " attempted to set item at invalid slot " + slot + " (size: " + menuData.getSize() + ")");
            return;
        }

        itemsBySlot.put(slot, processedItem);

        if (inventory != null && !suppressDisplay.get()) {
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

        ProcessedItem processedItem;
        try {
            processedItem = ItemsAPI.process(enhancedItemData, player, false);
        } catch (Throwable t) {
            DebugAPI.logLibError(DebugCategory.UI,
                "Menu " + menuId + " failed to process item at slot " + slot +
                " (material: " + enhancedItemData.getRawMaterial() + ")", t);
            return;
        }

        boolean hasActions = processedItem.getActions() != null &&
                             !processedItem.getActions().isEmpty();
        boolean hasCommands = processedItem.getCommands() != null &&
                              !processedItem.getCommands().isEmpty();

        if (hasActions || hasCommands) {
            DebugAPI.logLibDebug(DebugCategory.UI,
                "Menu " + menuId + " setting interactive item at slot " + slot +
                " (actions: " + (hasActions ? processedItem.getActions().size() : 0) +
                ", commands: " + (hasCommands ? processedItem.getCommands().size() : 0) + ")");
        }

        setItem(slot, processedItem);
    }

    protected void applyFillers() {
        int fillerCount = 0;

        for (int slot = 0; slot < menuData.getSize(); slot++) {
            if (itemsBySlot.containsKey(slot)) {
                continue;
            }

            ItemData filler = getFillerForSlot(slot);
            if (filler != null) {
                setItem(slot, filler);
                fillerCount++;
            }
        }

        if (fillerCount > 0) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " applied " + fillerCount + " global/border filler items");
        }

        applyCustomFillers();
    }

    protected void applyCustomFillers() {
        if (!menuData.hasCustomFillers()) {
            return;
        }

        int customFillerCount = 0;

        for (FillerData fillerData : menuData.getCustomFillers()) {
            if (fillerData.getItemData() == null || fillerData.getSlots().isEmpty()) {
                continue;
            }

            for (Integer slot : fillerData.getSlots()) {
                if (slot >= 0 && slot < menuData.getSize()) {
                    setItem(slot, fillerData.getItemData());
                    customFillerCount++;
                }
            }
        }

        if (customFillerCount > 0) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " applied " + customFillerCount + " custom filler items");
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

    protected String processTitle(String title) {
        return Placeholders.process(title, player, context);
    }

    protected String computeCurrentTitle() {
        return processTitle(menuData.getTitle());
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
                    DebugAPI.logLibError(DebugCategory.UI, "Menu " + menuId + " failed to set item '" + entry.getKey() + "': " + e.getMessage(), e);
                }
            }
        }
    }

    protected void cleanup() {
        cancelRefresh();
        itemsBySlot.clear();
    }

    protected void playOpenSounds() {
        if (menuData.hasOpenSounds()) {
            for (String sound : menuData.getOpenSounds()) {
                SoundAPI.play(player, sound);
            }
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " played " + menuData.getOpenSounds().size() + " open sounds");
        }
    }

    protected void playCloseSounds() {
        if (menuData.hasCloseSounds()) {
            for (String sound : menuData.getCloseSounds()) {
                SoundAPI.play(player, sound);
            }
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " played " + menuData.getCloseSounds().size() + " close sounds");
        }
    }

    protected void playMenuClickSounds() {
        if (menuData.hasClickSounds()) {
            for (String sound : menuData.getClickSounds()) {
                SoundAPI.play(player, sound);
            }
        }
    }

    protected void playItemClickSounds(ItemData itemData) {
        if (itemData.getClickSounds() != null && !itemData.getClickSounds().isEmpty()) {
            for (String sound : itemData.getClickSounds()) {
                SoundAPI.play(player, sound);
            }
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " played " + itemData.getClickSounds().size() + " item click sounds");
        }
    }

    public boolean isOpen() {
        return state.get() == MenuState.OPEN;
    }

    public MenuData getMenuData() {
        return menuData;
    }

    public void ensureOpen() {
        if (state.get() != MenuState.OPEN) {
            throw new MenuStateException(MenuState.OPEN, state.get());
        }
    }
}
