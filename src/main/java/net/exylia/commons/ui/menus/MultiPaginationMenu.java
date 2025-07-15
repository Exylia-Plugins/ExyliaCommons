// ==================== MULTI PAGINATION MENU V2 - ENHANCED ====================

package net.exylia.commons.ui.menus;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Advanced multi-section pagination menu with optimized performance and filtering support
 * Each section can have independent pagination, navigation, items, and filtering
 */
public class MultiPaginationMenu extends Menu {

    // Section management
    private final Map<String, PaginationSection> sections = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerPages = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Map<Integer, Integer>>> playerItemTasks = new ConcurrentHashMap<>();

    // Event handlers
    private BiConsumer<String, Integer> onSectionUpdate;
    private Consumer<Player> externalCloseHandler;

    public MultiPaginationMenu(String title, int rows) {
        super(title, rows);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    public MultiPaginationMenu(String title, int rows, ExyliaContext context) {
        super(title, rows, context);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    // ==================== PAGINATION SECTION CLASS ====================

    /**
     * Independent pagination section with its own items, navigation, settings, and filtering
     */
    public static class PaginationSection {
        private final String name;
        private final List<MenuItem> allItems = new ArrayList<>(); // Todos los items
        private final List<MenuItem> filteredItems = new ArrayList<>(); // Items después del filtro
        private final int[] slots;
        private final int itemsPerPage;

        // Navigation configuration
        private MenuItem previousButton;
        private MenuItem nextButton;
        private int previousButtonSlot = -1;
        private int nextButtonSlot = -1;

        // Section configuration
        private MenuItem fillerItem;
        private MenuItem selectedItemTemplate;
        private BiConsumer<MenuClickEvent, Integer> onItemSelect;
        private Consumer<Integer> onPageChange;
        private Integer selectedIndex = null;

        // Filtering support
        private Predicate<MenuItem> currentFilter = null;
        private boolean filterDirty = true;

        public PaginationSection(String name, int... slots) {
            this.name = name;
            this.slots = slots.clone();
            this.itemsPerPage = slots.length;
        }

        // ==================== ITEM MANAGEMENT ====================

        /**
         * Adds an item to this section
         * @param item The item to add
         * @return This section for chaining
         */
        public PaginationSection addItem(MenuItem item) {
            allItems.add(item);
            filterDirty = true;
            return this;
        }

        /**
         * Adds multiple items to this section
         * @param items The items to add
         * @return This section for chaining
         */
        public PaginationSection addItems(Collection<MenuItem> items) {
            this.allItems.addAll(items);
            filterDirty = true;
            return this;
        }

        /**
         * Sets all items for this section
         * @param newItems The items to set
         * @return This section for chaining
         */
        public PaginationSection setItems(Collection<MenuItem> newItems) {
            allItems.clear();
            allItems.addAll(newItems);
            selectedIndex = null;
            filterDirty = true;
            return this;
        }

        /**
         * Clears all items from this section
         * @return This section for chaining
         */
        public PaginationSection clearItems() {
            allItems.clear();
            filteredItems.clear();
            selectedIndex = null;
            filterDirty = false;
            return this;
        }

        /**
         * Updates an item at a specific index in the original list
         * @param index The index to update
         * @param item The new item
         * @return This section for chaining
         */
        public PaginationSection updateItem(int index, MenuItem item) {
            if (index >= 0 && index < allItems.size()) {
                allItems.set(index, item);
                filterDirty = true;
            }
            return this;
        }

        /**
         * Removes an item at a specific index from the original list
         * @param index The index to remove
         * @return This section for chaining
         */
        public PaginationSection removeItem(int index) {
            if (index >= 0 && index < allItems.size()) {
                allItems.remove(index);
                adjustSelectionAfterRemoval(index);
                filterDirty = true;
            }
            return this;
        }

        // ==================== FILTERING ====================

        /**
         * Sets a filter for this section
         * @param filter The filter predicate (null to remove filter)
         * @return This section for chaining
         */
        public PaginationSection setFilter(Predicate<MenuItem> filter) {
            this.currentFilter = filter;
            this.filterDirty = true;
            this.selectedIndex = null; // Clear selection when filter changes
            return this;
        }

        /**
         * Removes the current filter
         * @return This section for chaining
         */
        public PaginationSection clearFilter() {
            return setFilter(null);
        }

        /**
         * Applies the current filter to update the filtered items list
         */
        private void applyFilter() {
            if (!filterDirty) return;

            filteredItems.clear();
            if (currentFilter == null) {
                filteredItems.addAll(allItems);
            } else {
                allItems.stream()
                        .filter(currentFilter)
                        .forEach(filteredItems::add);
            }
            filterDirty = false;
        }

        /**
         * Gets the currently visible items (after filtering)
         * @return List of filtered items
         */
        public List<MenuItem> getVisibleItems() {
            applyFilter();
            return new ArrayList<>(filteredItems);
        }

        // ==================== NAVIGATION CONFIGURATION ====================

        public PaginationSection setPreviousButton(MenuItem button, int slot) {
            this.previousButton = button;
            this.previousButtonSlot = slot;
            return this;
        }

        public PaginationSection setNextButton(MenuItem button, int slot) {
            this.nextButton = button;
            this.nextButtonSlot = slot;
            return this;
        }

        public PaginationSection setFillerItem(MenuItem filler) {
            this.fillerItem = filler;
            return this;
        }

        public PaginationSection setSelectedItemTemplate(MenuItem template) {
            this.selectedItemTemplate = template;
            return this;
        }

        // ==================== EVENT HANDLERS ====================

        public PaginationSection setOnItemSelect(BiConsumer<MenuClickEvent, Integer> handler) {
            this.onItemSelect = handler;
            return this;
        }

        public PaginationSection setOnPageChange(Consumer<Integer> handler) {
            this.onPageChange = handler;
            return this;
        }

        // ==================== SELECTION MANAGEMENT ====================

        public PaginationSection setSelectedIndex(Integer index) {
            this.selectedIndex = index;
            return this;
        }

        public Integer getSelectedIndex() {
            return selectedIndex;
        }

        public MenuItem getSelectedItem() {
            applyFilter();
            return selectedIndex != null && selectedIndex < filteredItems.size()
                    ? filteredItems.get(selectedIndex) : null;
        }

        // ==================== UTILITY METHODS ====================

        /**
         * Gets the total number of pages based on filtered items
         * @return The total pages
         */
        public int getTotalPages() {
            applyFilter();
            return Math.max(1, (int) Math.ceil((double) filteredItems.size() / itemsPerPage));
        }

        /**
         * Gets items for a specific page from filtered items
         * @param page The page number (1-based)
         * @return List of items for the page
         */
        public List<MenuItem> getItemsForPage(int page) {
            applyFilter();
            int start = (page - 1) * itemsPerPage;
            int end = Math.min(start + itemsPerPage, filteredItems.size());

            if (start >= filteredItems.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(filteredItems.subList(start, end));
        }

        /**
         * Checks if an item is selected based on filtered index
         * @param filteredIndex The filtered item index
         * @return True if the item is selected
         */
        public boolean isItemSelected(int filteredIndex) {
            return selectedIndex != null && selectedIndex.equals(filteredIndex);
        }

        private void adjustSelectionAfterRemoval(int removedIndex) {
            if (selectedIndex != null) {
                if (selectedIndex.equals(removedIndex)) {
                    selectedIndex = null;
                } else if (selectedIndex > removedIndex) {
                    selectedIndex--;
                }
            }
        }

        // ==================== GETTERS ====================

        public String getName() { return name; }
        public List<MenuItem> getAllItems() { return new ArrayList<>(allItems); }
        public int getItemCount() {
            applyFilter();
            return filteredItems.size();
        }
        public int getTotalItemCount() { return allItems.size(); }
        public int[] getSlots() { return slots.clone(); }
        public int getItemsPerPage() { return itemsPerPage; }
        public boolean hasFilter() { return currentFilter != null; }

        // Package-private getters for menu processing
        MenuItem getPreviousButton() { return previousButton; }
        MenuItem getNextButton() { return nextButton; }
        int getPreviousButtonSlot() { return previousButtonSlot; }
        int getNextButtonSlot() { return nextButtonSlot; }
        MenuItem getFillerItem() { return fillerItem; }
        MenuItem getSelectedItemTemplate() { return selectedItemTemplate; }
        BiConsumer<MenuClickEvent, Integer> getOnItemSelect() { return onItemSelect; }
        Consumer<Integer> getOnPageChange() { return onPageChange; }
    }

    // ==================== SECTION MANAGEMENT ====================

    public PaginationSection addSection(String name, int... slots) {
        PaginationSection section = new PaginationSection(name, slots);
        sections.put(name, section);
        return section;
    }

    public PaginationSection getSection(String name) {
        return sections.get(name);
    }

    public MultiPaginationMenu removeSection(String name) {
        sections.remove(name);
        return this;
    }

    public Set<String> getSectionNames() {
        return new HashSet<>(sections.keySet());
    }

    public MultiPaginationMenu setOnSectionUpdate(BiConsumer<String, Integer> handler) {
        this.onSectionUpdate = handler;
        return this;
    }

    // ==================== PAGE MANAGEMENT ====================

    public int getCurrentPage(Player player, String sectionName) {
        Map<String, Integer> pages = playerPages.get(player.getUniqueId());
        return pages != null ? pages.getOrDefault(sectionName, 1) : 1;
    }

    public MultiPaginationMenu setCurrentPage(Player player, String sectionName, int page) {
        PaginationSection section = sections.get(sectionName);
        if (section != null) {
            int maxPages = section.getTotalPages();
            int oldPage = getCurrentPage(player, sectionName);
            page = Math.max(1, Math.min(page, maxPages));

            playerPages.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(sectionName, page);

            if (onSectionUpdate != null) {
                onSectionUpdate.accept(sectionName, page);
            }

            if (section.getOnPageChange() != null) {
                section.getOnPageChange().accept(page);
            }
        }
        return this;
    }

    public MultiPaginationMenu nextPage(Player player, String sectionName) {
        int currentPage = getCurrentPage(player, sectionName);
        PaginationSection section = sections.get(sectionName);

        if (section != null && currentPage < section.getTotalPages()) {
            setCurrentPage(player, sectionName, currentPage + 1);

            if (viewer == player && isOpen()) {
                refreshSectionSynchronous(player, sectionName);
            }
        }
        return this;
    }

    public MultiPaginationMenu previousPage(Player player, String sectionName) {
        int currentPage = getCurrentPage(player, sectionName);

        if (currentPage > 1) {
            setCurrentPage(player, sectionName, currentPage - 1);

            if (viewer == player && isOpen()) {
                refreshSectionSynchronous(player, sectionName);
            }
        }
        return this;
    }

    /**
     * Refreshes a specific section synchronously
     * @param player The player
     * @param sectionName The section name
     */
    private void refreshSectionSynchronous(Player player, String sectionName) {
        PaginationSection section = sections.get(sectionName);
        if (section != null) {
            int currentPage = getCurrentPage(player, sectionName);
            processSectionForPlayer(player, section, currentPage);
            updateInventoryDisplay();
        }
    }

    // ==================== MENU OPERATIONS ====================

    @Override
    public void open(Player player, ExyliaContext context) {
        cleanupPlayerResources(player);
        initializePlayerPages(player);

        super.open(player, context);

        updateAllSectionsSynchronous(player);
    }

    /**
     * Synchronous update of all sections
     */
    private void updateAllSectionsSynchronous(Player player) {
        if (viewer != player || !isOpen()) {
            return;
        }

        try {
            super.applyFillers();

            Map<String, Integer> pages = playerPages.get(player.getUniqueId());
            if (pages != null) {
                sections.forEach((sectionName, section) -> {
                    int currentPage = pages.getOrDefault(sectionName, 1);
                    processSectionForPlayer(player, section, currentPage);
                });
            }

            updateInventoryDisplay();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void forceRefreshAllSections() {
        if (viewer != null && isOpen()) {
            updateAllSectionsSynchronous(viewer);
        }
    }

    public void refreshAllSections() {
        if (viewer != null && isOpen()) {
            updateAllSectionsAsync(viewer);
        }
    }

    /**
     * Refreshes a specific section and resets to page 1
     * Useful when applying filters that change the item count
     * @param sectionName The section name
     */
    public void refreshSectionWithReset(String sectionName) {
        if (viewer != null && isOpen()) {
            // Reset to page 1
            setCurrentPage(viewer, sectionName, 1);
            refreshSectionSynchronous(viewer, sectionName);
        }
    }

    // ==================== PRIVATE IMPLEMENTATION ====================

    private void initializePlayerPages(Player player) {
        Map<String, Integer> pages = new ConcurrentHashMap<>();
        sections.keySet().forEach(sectionName -> pages.put(sectionName, 1));
        playerPages.put(player.getUniqueId(), pages);
    }

    private CompletableFuture<Void> updateAllSectionsAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                super.applyFillers();

                Map<String, Integer> pages = playerPages.get(player.getUniqueId());
                if (pages != null) {
                    sections.forEach((sectionName, section) -> {
                        int currentPage = pages.getOrDefault(sectionName, 1);
                        processSectionForPlayer(player, section, currentPage);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory != null && viewer == player) {
                    updateInventoryDisplay();
                }
            });
        });
    }

    private void placeSectionItems(Player player, PaginationSection section, int currentPage) {
        List<MenuItem> pageItems = section.getItemsForPage(currentPage);
        int[] slots = section.getSlots();

        for (int i = 0; i < pageItems.size() && i < slots.length; i++) {
            int slot = slots[i];
            MenuItem item = pageItems.get(i).clone();
            int filteredIndex = (currentPage - 1) * section.getItemsPerPage() + i;

            if (section.isItemSelected(filteredIndex) && section.getSelectedItemTemplate() != null) {
                MenuItem selectedTemplate = section.getSelectedItemTemplate().clone();
                if (item.getContext() != null && !item.getContext().isEmpty()) {
                    selectedTemplate.mergeContext(item.getContext());
                }
                item = selectedTemplate;
            }

            ExyliaContext combinedContext = prepareItemContext(item);

            item.withContext(combinedContext);
            item.process(player);

            setupItemClickHandler(item, section, filteredIndex);
            items.put(slot, item);
        }
    }

    @Override
    protected ExyliaContext prepareItemContext(MenuItem item) {
        // Crear contexto base desde el menú
        ExyliaContext combinedContext = this.context != null ? this.context.createChild() : ExyliaContext.create();

        // Fusionar con el contexto específico del item (tiene prioridad)
        if (item.getContext() != null && !item.getContext().isEmpty()) {
            combinedContext.merge(item.getContext());
        }

        return combinedContext;
    }

    private void placeSectionNavigation(Player player, PaginationSection section, int currentPage) {
        String sectionName = section.getName();
        int totalPages = section.getTotalPages();

        // ==================== BOTÓN ANTERIOR ====================
        if (currentPage > 1 && section.getPreviousButton() != null && section.getPreviousButtonSlot() != -1) {
            MenuItem prevButton = section.getPreviousButton().clone();

            // ✅ USAR EL CONTEXTO DEL MENÚ PARA BOTONES DE NAVEGACIÓN
            ExyliaContext buttonContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
            // Agregar información de paginación al contexto
            buttonContext.put("current_page", currentPage);
            buttonContext.put("total_pages", totalPages);
            buttonContext.put("section_name", sectionName);

            prevButton.withContext(buttonContext);
            prevButton.process(player);

            prevButton.setClickHandler(event -> {
                previousPage(event.getPlayer(), sectionName);
            });

            items.put(section.getPreviousButtonSlot(), prevButton);
        } else {
            // Remover botón anterior si no debe aparecer
            if (section.getPreviousButtonSlot() != -1) {
                items.remove(section.getPreviousButtonSlot());

                MenuItem filler = getEffectiveFillerForSlot(section.getPreviousButtonSlot());
                if (filler != null) {
                    MenuItem fillerClone = filler.clone();
                    ExyliaContext fillerContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
                    fillerClone.withContext(fillerContext);
                    fillerClone.process(player);
                    items.put(section.getPreviousButtonSlot(), fillerClone);
                }
            }
        }

        // ==================== BOTÓN SIGUIENTE ====================
        if (currentPage < totalPages && section.getNextButton() != null && section.getNextButtonSlot() != -1) {
            MenuItem nextButton = section.getNextButton().clone();

            // ✅ USAR EL CONTEXTO DEL MENÚ PARA BOTONES DE NAVEGACIÓN
            ExyliaContext buttonContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
            // Agregar información de paginación al contexto
            buttonContext.put("current_page", currentPage);
            buttonContext.put("total_pages", totalPages);
            buttonContext.put("section_name", sectionName);

            nextButton.withContext(buttonContext);
            nextButton.process(player);

            nextButton.setClickHandler(event -> {
                nextPage(event.getPlayer(), sectionName);
            });

            items.put(section.getNextButtonSlot(), nextButton);
        } else {
            // Remover botón siguiente si no debe aparecer
            if (section.getNextButtonSlot() != -1) {
                items.remove(section.getNextButtonSlot());

                MenuItem filler = getEffectiveFillerForSlot(section.getNextButtonSlot());
                if (filler != null) {
                    MenuItem fillerClone = filler.clone();
                    ExyliaContext fillerContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
                    fillerClone.withContext(fillerContext);
                    fillerClone.process(player);
                    items.put(section.getNextButtonSlot(), fillerClone);
                }
            }
        }
    }

    private void processSectionForPlayer(Player player, PaginationSection section, int currentPage) {
        // Debug opcional (comentar en producción)
        // debugSectionState(section, currentPage);

        applySectionFiller(player, section);
        placeSectionItems(player, section, currentPage);
        placeSectionNavigation(player, section, currentPage);
    }

    /**
     * Fuerza la actualización de los botones de navegación para todas las secciones
     */
    public void forceRefreshNavigation() {
        if (viewer != null && isOpen()) {
            Map<String, Integer> pages = playerPages.get(viewer.getUniqueId());
            if (pages != null) {
                sections.forEach((sectionName, section) -> {
                    int currentPage = pages.getOrDefault(sectionName, 1);
                    placeSectionNavigation(viewer, section, currentPage);
                });
                updateInventoryDisplay();
            }
        }
    }

    /**
     * Gets the effective filler for a specific slot (considering border and global fillers)
     * @param slot The slot to get the filler for
     * @return The appropriate filler item
     */
    private MenuItem getEffectiveFillerForSlot(int slot) {
        if (borderFiller != null && isBorderSlot(slot)) {
            return borderFiller;
        }
        return globalFiller;
    }

    private void applySectionFiller(Player player, PaginationSection section) {
        if (section.getFillerItem() == null) return;

        for (int slot : section.getSlots()) {
            MenuItem filler = section.getFillerItem().clone();
            ExyliaContext fillerContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
            filler.withContext(fillerContext);
            filler.process(player);
            items.put(slot, filler);
        }
    }

    private void setupItemClickHandler(MenuItem item, PaginationSection section, int filteredIndex) {
        Consumer<MenuClickEvent> originalHandler = item.getClickHandler();

        item.setClickHandler(event -> {
            try {
                if (originalHandler != null) {
                    originalHandler.accept(event);
                }

                if (section.getOnItemSelect() != null) {
                    section.getOnItemSelect().accept(event, filteredIndex);
                }

                Integer oldSelection = section.getSelectedIndex();
                section.setSelectedIndex(filteredIndex);

                if (viewer != null && isOpen()) {
                    refreshSectionSynchronous(event.getPlayer(), section.getName());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateInventoryDisplay() {
        if (inventory == null) {
            return;
        }

        AtomicInteger itemsSet = new AtomicInteger();
        items.forEach((slot, item) -> {
            if (item != null) {
                inventory.setItem(slot, item.build());
                itemsSet.getAndIncrement();
            } else {
                inventory.setItem(slot, null);
            }
        });
    }

    private void scheduleAllSectionUpdates(Player player) {
        if (!playerItemTasks.containsKey(player.getUniqueId())) {
            playerItemTasks.put(player.getUniqueId(), new ConcurrentHashMap<>());
        }

        Map<String, Map<Integer, Integer>> sectionTasks = playerItemTasks.get(player.getUniqueId());

        sections.forEach((sectionName, section) -> {
            Map<Integer, Integer> itemTasks = sectionTasks.computeIfAbsent(sectionName, k -> new ConcurrentHashMap<>());

            for (int slot : section.getSlots()) {
                MenuItem item = getItem(slot);
                if (item != null && item.needsDynamicUpdate()) {
                    scheduleItemUpdate(player, slot, item, itemTasks);
                }
            }
        });
    }

    private void scheduleItemUpdate(Player player, int slot, MenuItem item, Map<Integer, Integer> itemTasks) {
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (player.isOnline() && getViewer() == player && inventory != null) {
                MenuItem currentItem = getItem(slot);
                if (currentItem != null) {
                    if (context != null) {
                        currentItem.withContext(context);
                        currentItem.process(viewer);
                    }
                    inventory.setItem(slot, currentItem.build());
                }
            } else {
                Integer existingTaskId = itemTasks.remove(slot);
                if (existingTaskId != null) {
                    Bukkit.getScheduler().cancelTask(existingTaskId);
                }
            }
        }, item.getUpdateInterval(), item.getUpdateInterval());

        itemTasks.put(slot, taskId);
    }

    // ==================== CLEANUP ====================

    private void onPlayerCloseMenu(Player player) {
        cleanupPlayerResources(player);
        if (externalCloseHandler != null) {
            externalCloseHandler.accept(player);
        }
    }

    private void cleanupPlayerResources(Player player) {
        UUID playerId = player.getUniqueId();
        playerPages.remove(playerId);

        Map<String, Map<Integer, Integer>> sectionTasks = playerItemTasks.remove(playerId);
        if (sectionTasks != null) {
            sectionTasks.values().forEach(itemTasks ->
                    itemTasks.values().forEach(taskId -> {
                        if (taskId != null && taskId != -1) {
                            Bukkit.getScheduler().cancelTask(taskId);
                        }
                    })
            );
        }
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public MultiPaginationMenu setCloseHandler(Consumer<Player> closeHandler) {
        this.externalCloseHandler = closeHandler;
        super.setCloseHandler(this::onPlayerCloseMenu);
        return this;
    }

    @Override
    public MultiPaginationMenu enableDynamicUpdates(JavaPlugin plugin, long tickInterval) {
        super.enableDynamicUpdates(plugin, tickInterval);
        return this;
    }

    @Override
    public MultiPaginationMenu disableDynamicUpdates() {
        super.disableDynamicUpdates();

        playerItemTasks.values().forEach(sectionTasks ->
                sectionTasks.values().forEach(itemTasks ->
                        itemTasks.values().forEach(taskId -> {
                            if (taskId != null && taskId != -1) {
                                Bukkit.getScheduler().cancelTask(taskId);
                            }
                        })
                )
        );
        playerItemTasks.clear();

        return this;
    }

    @Override
    public MultiPaginationMenu setGlobalFiller(MenuItem filler) {
        super.setGlobalFiller(filler);
        return this;
    }

    @Override
    public MultiPaginationMenu setBorderFiller(MenuItem borderItem) {
        super.setBorderFiller(borderItem);
        return this;
    }
}