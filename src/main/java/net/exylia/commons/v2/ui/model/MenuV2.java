package net.exylia.commons.v2.ui.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.ui.event.MenuClickEvent;
import net.exylia.commons.v2.ui.refresh.RefreshStrategy;
import net.exylia.commons.v2.ui.sound.SoundConfig;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
@Setter
public abstract class MenuV2 implements InventoryHolder {
    protected final String id;
    protected final MenuType type;
    protected String title;
    protected String rawTitle;
    protected int size;
    protected int rows;

    protected MenuContext context;
    protected MenuState state;
    protected Player viewer;
    protected Inventory inventory;

    protected final Map<Integer, MenuItemV2> items = new ConcurrentHashMap<>();
    protected MenuItemV2 globalFiller;
    protected MenuItemV2 borderFiller;

    protected Consumer<MenuClickEvent> globalClickHandler;
    protected Consumer<Player> openHandler;
    protected Consumer<Player> closeHandler;

    protected MenuV2 parentMenu;

    protected boolean autoRefreshOnClick = true;
    protected RefreshMode refreshMode = RefreshMode.SMART;
    protected RefreshStrategy refreshStrategy;

    protected boolean dynamicUpdates = false;
    protected long updateInterval = 20L;
    protected ScheduledTask updateTask;

    protected SoundConfig openSound;
    protected SoundConfig closeSound;
    protected SoundConfig clickSound;

    public MenuV2(String id, MenuType type) {
        this.id = id;
        this.type = type;
        this.state = MenuState.CREATED;
    }

    public abstract void open(Player player, MenuContext context);

    public void close() {
        if (state != MenuState.OPEN) {
            return;
        }

        state = MenuState.CLOSING;

        stopDynamicUpdates();

        if (closeHandler != null && viewer != null) {
            closeHandler.accept(viewer);
        }

        if (closeSound != null && viewer != null) {
            closeSound.play(viewer);
        }

        if (viewer != null && viewer.getOpenInventory().getTopInventory().equals(inventory)) {
            viewer.closeInventory();
        }

        state = MenuState.CLOSED;
    }

    public void refresh() {
        if (state != MenuState.OPEN || viewer == null || !viewer.isOnline()) {
            return;
        }

        Schedulers.sync(() -> {
            processTitle();
            applyFillers();
            populateInventory();
        });
    }

    public void processTitle() {
        if (rawTitle == null) {
            return;
        }

        String processed = Placeholders.process(rawTitle, context.getPlaceholderContext());
        this.title = ColorAPI.parseToString(processed);
    }

    protected void createInventory() {
        Component titleComponent = ColorAPI.parse(title != null ? title : "Menu");
        this.inventory = Bukkit.createInventory(this, size, titleComponent);
    }

    public abstract void populateInventory();

    protected void applyFillers() {
        if (globalFiller != null) {
            for (int i = 0; i < size; i++) {
                if (!items.containsKey(i)) {
                    inventory.setItem(i, globalFiller.build(viewer, context.getPlaceholderContext()));
                }
            }
        }

        if (borderFiller != null) {
            List<Integer> borderSlots = getBorderSlots();
            for (int slot : borderSlots) {
                if (!items.containsKey(slot)) {
                    inventory.setItem(slot, borderFiller.build(viewer, context.getPlaceholderContext()));
                }
            }
        }
    }

    public void updateSlot(int slot) {
        if (inventory == null || state != MenuState.OPEN) {
            return;
        }

        MenuItemV2 item = items.get(slot);
        if (item != null) {
            inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
        }
    }

    protected void startDynamicUpdates() {
        if (!dynamicUpdates || updateTask != null) {
            return;
        }

        updateTask = Schedulers.syncTimer(this::refresh, updateInterval, updateInterval);
    }

    protected void stopDynamicUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    protected void enrichContext() {
        if (context == null) {
            return;
        }

        context.getPlaceholderContext()
                .put("menu_id", id)
                .put("menu_type", type.name())
                .put("menu_title", rawTitle != null ? rawTitle : title);
    }

    public void handleClick(MenuClickEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (clickSound != null) {
            clickSound.play(event.getPlayer());
        }

        if (globalClickHandler != null) {
            globalClickHandler.accept(event);
        }

        MenuItemV2 item = event.getItem();
        if (item != null && !event.isCancelled()) {
            item.executeActions(event.getPlayer(), event.getClickType(), context);
        }

        if (autoRefreshOnClick && refreshStrategy != null && !event.isCancelled()) {
            Schedulers.syncLater(() -> {
                refreshStrategy.refresh(this, event.getSlot());
            }, 1L);
        }
    }

    protected void handleOpen() {
        if (openHandler != null && viewer != null) {
            openHandler.accept(viewer);
        }

        if (openSound != null && viewer != null) {
            openSound.play(viewer);
        }
    }

    public MenuV2 setItem(int slot, MenuItemV2 item) {
        if (slot < 0 || slot >= size) {
            return this;
        }

        items.put(slot, item);

        if (inventory != null && state == MenuState.OPEN) {
            updateSlot(slot);
        }

        return this;
    }

    public MenuItemV2 getItem(int slot) {
        return items.get(slot);
    }

    public MenuV2 removeItem(int slot) {
        items.remove(slot);

        if (inventory != null) {
            inventory.setItem(slot, null);
        }

        return this;
    }

    public MenuV2 clearItems() {
        items.clear();

        if (inventory != null) {
            inventory.clear();
        }

        return this;
    }

    public MenuV2 setGlobalFiller(MenuItemV2 filler) {
        this.globalFiller = filler;
        return this;
    }

    public MenuV2 setBorderFiller(MenuItemV2 filler) {
        this.borderFiller = filler;
        return this;
    }

    public MenuV2 setGlobalClickHandler(Consumer<MenuClickEvent> handler) {
        this.globalClickHandler = handler;
        return this;
    }

    public MenuV2 setOpenHandler(Consumer<Player> handler) {
        this.openHandler = handler;
        return this;
    }

    public MenuV2 setCloseHandler(Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    public MenuV2 enableDynamicUpdates(long intervalTicks) {
        this.dynamicUpdates = true;
        this.updateInterval = intervalTicks;
        return this;
    }

    public MenuV2 disableDynamicUpdates() {
        this.dynamicUpdates = false;
        stopDynamicUpdates();
        return this;
    }

    protected List<Integer> getBorderSlots() {
        List<Integer> borderSlots = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            borderSlots.add(i);
        }

        for (int i = (rows - 1) * 9; i < rows * 9; i++) {
            borderSlots.add(i);
        }

        for (int row = 1; row < rows - 1; row++) {
            borderSlots.add(row * 9);
            borderSlots.add(row * 9 + 8);
        }

        return borderSlots;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public MenuV2 clone() {
        try {
            MenuV2 cloned = (MenuV2) super.clone();
            cloned.items.clear();
            this.items.forEach((slot, item) -> cloned.items.put(slot, item.clone()));
            cloned.state = MenuState.CREATED;
            cloned.viewer = null;
            cloned.inventory = null;
            cloned.updateTask = null;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone menu", e);
        }
    }
}
