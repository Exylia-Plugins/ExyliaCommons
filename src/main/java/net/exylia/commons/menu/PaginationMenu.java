package net.exylia.commons.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;

public class PaginationMenu extends Menu {

    // Datos de paginación
    private final List<MenuItem> paginationItems;
    private final int[] itemSlots;
    private final int maxItemsPerPage;

    // Estado por jugador
    private final Map<Player, Integer> playerCurrentPage = new ConcurrentHashMap<>();
    private final Map<Player, Map<Integer, Integer>> playerItemTasks = new ConcurrentHashMap<>();

    // Botones de navegación
    private MenuItem previousPageButton;
    private MenuItem nextPageButton;
    private int previousPageButtonSlot;
    private int nextPageButtonSlot;

    // Filler específico para slots de items
    private MenuItem itemSlotsFillerItem;
    private BiConsumer<Menu, Integer> menuCustomizer;
    private Consumer<Player> externalCloseHandler;

    public PaginationMenu(String baseTitle, int rows, int... itemSlots) {
        super(baseTitle, rows);

        this.paginationItems = new ArrayList<>();
        this.itemSlots = itemSlots;
        this.maxItemsPerPage = itemSlots.length;

        initializeDefaultButtons(rows);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    private void initializeDefaultButtons(int rows) {
        this.previousPageButton = new MenuItem("headbase-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGExZDU1YjNmOTg5NDEwYTM0NzUyNjUwZTI0OGM5YjZjMTc4M2E3ZWMyYWEzZmQ3Nzg3YmRjNGQwZTYzN2QzOSJ9fX0=")
                .setName("<#8fffc1>⏪ <#8fffc1>Anterior")
                .setLore("&8⏵ <#e7cfff>Para ir a la página anterior.")
                .hideAllAttributes();

        this.nextPageButton = new MenuItem("headurl-http://textures.minecraft.net/texture/fa87e3d96e1cfeb9ccfb3ba53a217faf5249e285533b271a2fb284c30dbd9829")
                .setName("<#8fffc1>Siguiente <#a1ffc3>⏩")
                .setLore("&8⏵ <#e7cfff>Para ir a la siguiente página.")
                .hideAllAttributes();

        this.previousPageButtonSlot = rows * 9 - 9;
        this.nextPageButtonSlot = rows * 9 - 1;
    }

    // ==================== CONFIGURACIÓN ====================

    public PaginationMenu addItem(MenuItem item) {
        paginationItems.add(item);
        return this;
    }

    public PaginationMenu addItems(List<MenuItem> items) {
        this.paginationItems.addAll(items);
        return this;
    }

    public PaginationMenu setPreviousPageButton(MenuItem item, int slot) {
        this.previousPageButton = item;
        this.previousPageButtonSlot = slot;
        return this;
    }

    public PaginationMenu setNextPageButton(MenuItem item, int slot) {
        this.nextPageButton = item;
        this.nextPageButtonSlot = slot;
        return this;
    }

    public PaginationMenu setItemSlotsFillerItem(MenuItem item) {
        this.itemSlotsFillerItem = item;
        return this;
    }

    public PaginationMenu setMenuCustomizer(BiConsumer<Menu, Integer> customizer) {
        this.menuCustomizer = customizer;
        return this;
    }

    // ==================== APERTURA Y NAVEGACIÓN ====================

    @Override
    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
        cleanupPlayerResources(player);

        int maxPages = getTotalPages();
        page = Math.max(1, Math.min(page, maxPages));
        playerCurrentPage.put(player, page);

        updateMenuTitle(player, page, maxPages);
        buildPage(player, page, maxPages);

        if (menuCustomizer != null) {
            menuCustomizer.accept(this, page);
        }

        super.open(player);

        if (super.dynamicUpdates && super.plugin != null) {
            scheduleItemUpdatesAsync(player);
        }
    }

    private void updateMenuTitle(Player player, int page, int maxPages) {
        if (super.rawTitle == null) return;

        String newTitle = super.rawTitle
                .replace("%page%", String.valueOf(page))
                .replace("%pages%", String.valueOf(maxPages));

        if (super.usePlaceholdersInTitle) {
            if (super.titlePlaceholderContext != null) {
                newTitle = CustomPlaceholderManager.process(newTitle, super.titlePlaceholderContext);
            }

            if (isPlaceholderAPIEnabled()) {
                newTitle = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, newTitle);
            }
        }

        super.title = net.exylia.commons.utils.ColorUtils.parse(newTitle);
    }

    private void buildPage(Player player, int page, int maxPages) {
        // Limpiar items de paginación anteriores
        clearPaginationSlots();

        // Aplicar fillers (heredado de Menu)
        super.applyFillers(player);

        // Aplicar filler específico para slots de items
        applyItemSlotsFiller(player, page);

        // Agregar items de la página actual
        addPageItems(player, page);

        // Agregar botones de navegación
        addNavigationButtons(player, page, maxPages);
    }

    private void clearPaginationSlots() {
        for (int slot : itemSlots) {
            super.items.remove(slot);
        }
        super.items.remove(previousPageButtonSlot);
        super.items.remove(nextPageButtonSlot);
    }

    private void applyItemSlotsFiller(Player player, int page) {
        if (itemSlotsFillerItem == null) return;

        int itemsInPage = getItemsInPage(page);
        for (int i = itemsInPage; i < itemSlots.length; i++) {
            int slot = itemSlots[i];
            MenuItem filler = itemSlotsFillerItem.clone();
            if (filler.usesPlaceholders()) {
                filler.updatePlaceholders(player);
            }
            super.items.put(slot, filler);
        }
    }

    private void addPageItems(Player player, int page) {
        int start = (page - 1) * maxItemsPerPage;
        int end = Math.min(start + maxItemsPerPage, paginationItems.size());
        for (int i = start, slotIndex = 0; i < end; i++, slotIndex++) {
            if (slotIndex < itemSlots.length) {
                MenuItem originalItem = paginationItems.get(i);
                MenuItem item = originalItem.clone();
                if (item.usesPlaceholders()) {
                    item.updatePlaceholders(player);
                }
                super.items.put(itemSlots[slotIndex], item);
            }
        }
    }

    private void addNavigationButtons(Player player, int page, int maxPages) {
        if (page > 1) {
            MenuItem prevButton = previousPageButton.clone();
            if (prevButton.usesPlaceholders()) {
                prevButton.updatePlaceholders(player);
            }
            prevButton.setClickHandler(info -> {
                open(info.player(), page - 1);
            });
            super.items.put(previousPageButtonSlot, prevButton);
        }

        if (page < maxPages) {
            MenuItem nextButton = nextPageButton.clone();
            if (nextButton.usesPlaceholders()) {
                nextButton.updatePlaceholders(player);
            }
            nextButton.setClickHandler(info -> {
                open(info.player(), page + 1);
            });
            super.items.put(nextPageButtonSlot, nextButton);
        }
    }

    // ==================== ACTUALIZACIONES ASÍNCRONAS ====================

    private CompletableFuture<Void> scheduleItemUpdatesAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            if (!playerItemTasks.containsKey(player)) {
                playerItemTasks.put(player, new ConcurrentHashMap<>());
            }

            Map<Integer, Integer> itemTasks = playerItemTasks.get(player);

            for (int slot : itemSlots) {
                MenuItem item = super.getItem(slot);
                if (item != null && item.needsDynamicUpdate() && item.usesPlaceholders()) {
                    scheduleItemUpdate(player, slot, item, itemTasks);
                }
            }
        });
    }

    private void scheduleItemUpdate(Player player, int slot, MenuItem item, Map<Integer, Integer> itemTasks) {
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(super.plugin, () -> {
            if (player.isOnline() && super.getViewer() == player) {
                item.updatePlaceholders(player);
                super.setItem(slot, item);
            } else {
                Integer existingTaskId = itemTasks.remove(slot);
                if (existingTaskId != null) {
                    Bukkit.getScheduler().cancelTask(existingTaskId);
                }
            }
        }, item.getUpdateInterval(), item.getUpdateInterval());

        itemTasks.put(slot, taskId);
    }

    /**
     * Actualiza un item de paginación en tiempo real
     * @param itemIndex Índice en la lista de paginationItems
     * @param updatedItem Item actualizado
     */
    public void updatePaginationItemInPlace(int itemIndex, MenuItem updatedItem) {
        if (itemIndex < 0 || itemIndex >= paginationItems.size()) return;

        paginationItems.set(itemIndex, updatedItem);

        Player currentViewer = getViewer();
        if (currentViewer != null) {
            int currentPage = getCurrentPage(currentViewer);
            int startIndex = (currentPage - 1) * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, paginationItems.size());

            if (itemIndex >= startIndex && itemIndex < endIndex) {
                int slotIndex = itemIndex - startIndex;
                if (slotIndex < itemSlots.length) {
                    int slot = itemSlots[slotIndex];
                    updateItemInPlace(slot, updatedItem);
                }
            }
        }
    }

    public void updateCurrentPageItemInPlace(int slot, MenuItem updatedItem) {
        Player currentViewer = getViewer();
        if (currentViewer == null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getOpenInventory().getTopInventory() == super.inventory) {
                    currentViewer = online;
                    super.setViewer(currentViewer);
                    break;
                }
            }
        }

        if (currentViewer != null) {
            int currentPage = getCurrentPage(currentViewer);
            int startIndex = (currentPage - 1) * maxItemsPerPage;

            // Encontrar qué slot del itemSlots array corresponde al slot dado
            int slotIndex = -1;
            for (int i = 0; i < itemSlots.length; i++) {
                if (itemSlots[i] == slot) {
                    slotIndex = i;
                    break;
                }
            }

            if (slotIndex != -1) {
                int globalIndex = startIndex + slotIndex;
                if (globalIndex >= 0 && globalIndex < paginationItems.size()) {
                    updatePaginationItemInPlace(globalIndex, updatedItem);
                    return;
                }
            }
        }
        updateItemInPlace(slot, updatedItem);
    }

    /**
     * Actualiza un item de paginación usando su función click handler para preservar la funcionalidad
     */
    public void updatePaginationItemInPlace(int itemIndex, Function<MenuItem, MenuItem> itemBuilder) {
        if (itemIndex >= 0 && itemIndex < paginationItems.size()) {
            MenuItem currentItem = paginationItems.get(itemIndex);
            MenuItem updatedItem = itemBuilder.apply(currentItem);
            updatePaginationItemInPlace(itemIndex, updatedItem);
        }
    }

    // ==================== LIMPIEZA Y CIERRE ====================

    private void onPlayerCloseMenu(Player player) {
        cleanupPlayerResources(player);
        if (externalCloseHandler != null) {
            externalCloseHandler.accept(player);
        }
    }

    private void cleanupPlayerResources(Player player) {
        playerCurrentPage.remove(player);

        Map<Integer, Integer> itemTasks = playerItemTasks.remove(player);
        if (itemTasks != null) {
            itemTasks.values().forEach(taskId -> {
                if (taskId != null && taskId != -1) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            });
        }
    }

    // ==================== UTILIDADES ====================

    private int getItemsInPage(int page) {
        int start = (page - 1) * maxItemsPerPage;
        int end = Math.min(start + maxItemsPerPage, paginationItems.size());
        return end - start;
    }

    public int getCurrentPage(Player player) {
        return playerCurrentPage.getOrDefault(player, 1);
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) paginationItems.size() / maxItemsPerPage));
    }

    public List<MenuItem> getPaginationItems() {
        return new ArrayList<>(paginationItems);
    }

    public PaginationMenu clearPaginationItems() {
        paginationItems.clear();
        return this;
    }

    public MenuItem getItemSlotsFillerItem() {
        return itemSlotsFillerItem;
    }

    // ==================== CLONACIÓN ====================

    public PaginationMenu clone() {
        PaginationMenu cloned = new PaginationMenu(
                super.rawTitle != null ? super.rawTitle : "Cloned Menu",
                super.size / 9,
                itemSlots
        );

        // Copiar items de paginación
        paginationItems.forEach(item -> cloned.addItem(item.clone()));

        // Copiar configuración
        if (previousPageButton != null) {
            cloned.setPreviousPageButton(previousPageButton.clone(), previousPageButtonSlot);
        }
        if (nextPageButton != null) {
            cloned.setNextPageButton(nextPageButton.clone(), nextPageButtonSlot);
        }
        if (itemSlotsFillerItem != null) {
            cloned.setItemSlotsFillerItem(itemSlotsFillerItem.clone());
        }
        if (super.globalFiller != null) {
            cloned.setGlobalFiller(super.globalFiller.clone());
        }
        if (menuCustomizer != null) {
            cloned.setMenuCustomizer(menuCustomizer);
        }

        // Copiar configuración heredada
        cloned.usePlaceholdersInTitle(super.usePlaceholdersInTitle);
        if (super.dynamicUpdates) {
            cloned.enableDynamicUpdates(super.plugin, super.updateInterval);
        }

        return cloned;
    }

    // ==================== OVERRIDE MÉTODOS DE ENCADENAMIENTO ====================

    @Override
    public PaginationMenu setCloseHandler(Consumer<Player> closeHandler) {
        this.externalCloseHandler = closeHandler;
        super.setCloseHandler(this::onPlayerCloseMenu);
        return this;
    }

    @Override
    public PaginationMenu setReturnMenu(Menu returnMenu) {
        super.setReturnMenu(returnMenu);
        return this;
    }

    @Override
    public PaginationMenu enableDynamicUpdates(JavaPlugin plugin, long tickInterval) {
        super.enableDynamicUpdates(plugin, tickInterval);
        return this;
    }

    @Override
    public PaginationMenu disableDynamicUpdates() {
        super.disableDynamicUpdates();

        // Limpiar tareas específicas de paginación
        playerItemTasks.values().forEach(itemTasks ->
                itemTasks.values().forEach(taskId -> {
                    if (taskId != null && taskId != -1) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                })
        );
        playerItemTasks.clear();

        return this;
    }

    @Override
    public PaginationMenu setGlobalFiller(MenuItem filler) {
        super.setGlobalFiller(filler);
        return this;
    }

    @Override
    public PaginationMenu setBorderFiller(MenuItem borderItem) {
        super.setBorderFiller(borderItem);
        return this;
    }
}