package net.exylia.commons.menu;

import me.clip.placeholderapi.PlaceholderAPI;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.InventoryAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;

/**
 * Sistema de menús optimizado con cache y operaciones asíncronas
 */
public class Menu {

    // Cache estático para inventarios reutilizables
    private static final Map<String, Inventory> INVENTORY_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    // Configuración del menú
    protected Component title;
    protected final int size;
    protected final Map<Integer, MenuItem> items;
    protected String rawTitle;
    protected boolean usePlaceholdersInTitle = false;
    protected Object titlePlaceholderContext = null;

    // Estado del menú
    protected Inventory inventory;
    protected Player viewer;
    protected Consumer<Player> closeHandler;
    protected Menu returnMenu;

    // Configuración de actualizaciones
    protected boolean dynamicUpdates = false;
    protected JavaPlugin plugin;
    protected long updateInterval = 20L;
    protected int globalTaskId = -1;
    protected final Map<Integer, Integer> itemTaskIds = new ConcurrentHashMap<>();

    // Fillers globales
    protected MenuItem globalFiller;
    protected MenuItem borderFiller;

    // Adaptador de inventario
    protected final InventoryAdapter inventoryAdapter = AdapterFactory.getInventoryAdapter();

    public Menu(String title, int rows) {
        this.rawTitle = title;
        this.title = ColorUtils.parse(title);
        this.size = Math.max(9, Math.min(54, rows * 9));
        this.items = new ConcurrentHashMap<>();
    }

    public Menu(Component title, int rows) {
        this.title = title;
        this.size = Math.max(9, Math.min(54, rows * 9));
        this.items = new ConcurrentHashMap<>();
    }

    // ==================== CONFIGURACIÓN BÁSICA ====================

    public Menu setItem(int slot, MenuItem menuItem) {
        if (slot < 0 || slot >= size) return this;

        items.put(slot, menuItem);

        if (inventory != null && viewer != null) {
            updateSingleItem(slot, menuItem);
        }

        return this;
    }

    public Menu setCloseHandler(Consumer<Player> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public Menu setReturnMenu(Menu returnMenu) {
        this.returnMenu = returnMenu;
        return this;
    }

    // ==================== SISTEMA DE FILLERS GLOBAL ====================

    /**
     * Establece un filler global para todos los slots vacíos
     */
    public Menu setGlobalFiller(MenuItem filler) {
        this.globalFiller = filler;
        return this;
    }

    /**
     * Crea un borde alrededor del menú
     */
    public Menu setBorderFiller(MenuItem borderItem) {
        this.borderFiller = borderItem;
        return this;
    }

    /**
     * Aplica fillers usando un sistema de capas
     */
    protected void applyFillers(Player player) {
        // Capa 1: Filler global
        if (globalFiller != null) {
            for (int i = 0; i < size; i++) {
                if (!items.containsKey(i)) {
                    MenuItem filler = globalFiller.clone();
                    if (filler.usesPlaceholders()) {
                        filler.updatePlaceholders(player);
                    }
                    items.put(i, filler);
                }
            }
        }

        // Capa 2: Borde (sobrescribe el filler global en bordes)
        if (borderFiller != null) {
            applyBorder(player);
        }
    }

    private void applyBorder(Player player) {
        int rows = size / 9;

        // Fila superior e inferior
        for (int i = 0; i < 9; i++) {
            setBorderItem(i, player);
            setBorderItem(size - 9 + i, player);
        }

        // Columnas laterales
        for (int i = 1; i < rows - 1; i++) {
            setBorderItem(i * 9, player);
            setBorderItem(i * 9 + 8, player);
        }
    }

    private void setBorderItem(int slot, Player player) {
        MenuItem border = borderFiller.clone();
        if (border.usesPlaceholders()) {
            border.updatePlaceholders(player);
        }
        items.put(slot, border);
    }

    // ==================== SISTEMA DE CACHE ====================

    private String getCacheKey() {
        return String.format("menu_%s_%d_%d",
                rawTitle != null ? rawTitle.hashCode() : title.hashCode(),
                size,
                items.size());
    }

    private Inventory createOrGetCachedInventory() {
        if (!usePlaceholdersInTitle && globalFiller == null) {
            String cacheKey = getCacheKey();
            Inventory cached = INVENTORY_CACHE.get(cacheKey);

            if (cached != null) {
                return cloneInventory(cached);
            }

            Inventory newInventory = inventoryAdapter.createInventory(size, title);

            if (INVENTORY_CACHE.size() < MAX_CACHE_SIZE) {
                INVENTORY_CACHE.put(cacheKey, cloneInventory(newInventory));
            }

            return newInventory;
        }

        return inventoryAdapter.createInventory(size, title);
    }

    private Inventory cloneInventory(Inventory original) {
        Inventory clone = inventoryAdapter.createInventory(original.getSize(), title);
        for (int i = 0; i < original.getSize(); i++) {
            clone.setItem(i, original.getItem(i));
        }
        return clone;
    }


    // ==================== PLACEHOLDERS ====================

    public Menu usePlaceholdersInTitle(boolean use) {
        this.usePlaceholdersInTitle = use;
        return this;
    }

    public Menu setTitlePlaceholderContext(Object context) {
        this.titlePlaceholderContext = context;
        return this;
    }

    public Menu updateTitle(Player player) {
        if (!usePlaceholdersInTitle || rawTitle == null) return this;

        String processedTitle = rawTitle;

        if (titlePlaceholderContext != null) {
            processedTitle = CustomPlaceholderManager.process(processedTitle, titlePlaceholderContext);
        }

        if (isPlaceholderAPIEnabled()) {
            processedTitle = PlaceholderAPI.setPlaceholders(player, processedTitle);
        }

        this.title = ColorUtils.parse(processedTitle);

        if (inventory != null && viewer != null) {
            reopenWithNewTitle(player);
        }

        return this;
    }

    private void reopenWithNewTitle(Player player) {
        Inventory newInventory = inventoryAdapter.createInventory(size, title);

        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) != null) {
                newInventory.setItem(i, inventory.getItem(i));
            }
        }

        inventory = newInventory;
        player.openInventory(inventory);
    }

    // ==================== ACTUALIZACIONES DINÁMICAS ====================

    public Menu enableDynamicUpdates(JavaPlugin plugin, long tickInterval) {
        this.dynamicUpdates = true;
        this.plugin = plugin;
        this.updateInterval = tickInterval;
        return this;
    }

    public Menu disableDynamicUpdates() {
        this.dynamicUpdates = false;
        cancelAllTasks();
        return this;
    }

    private void cancelAllTasks() {
        if (globalTaskId != -1) {
            Bukkit.getScheduler().cancelTask(globalTaskId);
            globalTaskId = -1;
        }

        itemTaskIds.values().forEach(taskId -> {
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        });
        itemTaskIds.clear();
    }

    /**
     * Actualiza todos los items de forma asíncrona
     */
    public CompletableFuture<Void> updateItemsAsync() {
        if (inventory == null || viewer == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            Map<Integer, MenuItem> updatedItems = new HashMap<>();

            for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
                MenuItem item = entry.getValue();
                if (item.usesPlaceholders()) {
                    MenuItem cloned = item.clone();
                    cloned.updatePlaceholders(viewer);
                    updatedItems.put(entry.getKey(), cloned);
                }
            }

            // Actualizar en el hilo principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map.Entry<Integer, MenuItem> entry : updatedItems.entrySet()) {
                    inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
                }
            });
        });
    }

    private void updateSingleItem(int slot, MenuItem item) {
        if (viewer != null && item.usesPlaceholders()) {
            item.updatePlaceholders(viewer);
        }

        inventory.setItem(slot, item.getItemStack());

        if (viewer != null && item.needsDynamicUpdate() && plugin != null) {
            scheduleItemUpdate(slot, item);
        }
    }

    private void scheduleItemUpdate(int slot, MenuItem item) {
        // Cancelar tarea anterior
        Integer oldTaskId = itemTaskIds.get(slot);
        if (oldTaskId != null && oldTaskId != -1) {
            Bukkit.getScheduler().cancelTask(oldTaskId);
        }

        // Programar nueva tarea
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (viewer != null && viewer.isOnline() && inventory != null) {
                item.updatePlaceholders(viewer);
                inventory.setItem(slot, item.getItemStack());
            } else {
                // Cleanup automático
                Integer currentTaskId = itemTaskIds.remove(slot);
                if (currentTaskId != null) {
                    Bukkit.getScheduler().cancelTask(currentTaskId);
                }
            }
        }, item.getUpdateInterval(), item.getUpdateInterval());

        itemTaskIds.put(slot, taskId);
    }

    // ==================== APERTURA Y CIERRE ====================

    public void open(Player player) {
        this.viewer = player;

        // Actualizar título si usa placeholders
        if (usePlaceholdersInTitle) {
            updateTitle(player);
        }

        // Aplicar fillers antes de crear el inventario
        applyFillers(player);

        // Crear o reutilizar inventario
        if (inventory == null) {
            inventory = createOrGetCachedInventory();
        }

        // Poblar inventario
        populateInventory(player);

        // Abrir y registrar
        player.openInventory(inventory);
        MenuManager.registerOpenMenu(player, this);

        // Iniciar actualizaciones si están habilitadas
        startDynamicUpdates();
    }

    private void populateInventory(Player player) {
        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            MenuItem item = entry.getValue();

            if (item.usesPlaceholders()) {
                item.updatePlaceholders(player);
            }

            inventory.setItem(entry.getKey(), item.getItemStack());

            if (item.needsDynamicUpdate() && plugin != null) {
                scheduleItemUpdate(entry.getKey(), item);
            }
        }
    }

    private void startDynamicUpdates() {
        if (dynamicUpdates && plugin != null && globalTaskId == -1) {
            globalTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    plugin,
                    () -> updateItemsAsync(),
                    updateInterval,
                    updateInterval
            );
        }
    }

    void onClose(Player player) {
        cancelAllTasks();
        this.viewer = null;

        if (closeHandler != null) {
            closeHandler.accept(player);
        }
    }

    // ==================== GETTERS ====================

    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    public Map<Integer, MenuItem> getItems() {
        return new HashMap<>(items);
    }

    public Consumer<Player> getCloseHandler() {
        return closeHandler;
    }

    public Menu getReturnMenu() {
        return returnMenu;
    }

    public Player getViewer() {
        return viewer;
    }

    // ==================== UTILIDADES ====================

    /**
     * Limpia el cache de inventarios
     */
    public static void clearCache() {
        INVENTORY_CACHE.clear();
    }

    /**
     * Obtiene estadísticas del cache
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", INVENTORY_CACHE.size());
        stats.put("maxSize", MAX_CACHE_SIZE);
        stats.put("hitRate", "N/A"); // Podrías implementar un contador si lo necesitas
        return stats;
    }

    /**
     * Actualiza un item específico en tiempo real sin reabrir el menú
     * @param slot Slot del item a actualizar
     * @param updatedItem Nuevo item actualizado
     */
    public void updateItemInPlace(int slot, MenuItem updatedItem) {
        if (slot < 0 || slot >= size || inventory == null) {
            return;
        }

        Player currentPlayer = viewer;
        if (currentPlayer == null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getOpenInventory().getTopInventory() == inventory) {
                    currentPlayer = online;
                    break;
                }
            }
        }

        // Actualizar en el mapa de items
        items.put(slot, updatedItem);

        // Procesar placeholders si es necesario
        if (updatedItem.usesPlaceholders() && currentPlayer != null) {
            updatedItem.updatePlaceholders(currentPlayer);
        }

        // Actualizar inmediatamente en el inventario visible
        inventory.setItem(slot, updatedItem.getItemStack());

        // Si el item necesita actualizaciones dinámicas, reprogramar
        if (updatedItem.needsDynamicUpdate() && plugin != null) {
            scheduleItemUpdate(slot, updatedItem);
        }
    }

    /**
     * Actualiza un item usando un builder/factory function
     * @param slot Slot del item
     * @param itemBuilder Function que recibe el item actual y retorna el actualizado
     */
    public void updateItemInPlace(int slot, Function<MenuItem, MenuItem> itemBuilder) {
        MenuItem currentItem = items.get(slot);
        if (currentItem != null) {
            MenuItem updatedItem = itemBuilder.apply(currentItem);
            updateItemInPlace(slot, updatedItem);
        }
    }

    /**
     * Actualiza múltiples items de forma eficiente
     * @param updates Mapa de slot -> item actualizado
     */
    public void updateItemsInPlace(Map<Integer, MenuItem> updates) {
        if (inventory == null || viewer == null) return;

        updates.forEach((slot, item) -> {
            if (slot >= 0 && slot < size) {
                items.put(slot, item);

                if (item.usesPlaceholders()) {
                    item.updatePlaceholders(viewer);
                }

                inventory.setItem(slot, item.getItemStack());

                if (item.needsDynamicUpdate() && plugin != null) {
                    scheduleItemUpdate(slot, item);
                }
            }
        });
    }

    public void setViewer(Player viewer) {
        this.viewer = viewer;
    }
}