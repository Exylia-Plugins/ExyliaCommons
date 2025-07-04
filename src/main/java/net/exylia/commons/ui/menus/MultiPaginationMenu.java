// ==================== MULTI PAGINATION MENU V2 ====================

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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Advanced multi-section pagination menu with optimized performance
 * Each section can have independent pagination, navigation, and items
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
     * Independent pagination section with its own items, navigation, and settings
     */
    public static class PaginationSection {
        private final String name;
        private final List<MenuItem> items = new ArrayList<>();
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
            items.add(item);
            return this;
        }

        /**
         * Adds multiple items to this section
         * @param items The items to add
         * @return This section for chaining
         */
        public PaginationSection addItems(Collection<MenuItem> items) {
            this.items.addAll(items);
            return this;
        }

        /**
         * Sets all items for this section
         * @param newItems The items to set
         * @return This section for chaining
         */
        public PaginationSection setItems(Collection<MenuItem> newItems) {
            items.clear();
            items.addAll(newItems);
            selectedIndex = null;
            return this;
        }

        /**
         * Clears all items from this section
         * @return This section for chaining
         */
        public PaginationSection clearItems() {
            items.clear();
            selectedIndex = null;
            return this;
        }

        /**
         * Updates an item at a specific index
         * @param index The index to update
         * @param item The new item
         * @return This section for chaining
         */
        public PaginationSection updateItem(int index, MenuItem item) {
            if (index >= 0 && index < items.size()) {
                items.set(index, item);
            }
            return this;
        }

        /**
         * Removes an item at a specific index
         * @param index The index to remove
         * @return This section for chaining
         */
        public PaginationSection removeItem(int index) {
            if (index >= 0 && index < items.size()) {
                items.remove(index);
                adjustSelectionAfterRemoval(index);
            }
            return this;
        }

        // ==================== NAVIGATION CONFIGURATION ====================

        /**
         * Sets the previous page button
         * @param button The button item
         * @param slot The slot for the button
         * @return This section for chaining
         */
        public PaginationSection setPreviousButton(MenuItem button, int slot) {
            this.previousButton = button;
            this.previousButtonSlot = slot;
            return this;
        }

        /**
         * Sets the next page button
         * @param button The button item
         * @param slot The slot for the button
         * @return This section for chaining
         */
        public PaginationSection setNextButton(MenuItem button, int slot) {
            this.nextButton = button;
            this.nextButtonSlot = slot;
            return this;
        }

        /**
         * Sets the filler item for empty slots in this section
         * @param filler The filler item
         * @return This section for chaining
         */
        public PaginationSection setFillerItem(MenuItem filler) {
            this.fillerItem = filler;
            return this;
        }

        /**
         * Sets the template for selected items
         * @param template The selected item template
         * @return This section for chaining
         */
        public PaginationSection setSelectedItemTemplate(MenuItem template) {
            this.selectedItemTemplate = template;
            return this;
        }

        // ==================== EVENT HANDLERS ====================

        /**
         * Sets the item selection handler
         * @param handler The selection handler
         * @return This section for chaining
         */
        public PaginationSection setOnItemSelect(BiConsumer<MenuClickEvent, Integer> handler) {
            this.onItemSelect = handler;
            return this;
        }

        /**
         * Sets the page change handler
         * @param handler The page change handler
         * @return This section for chaining
         */
        public PaginationSection setOnPageChange(Consumer<Integer> handler) {
            this.onPageChange = handler;
            return this;
        }

        // ==================== SELECTION MANAGEMENT ====================

        /**
         * Sets the selected item index
         * @param index The index to select (null to clear selection)
         * @return This section for chaining
         */
        public PaginationSection setSelectedIndex(Integer index) {
            this.selectedIndex = index;
            return this;
        }

        /**
         * Gets the selected item index
         * @return The selected index or null
         */
        public Integer getSelectedIndex() {
            return selectedIndex;
        }

        /**
         * Gets the selected item
         * @return The selected item or null
         */
        public MenuItem getSelectedItem() {
            return selectedIndex != null && selectedIndex < items.size()
                    ? items.get(selectedIndex) : null;
        }

        // ==================== UTILITY METHODS ====================

        /**
         * Gets the total number of pages
         * @return The total pages
         */
        public int getTotalPages() {
            return Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
        }

        /**
         * Gets items for a specific page
         * @param page The page number (1-based)
         * @return List of items for the page
         */
        public List<MenuItem> getItemsForPage(int page) {
            int start = (page - 1) * itemsPerPage;
            int end = Math.min(start + itemsPerPage, items.size());

            if (start >= items.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(items.subList(start, end));
        }

        /**
         * Checks if an item is selected
         * @param globalIndex The global item index
         * @return True if the item is selected
         */
        public boolean isItemSelected(int globalIndex) {
            return selectedIndex != null && selectedIndex.equals(globalIndex);
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
        public List<MenuItem> getAllItems() { return new ArrayList<>(items); }
        public int getItemCount() { return items.size(); }
        public int[] getSlots() { return slots.clone(); }
        public int getItemsPerPage() { return itemsPerPage; }

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

    /**
     * Adds a new pagination section
     * @param name The section name
     * @param slots The slots for this section's items
     * @return The created section
     */
    public PaginationSection addSection(String name, int... slots) {
        PaginationSection section = new PaginationSection(name, slots);
        sections.put(name, section);
        return section;
    }

    /**
     * Gets an existing section
     * @param name The section name
     * @return The section or null if not found
     */
    public PaginationSection getSection(String name) {
        return sections.get(name);
    }

    /**
     * Removes a section
     * @param name The section name
     * @return This menu for chaining
     */
    public MultiPaginationMenu removeSection(String name) {
        sections.remove(name);
        return this;
    }

    /**
     * Gets all section names
     * @return Set of section names
     */
    public Set<String> getSectionNames() {
        return new HashSet<>(sections.keySet());
    }

    /**
     * Sets the section update handler
     * @param handler The update handler
     * @return This menu for chaining
     */
    public MultiPaginationMenu setOnSectionUpdate(BiConsumer<String, Integer> handler) {
        this.onSectionUpdate = handler;
        return this;
    }

    // ==================== PAGE MANAGEMENT ====================

    /**
     * Gets the current page for a section
     * @param player The player
     * @param sectionName The section name
     * @return The current page (1-based)
     */
    public int getCurrentPage(Player player, String sectionName) {
        Map<String, Integer> pages = playerPages.get(player.getUniqueId());
        return pages != null ? pages.getOrDefault(sectionName, 1) : 1;
    }

    /**
     * Sets the current page for a section
     * @param player The player
     * @param sectionName The section name
     * @param page The page to set (1-based)
     * @return This menu for chaining
     */
    public MultiPaginationMenu setCurrentPage(Player player, String sectionName, int page) {
        PaginationSection section = sections.get(sectionName);
        if (section != null) {
            int maxPages = section.getTotalPages();
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

    /**
     * Navigates to the next page in a section
     * @param player The player
     * @param sectionName The section name
     * @return This menu for chaining
     */
    public MultiPaginationMenu nextPage(Player player, String sectionName) {
        int currentPage = getCurrentPage(player, sectionName);
        PaginationSection section = sections.get(sectionName);

        if (section != null && currentPage < section.getTotalPages()) {
            setCurrentPage(player, sectionName, currentPage + 1);
            refreshSection(player, sectionName);
        }
        return this;
    }

    /**
     * Navigates to the previous page in a section
     * @param player The player
     * @param sectionName The section name
     * @return This menu for chaining
     */
    public MultiPaginationMenu previousPage(Player player, String sectionName) {
        int currentPage = getCurrentPage(player, sectionName);

        if (currentPage > 1) {
            setCurrentPage(player, sectionName, currentPage - 1);
            refreshSection(player, sectionName);
        }
        return this;
    }

    // ==================== MENU OPERATIONS ====================

    @Override
    public void open(Player player, ExyliaContext context) {
        cleanupPlayerResources(player);
        initializePlayerPages(player);

        super.open(player, context);

        // Update all sections asynchronously
        updateAllSectionsAsync(player).thenRun(() -> {
            if (dynamicUpdates && plugin != null) {
                scheduleAllSectionUpdates(player);
            }
        });
    }

    /**
     * Refreshes a specific section for a player
     * @param player The player
     * @param sectionName The section name
     */
    public void refreshSection(Player player, String sectionName) {
        if (viewer == player && isOpen()) {
            PaginationSection section = sections.get(sectionName);
            if (section != null) {
                int currentPage = getCurrentPage(player, sectionName);
                updateSectionAsync(player, section, currentPage);
            }
        }
    }

    /**
     * Refreshes all sections for the current player
     */
    public void refreshAllSections() {
        if (viewer != null && isOpen()) {
            updateAllSectionsAsync(viewer);
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
                // Apply global fillers first
                super.applyFillers();

                // Process each section
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
            // Update inventory in main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory != null && viewer == player) {
                    updateInventoryDisplay();
                }
            });
        });
    }

    private CompletableFuture<Void> updateSectionAsync(Player player, PaginationSection section, int page) {
        return CompletableFuture.runAsync(() -> {
            try {
                processSectionForPlayer(player, section, page);
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

    private void processSectionForPlayer(Player player, PaginationSection section, int currentPage) {
        clearSectionSlots(section);
        applySectionFiller(player, section);
        placeSectionItems(player, section, currentPage);
        placeSectionNavigation(player, section, currentPage);
    }

    private void clearSectionSlots(PaginationSection section) {
        for (int slot : section.getSlots()) {
            items.remove(slot);
        }

        if (section.getPreviousButtonSlot() != -1) {
            items.remove(section.getPreviousButtonSlot());
        }
        if (section.getNextButtonSlot() != -1) {
            items.remove(section.getNextButtonSlot());
        }
    }

    private void applySectionFiller(Player player, PaginationSection section) {
        if (section.getFillerItem() == null) return;

        for (int slot : section.getSlots()) {
            MenuItem filler = section.getFillerItem().clone();
            if (context != null) {
                filler.withContext(context);
                filler.process(player);
            }
            items.put(slot, filler);
        }
    }

    private void placeSectionItems(Player player, PaginationSection section, int currentPage) {
        List<MenuItem> pageItems = section.getItemsForPage(currentPage);
        int[] slots = section.getSlots();

        for (int i = 0; i < pageItems.size() && i < slots.length; i++) {
            int slot = slots[i];
            MenuItem item = pageItems.get(i).clone();
            int globalIndex = (currentPage - 1) * section.getItemsPerPage() + i;

            // Apply selection template if needed
            if (section.isItemSelected(globalIndex) && section.getSelectedItemTemplate() != null) {
                item = section.getSelectedItemTemplate().clone();
            }

            // Process with context
            if (context != null) {
                item.withContext(context);
                item.process(player);
            }

            // Setup click handler
            setupItemClickHandler(item, section, globalIndex);
            items.put(slot, item);
        }
    }

    private void setupItemClickHandler(MenuItem item, PaginationSection section, int globalIndex) {
        Consumer<MenuClickEvent> originalHandler = item.getClickHandler();

        item.setClickHandler(event -> {
            try {
                if (section.getOnItemSelect() != null) {
                    section.getOnItemSelect().accept(event, globalIndex);
                }

                section.setSelectedIndex(globalIndex);
                refreshSection(event.getPlayer(), section.getName());

                if (originalHandler != null) {
                    originalHandler.accept(event);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void placeSectionNavigation(Player player, PaginationSection section, int currentPage) {
        String sectionName = section.getName();
        int totalPages = section.getTotalPages();

        // Previous button
        if (currentPage > 1 && section.getPreviousButton() != null && section.getPreviousButtonSlot() != -1) {
            MenuItem prevButton = section.getPreviousButton().clone();
            if (context != null) {
                prevButton.withContext(context);
                prevButton.process(viewer);
            }
            prevButton.setClickHandler(event -> previousPage(event.getPlayer(), sectionName));
            items.put(section.getPreviousButtonSlot(), prevButton);
        }

        // Next button
        if (currentPage < totalPages && section.getNextButton() != null && section.getNextButtonSlot() != -1) {
            MenuItem nextButton = section.getNextButton().clone();
            if (context != null) {
                nextButton.withContext(context);
                nextButton.process(viewer);
            }
            nextButton.setClickHandler(event -> nextPage(event.getPlayer(), sectionName));
            items.put(section.getNextButtonSlot(), nextButton);
        }
    }

    private void updateInventoryDisplay() {
        items.forEach((slot, item) -> {
            if (item != null) {
                inventory.setItem(slot, item.build());
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