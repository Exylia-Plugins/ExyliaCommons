package net.exylia.commons.ui.core;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.manager.MenuManager;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.effects.SoundUtils;
import net.exylia.commons.utils.versions.InventoryAdapter;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Menu {

    @Getter
    protected final String id;
    @Getter
    protected Component title;
    @Getter
    protected String rawTitle;
    @Getter
    protected final int size;
    @Getter
    protected final int rows;

    @Getter
    protected Inventory inventory;
    @Getter
    protected Player viewer;
    @Getter
    protected ExyliaContext context = ExyliaContext.create();
    @Getter
    protected boolean isOpen = false;

    protected final Map<Integer, MenuItem> items = new ConcurrentHashMap<>();
    protected Consumer<MenuClickEvent> globalClickHandler;
    protected Consumer<Player> closeHandler;
    @Getter
    protected Menu parentMenu;

    protected MenuItem globalFiller;
    protected MenuItem borderFiller;

    protected boolean dynamicUpdates = false;
    protected JavaPlugin plugin = ExyliaPlugin.getInstance();
    protected long updateInterval = 20L;
    protected ScheduledTask updateTask = null;

    @Getter
    protected boolean autoRefreshOnClick = true;  
    @Getter
    protected RefreshMode refreshMode = RefreshMode.SMART;  

    protected List<String> openSounds = new ArrayList<>();
    protected List<String> closeSounds = new ArrayList<>();
    protected List<String> clickSounds = new ArrayList<>();

    protected static final InventoryAdapter inventoryAdapter = AdapterFactory.getInventoryAdapter();

    public enum RefreshMode {
        DISABLED,     
        FULL,         
        SMART,        
        SLOT_ONLY     
    }

    public Menu(String title, int rows) {
        this(UUID.randomUUID().toString(), title, rows);
    }

    public Menu(String id, String title, int rows) {
        this.id = id;
        this.rawTitle = title;
        this.title = ColorAPI.parse(title);
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
    }

    public Menu(String title, int rows, ExyliaContext context) {
        this(title, rows);
        this.context = context != null ? context : ExyliaContext.create();
    }

    public Menu setAutoRefreshOnClick(boolean enabled) {
        this.autoRefreshOnClick = enabled;
        return this;
    }

    public Menu setRefreshMode(RefreshMode mode) {
        this.refreshMode = mode != null ? mode : RefreshMode.SMART;
        return this;
    }

    public Menu enableSmartRefresh() {
        this.autoRefreshOnClick = true;
        this.refreshMode = RefreshMode.SMART;
        return this;
    }

    public Menu enableFullRefresh() {
        this.autoRefreshOnClick = true;
        this.refreshMode = RefreshMode.FULL;
        return this;
    }

    public Menu disableAutoRefresh() {
        this.autoRefreshOnClick = false;
        this.refreshMode = RefreshMode.DISABLED;
        return this;
    }

    public Menu setOpenSounds(List<String> sounds) {
        this.openSounds = sounds != null ? new ArrayList<>(sounds) : new ArrayList<>();
        return this;
    }

    public Menu addOpenSound(String sound) {
        if (sound != null && !sound.trim().isEmpty()) {
            this.openSounds.add(sound);
        }
        return this;
    }

    public Menu setCloseSounds(List<String> sounds) {
        this.closeSounds = sounds != null ? new ArrayList<>(sounds) : new ArrayList<>();
        return this;
    }

    public Menu addCloseSound(String sound) {
        if (sound != null && !sound.trim().isEmpty()) {
            this.closeSounds.add(sound);
        }
        return this;
    }

    public Menu setClickSounds(List<String> sounds) {
        this.clickSounds = sounds != null ? new ArrayList<>(sounds) : new ArrayList<>();
        return this;
    }

    public Menu addClickSound(String sound) {
        if (sound != null && !sound.trim().isEmpty()) {
            this.clickSounds.add(sound);
        }
        return this;
    }

    public List<String> getOpenSounds() {
        return new ArrayList<>(openSounds);
    }

    public List<String> getCloseSounds() {
        return new ArrayList<>(closeSounds);
    }

    public List<String> getClickSounds() {
        return new ArrayList<>(clickSounds);
    }

    public void open(Player player) {
        open(player, null);
    }

    public void open(Player player, ExyliaContext additionalContext) {
        this.viewer = player;
        if (additionalContext != null) {
            this.context = this.context.copy().merge(additionalContext);
        }

        this.context.withPlayer(player);

        processTitle();

        createInventory();

        applyFillers();

        populateInventory();

        player.openInventory(inventory);
        this.isOpen = true;

        playOpenSounds(player);

        MenuManager.registerMenu(player, this);

        startUpdates();
    }

    public void close() {
        if (viewer != null && isOpen) {
            viewer.closeInventory();
        }
    }

    protected void onClose() {
        stopUpdates();

        Player currentPlayer = this.viewer;

        if (currentPlayer != null) {
            playCloseSounds(currentPlayer);
        }

        if (closeHandler != null && currentPlayer != null) {
            closeHandler.accept(currentPlayer);
        }

        this.isOpen = false;
        this.viewer = null;
    }

    public void handleClose() {
        onClose();
    }

    public Menu setItem(int slot, MenuItem item) {
        if (isValidSlot(slot)) {
            items.put(slot, item);

            if (inventory != null && isOpen) {
                updateSlot(slot);
            }
        }
        return this;
    }

    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    public Menu removeItem(int slot) {
        items.remove(slot);
        if (inventory != null && isOpen) {
            updateSlot(slot);
        }
        return this;
    }

    public Menu clearItems() {
        items.clear();
        if (inventory != null && isOpen) {
            populateInventory();
        }
        return this;
    }

    protected void updateSlot(int slot) {
        if (inventory == null || !isValidSlot(slot)) return;

        MenuItem item = getEffectiveItem(slot);
        if (item != null) {
             
            ExyliaContext itemContext = prepareItemContext(item);

            item.withContext(itemContext);

            inventory.setItem(slot, item.buildProcessed(viewer));
        } else {
            inventory.setItem(slot, null);
        }
    }

    protected ExyliaContext prepareItemContext(MenuItem item) {

        ExyliaContext combinedContext = this.context.createChild();

        if (item.getContext() != null && !item.getContext().isEmpty()) {
            combinedContext.merge(item.getContext());
        }

        return combinedContext;
    }

    protected MenuItem getEffectiveItem(int slot) {
        MenuItem item = items.get(slot);
        if (item != null) {
            return item;
        }

        if (borderFiller != null && isBorderSlot(slot)) {
            return borderFiller.clone();
        }

        if (globalFiller != null) {
            return globalFiller.clone();
        }

        return null;
    }

    protected void performAutoRefresh(int clickedSlot) {
        if (!autoRefreshOnClick || !isOpen || viewer == null) {
            return;
        }

        switch (refreshMode) {
            case DISABLED -> {
                 
            }
            case SLOT_ONLY -> refreshSlotOnly(clickedSlot);
            case SMART -> performSmartRefresh(clickedSlot);
            case FULL -> performFullRefresh();
        }
    }

    private void refreshSlotOnly(int slot) {
        updateSlot(slot);
    }

    private void performSmartRefresh(int clickedSlot) {
         
        updateSlot(clickedSlot);

        Set<Integer> slotsToUpdate = new HashSet<>();

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            MenuItem item = entry.getValue();

            if (slot != clickedSlot && shouldItemBeRefreshed(item)) {
                slotsToUpdate.add(slot);
            }
        }

        for (int slot : slotsToUpdate) {
            updateSlot(slot);
        }

        if (slotsToUpdate.size() > size / 2) {
            performFullRefresh();
        }
    }

    private boolean shouldItemBeRefreshed(MenuItem item) {
        if (item == null) return false;

        if (item.needsDynamicUpdate()) {
            return true;
        }

        if (item.hasDynamicLore()) {
            return true;
        }

        if (hasPlaceholders(item.getRawName()) ||
                hasPlaceholders(item.getRawAmount()) ||
                hasPlaceholdersInLore(item.getRawLore())) {
            return true;
        }

        return false;
    }

    private boolean hasPlaceholders(String text) {
        return text != null && (text.contains("{") || text.contains("%"));
    }

    private boolean hasPlaceholdersInLore(List<String> lore) {
        if (lore == null) return false;

        return lore.stream().anyMatch(this::hasPlaceholders);
    }

    private void performFullRefresh() {
         
        processTitle();

        populateInventory();
    }

    public Menu setGlobalFiller(MenuItem filler) {
        this.globalFiller = filler;
        if (inventory != null && isOpen) {
            populateInventory();
        }
        return this;
    }

    public Menu setBorderFiller(MenuItem filler) {
        this.borderFiller = filler;
        if (inventory != null && isOpen) {
            populateInventory();
        }
        return this;
    }

    public Menu setGlobalClickHandler(Consumer<MenuClickEvent> handler) {
        this.globalClickHandler = handler;
        return this;
    }

    public Menu setCloseHandler(Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    public Menu setParentMenu(Menu menu) {
        this.parentMenu = menu;
        return this;
    }

    public Menu enableDynamicUpdates(JavaPlugin plugin, long interval) {
        this.plugin = plugin;
        this.updateInterval = interval;
        this.dynamicUpdates = true;

        if (isOpen) {
            startUpdates();
        }
        return this;
    }

    public Menu disableDynamicUpdates() {
        this.dynamicUpdates = false;
        stopUpdates();
        return this;
    }

    protected void startUpdates() {
        if (!dynamicUpdates || plugin == null || updateTask != null) return;

        updateTask = Schedulers.syncTimer(
                this::updateDynamicItems,
                updateInterval,
                updateInterval
        );
    }

    protected void stopUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    protected void updateDynamicItems() {
        if (!isOpen || viewer == null || !viewer.isOnline()) {
            stopUpdates();
            return;
        }

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            MenuItem item = entry.getValue();
            if (item != null && item.needsDynamicUpdate()) {
                updateSlot(entry.getKey());
            }
        }
    }

    protected void processTitle() {
        if (rawTitle != null && context != null) {
            String processed = context.processPlaceholders(rawTitle, viewer);
            this.title = ColorAPI.parse(processed);
        }
    }

    protected void createInventory() {
        this.inventory = inventoryAdapter.createInventory(size, title);
    }

    protected void applyFillers() {
         
    }

    protected void populateInventory() {
        if (inventory == null) return;

        for (int i = 0; i < size; i++) {
            updateSlot(i);
        }
    }

    protected boolean isValidSlot(int slot) {
        return slot >= 0 && slot < size;
    }

    protected boolean isBorderSlot(int slot) {
        if (rows <= 2) return false;

        int row = slot / 9;
        int col = slot % 9;

        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    public Map<Integer, MenuItem> getItems() { return new HashMap<>(items); }

    public void handleClick(MenuClickEvent event) {
        if (globalClickHandler != null) {
            globalClickHandler.accept(event);
            if (event.isCancelled()) {
                return;
            }
        }

        MenuItem item = getItem(event.getSlot());
        if (item != null) {
            item.handleClick(event);
        }

        Schedulers.syncLater(() -> {
            performAutoRefresh(event.getSlot());
        }, 1);
    }

    protected void playOpenSounds(Player player) {
        if (player != null && !openSounds.isEmpty()) {
            for (String sound : openSounds) {
                SoundUtils.playSound(player, sound);
            }
        }
    }

    protected void playCloseSounds(Player player) {
        if (player != null && !closeSounds.isEmpty()) {
            for (String sound : closeSounds) {
                SoundUtils.playSound(player, sound);
            }
        }
    }

    public void playClickSounds(Player player) {
        if (player != null && !clickSounds.isEmpty()) {
            for (String sound : clickSounds) {
                SoundUtils.playSound(player, sound);
            }
        }
    }
}
