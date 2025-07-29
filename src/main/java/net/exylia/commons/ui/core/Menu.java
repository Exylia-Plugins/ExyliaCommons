// ==================== CORE MENU SYSTEM - ENHANCED ====================

package net.exylia.commons.ui.core;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.manager.MenuManager;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.versions.InventoryAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Base class for all menus in the UI system v2
 * Provides core functionality for menu creation, management, and interaction
 * Now with intelligent auto-refresh after clicks
 */
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

    // Menu state
    @Getter
    protected Inventory inventory;
    @Getter
    protected Player viewer;
    @Getter
    protected ExyliaContext context = ExyliaContext.create();
    @Getter
    protected boolean isOpen = false;

    // Menu configuration
    protected final Map<Integer, MenuItem> items = new ConcurrentHashMap<>();
    protected Consumer<MenuClickEvent> globalClickHandler;
    protected Consumer<Player> closeHandler;
    @Getter
    protected Menu parentMenu;

    // Filler items
    protected MenuItem globalFiller;
    protected MenuItem borderFiller;

    // Update system
    protected boolean dynamicUpdates = false;
    protected JavaPlugin plugin;
    protected long updateInterval = 20L;
    protected int updateTaskId = -1;

    // ✅ AUTO-REFRESH CONFIGURATION
    @Getter
    protected boolean autoRefreshOnClick = true; // Default habilitado
    @Getter
    protected RefreshMode refreshMode = RefreshMode.SMART; // Modo por defecto

    // Inventory adapter
    protected static final InventoryAdapter inventoryAdapter = AdapterFactory.getInventoryAdapter();

    /**
     * Modos de refresh después de clicks
     */
    public enum RefreshMode {
        DISABLED,    // Sin auto-refresh
        FULL,        // Refresh completo del menú
        SMART,       // Solo actualiza items que lo necesiten
        SLOT_ONLY    // Solo actualiza el slot clickeado
    }

    public Menu(String title, int rows) {
        this(UUID.randomUUID().toString(), title, rows);
    }

    public Menu(String id, String title, int rows) {
        this.id = id;
        this.rawTitle = title;
        this.title = ColorUtils.parse(title);
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
    }

    public Menu(String title, int rows, ExyliaContext context) {
        this(title, rows);
        this.context = context != null ? context : ExyliaContext.create();
    }

    // ==================== AUTO-REFRESH CONFIGURATION ====================

    /**
     * Configura el auto-refresh después de clicks
     * @param enabled Si debe refrescar automáticamente
     * @return This menu for chaining
     */
    public Menu setAutoRefreshOnClick(boolean enabled) {
        this.autoRefreshOnClick = enabled;
        return this;
    }

    /**
     * Configura el modo de refresh
     * @param mode El modo de refresh
     * @return This menu for chaining
     */
    public Menu setRefreshMode(RefreshMode mode) {
        this.refreshMode = mode != null ? mode : RefreshMode.SMART;
        return this;
    }

    /**
     * Habilita auto-refresh inteligente (modo por defecto)
     * @return This menu for chaining
     */
    public Menu enableSmartRefresh() {
        this.autoRefreshOnClick = true;
        this.refreshMode = RefreshMode.SMART;
        return this;
    }

    /**
     * Habilita auto-refresh completo
     * @return This menu for chaining
     */
    public Menu enableFullRefresh() {
        this.autoRefreshOnClick = true;
        this.refreshMode = RefreshMode.FULL;
        return this;
    }

    /**
     * Deshabilita auto-refresh
     * @return This menu for chaining
     */
    public Menu disableAutoRefresh() {
        this.autoRefreshOnClick = false;
        this.refreshMode = RefreshMode.DISABLED;
        return this;
    }

    // ==================== CORE FUNCTIONALITY ====================

    /**
     * Opens the menu for a player
     * @param player The player to open the menu for
     */
    public void open(Player player) {
        open(player, null);
    }

    /**
     * Opens the menu for a player with context
     * @param player The player to open the menu for
     * @param additionalContext The menu context
     */
    public void open(Player player, ExyliaContext additionalContext) {
        this.viewer = player;
        if (additionalContext != null) {
            this.context = this.context.copy().merge(additionalContext);
        }

        // Añadir el jugador al contexto automáticamente
        this.context.withPlayer(player);

        // Process title with context if available
        processTitle();

        // Create inventory
        createInventory();

        // Apply fillers
        applyFillers();

        // Populate inventory
        populateInventory();

        // Open for player
        player.openInventory(inventory);
        this.isOpen = true;

        // Register with manager
        MenuManager.registerMenu(player, this);

        // Start updates if enabled
        startUpdates();
    }

    /**
     * Closes the menu
     */
    public void close() {
        if (viewer != null && isOpen) {
            viewer.closeInventory();
        }
    }

    /**
     * Handles menu close event
     */
    protected void onClose() {
        stopUpdates();

        // Guardar referencia del player ANTES de establecerlo como null
        Player currentPlayer = this.viewer;

        // Llamar el handler de cierre CON el player aún disponible
        if (closeHandler != null && currentPlayer != null) {
            closeHandler.accept(currentPlayer);
        }

        // AHORA sí limpiar las referencias
        this.isOpen = false;
        this.viewer = null;
    }

    public void handleClose() {
        onClose();
    }

    // ==================== ITEM MANAGEMENT ====================

    /**
     * Sets an item at a specific slot
     * @param slot The slot to set the item at
     * @param item The item to set
     * @return This menu for chaining
     */
    public Menu setItem(int slot, MenuItem item) {
        if (isValidSlot(slot)) {
            items.put(slot, item);

            if (inventory != null && isOpen) {
                updateSlot(slot);
            }
        }
        return this;
    }

    /**
     * Gets an item at a specific slot
     * @param slot The slot to get the item from
     * @return The item at the slot, or null if no item exists
     */
    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    /**
     * Removes an item from a slot
     * @param slot The slot to remove the item from
     * @return This menu for chaining
     */
    public Menu removeItem(int slot) {
        items.remove(slot);
        if (inventory != null && isOpen) {
            updateSlot(slot);
        }
        return this;
    }

    /**
     * Clears all items from the menu
     * @return This menu for chaining
     */
    public Menu clearItems() {
        items.clear();
        if (inventory != null && isOpen) {
            populateInventory();
        }
        return this;
    }

    /**
     * Actualiza un slot específico en el inventario
     */
    protected void updateSlot(int slot) {
        if (inventory == null || !isValidSlot(slot)) return;

        MenuItem item = getEffectiveItem(slot);
        if (item != null) {
            // Preparar contexto completo para el item
            ExyliaContext itemContext = prepareItemContext(item);

            // Aplicar contexto al item
            item.withContext(itemContext);

            // Procesar y establecer en inventario
            inventory.setItem(slot, item.buildProcessed(viewer));
        } else {
            inventory.setItem(slot, null);
        }
    }

    /**
     * Prepara el contexto completo para un item específico
     */
    protected ExyliaContext prepareItemContext(MenuItem item) {
        // Crear contexto que combina el contexto del menú con el del item
        ExyliaContext combinedContext = this.context.createChild();

        // Fusionar con el contexto específico del item
        if (item.getContext() != null && !item.getContext().isEmpty()) {
            combinedContext.merge(item.getContext());
        }

        return combinedContext;
    }

    /**
     * Gets the effective item for a slot (considering fillers)
     * @param slot The slot to get the effective item for
     * @return The effective item
     */
    protected MenuItem getEffectiveItem(int slot) {
        MenuItem item = items.get(slot);
        if (item != null) {
            return item;
        }

        // Apply border filler if this is a border slot
        if (borderFiller != null && isBorderSlot(slot)) {
            return borderFiller.clone();
        }

        // Apply global filler
        if (globalFiller != null) {
            return globalFiller.clone();
        }

        return null;
    }

    // ==================== AUTO-REFRESH METHODS ====================

    /**
     * Ejecuta el refresh automático después de un click
     * @param clickedSlot El slot que fue clickeado
     */
    protected void performAutoRefresh(int clickedSlot) {
        if (!autoRefreshOnClick || !isOpen || viewer == null) {
            return;
        }

        switch (refreshMode) {
            case DISABLED -> {
                // No hacer nada
            }
            case SLOT_ONLY -> refreshSlotOnly(clickedSlot);
            case SMART -> performSmartRefresh(clickedSlot);
            case FULL -> performFullRefresh();
        }
    }

    /**
     * Actualiza solo el slot clickeado
     */
    private void refreshSlotOnly(int slot) {
        updateSlot(slot);
    }

    /**
     * Actualiza inteligentemente solo los items que lo necesiten
     */
    private void performSmartRefresh(int clickedSlot) {
        // Siempre actualizar el slot clickeado
        updateSlot(clickedSlot);

        // Buscar otros items que necesiten actualización
        Set<Integer> slotsToUpdate = new HashSet<>();

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            MenuItem item = entry.getValue();

            if (slot != clickedSlot && shouldItemBeRefreshed(item)) {
                slotsToUpdate.add(slot);
            }
        }

        // Actualizar slots identificados
        for (int slot : slotsToUpdate) {
            updateSlot(slot);
        }

        // Si hay muchos items para actualizar, mejor hacer refresh completo
        if (slotsToUpdate.size() > size / 2) {
            performFullRefresh();
        }
    }

    /**
     * Determina si un item necesita ser refrescado
     */
    private boolean shouldItemBeRefreshed(MenuItem item) {
        if (item == null) return false;

        // Items con actualización dinámica
        if (item.needsDynamicUpdate()) {
            return true;
        }

        // Items con lore dinámico
        if (item.hasDynamicLore()) {
            return true;
        }

        // Items con placeholders en nombre, lore o cantidad
        if (hasPlaceholders(item.getRawName()) ||
                hasPlaceholders(item.getRawAmount()) ||
                hasPlaceholdersInLore(item.getRawLore())) {
            return true;
        }

        return false;
    }

    /**
     * Verifica si un string contiene placeholders
     */
    private boolean hasPlaceholders(String text) {
        return text != null && (text.contains("{") || text.contains("%"));
    }

    /**
     * Verifica si el lore contiene placeholders
     */
    private boolean hasPlaceholdersInLore(List<String> lore) {
        if (lore == null) return false;

        return lore.stream().anyMatch(this::hasPlaceholders);
    }

    /**
     * Realiza un refresh completo del menú
     */
    private void performFullRefresh() {
        // Actualizar título si es necesario
        processTitle();

        // Repoblar inventario completo
        populateInventory();
    }

    // ==================== CONFIGURATION ====================

    /**
     * Sets the global filler item
     * @param filler The filler item
     * @return This menu for chaining
     */
    public Menu setGlobalFiller(MenuItem filler) {
        this.globalFiller = filler;
        if (inventory != null && isOpen) {
            populateInventory();
        }
        return this;
    }

    /**
     * Sets the border filler item
     * @param filler The border filler item
     * @return This menu for chaining
     */
    public Menu setBorderFiller(MenuItem filler) {
        this.borderFiller = filler;
        if (inventory != null && isOpen) {
            populateInventory();
        }
        return this;
    }

    /**
     * Sets a global click handler for all items
     * @param handler The click handler
     * @return This menu for chaining
     */
    public Menu setGlobalClickHandler(Consumer<MenuClickEvent> handler) {
        this.globalClickHandler = handler;
        return this;
    }

    /**
     * Sets the close handler
     * @param handler The close handler
     * @return This menu for chaining
     */
    public Menu setCloseHandler(Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    /**
     * Sets the parent menu to return to
     * @param menu The parent menu
     * @return This menu for chaining
     */
    public Menu setParentMenu(Menu menu) {
        this.parentMenu = menu;
        return this;
    }

    // ==================== DYNAMIC UPDATES ====================

    /**
     * Enables dynamic updates for the menu
     * @param plugin The plugin instance
     * @param interval The update interval in ticks
     * @return This menu for chaining
     */
    public Menu enableDynamicUpdates(JavaPlugin plugin, long interval) {
        this.plugin = plugin;
        this.updateInterval = interval;
        this.dynamicUpdates = true;

        if (isOpen) {
            startUpdates();
        }
        return this;
    }

    /**
     * Disables dynamic updates
     * @return This menu for chaining
     */
    public Menu disableDynamicUpdates() {
        this.dynamicUpdates = false;
        stopUpdates();
        return this;
    }

    /**
     * Starts the update task
     */
    protected void startUpdates() {
        if (!dynamicUpdates || plugin == null || updateTaskId != -1) return;

        updateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::updateDynamicItems,
                updateInterval,
                updateInterval
        );
    }

    /**
     * Stops the update task
     */
    protected void stopUpdates() {
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

    /**
     * Updates items that need dynamic updates
     */
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

    // ==================== INTERNAL METHODS ====================

    /**
     * Processes the title with context
     */
    protected void processTitle() {
        if (rawTitle != null && context != null) {
            String processed = context.processPlaceholders(rawTitle, viewer);
            this.title = ColorUtils.parse(processed);
        }
    }

    /**
     * Creates the inventory
     */
    protected void createInventory() {
        this.inventory = inventoryAdapter.createInventory(size, title);
    }

    /**
     * Applies filler items
     */
    protected void applyFillers() {
        // Global filler is applied in getEffectiveItem()
        // Border filler is applied in getEffectiveItem()
    }

    /**
     * Populates the inventory with items
     */
    protected void populateInventory() {
        if (inventory == null) return;

        for (int i = 0; i < size; i++) {
            updateSlot(i);
        }
    }

    /**
     * Checks if a slot is valid
     * @param slot The slot to check
     * @return True if the slot is valid
     */
    protected boolean isValidSlot(int slot) {
        return slot >= 0 && slot < size;
    }

    /**
     * Checks if a slot is a border slot
     * @param slot The slot to check
     * @return True if the slot is a border slot
     */
    protected boolean isBorderSlot(int slot) {
        if (rows <= 2) return false;

        int row = slot / 9;
        int col = slot % 9;

        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    // ==================== GETTERS ====================

    public Map<Integer, MenuItem> getItems() { return new HashMap<>(items); }

    // ==================== EVENT HANDLING ====================

    /**
     * Handles a click event on this menu
     * @param event The click event
     */
    public void handleClick(MenuClickEvent event) {
        // Handle global click handler first
        if (globalClickHandler != null) {
            globalClickHandler.accept(event);
            if (event.isCancelled()) {
                return;
            }
        }

        // Handle item-specific click
        MenuItem item = getItem(event.getSlot());
        if (item != null) {
            item.handleClick(event);
        }

        // ✅ NUEVO: Auto-refresh después del click
        performAutoRefresh(event.getSlot());
    }
}