// ==================== PAGINATION MENU ====================

package net.exylia.commons.ui.menus;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.context.MenuContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pagination menu implementation
 */
public class PaginationMenu extends Menu {

    private final List<MenuItem> paginationItems = new ArrayList<>();
    private final int[] itemSlots;
    private final int itemsPerPage;

    // Navigation
    private MenuItem previousButton;
    private MenuItem nextButton;
    private int previousButtonSlot;
    private int nextButtonSlot;

    // Per-player state
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    // Configuration
    private MenuItem itemSlotFiller;
    private String titleTemplate;

    public PaginationMenu(String title, int rows, int... itemSlots) {
        super(title, rows);
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        this.titleTemplate = title;

        initializeDefaultNavigation(rows);
    }

    private void initializeDefaultNavigation(int rows) {
        // Default navigation buttons
        this.previousButton = new MenuItem("ARROW")
                .setName("&c◀ Previous Page")
                .setLore("&7Click to go to the previous page");

        this.nextButton = new MenuItem("ARROW")
                .setName("&a▶ Next Page")
                .setLore("&7Click to go to the next page");

        this.previousButtonSlot = rows * 9 - 9; // Bottom left
        this.nextButtonSlot = rows * 9 - 1;    // Bottom right
    }

    // ==================== ITEM MANAGEMENT ====================

    /**
     * Adds an item to the pagination
     * @param item The item to add
     * @return This menu for chaining
     */
    public PaginationMenu addItem(MenuItem item) {
        paginationItems.add(item);
        return this;
    }

    /**
     * Adds multiple items to the pagination
     * @param items The items to add
     * @return This menu for chaining
     */
    public PaginationMenu addItems(Collection<MenuItem> items) {
        paginationItems.addAll(items);
        return this;
    }

    /**
     * Sets all pagination items
     * @param items The items to set
     * @return This menu for chaining
     */
    public PaginationMenu setItems(Collection<MenuItem> items) {
        paginationItems.clear();
        paginationItems.addAll(items);
        return this;
    }

    /**
     * Clears all pagination items
     * @return This menu for chaining
     */
    public PaginationMenu clearItems() {
        paginationItems.clear();
        return this;
    }

    /**
     * Updates a pagination item
     * @param index The index of the item
     * @param item The new item
     * @return This menu for chaining
     */
    public PaginationMenu updateItem(int index, MenuItem item) {
        if (index >= 0 && index < paginationItems.size()) {
            paginationItems.set(index, item);

            // Update display if currently visible
            if (isOpen() && viewer != null) {
                refreshCurrentPage();
            }
        }
        return this;
    }

    // ==================== NAVIGATION CONFIGURATION ====================

    /**
     * Sets the previous page button
     * @param button The button item
     * @param slot The slot for the button
     * @return This menu for chaining
     */
    public PaginationMenu setPreviousButton(MenuItem button, int slot) {
        this.previousButton = button;
        this.previousButtonSlot = slot;
        return this;
    }

    /**
     * Sets the next page button
     * @param button The button item
     * @param slot The slot for the button
     * @return This menu for chaining
     */
    public PaginationMenu setNextButton(MenuItem button, int slot) {
        this.nextButton = button;
        this.nextButtonSlot = slot;
        return this;
    }

    /**
     * Sets the filler item for empty item slots
     * @param filler The filler item
     * @return This menu for chaining
     */
    public PaginationMenu setItemSlotFiller(MenuItem filler) {
        this.itemSlotFiller = filler;
        return this;
    }

    // ==================== PAGE MANAGEMENT ====================

    /**
     * Opens the menu to a specific page
     * @param player The player
     * @param page The page number (1-based)
     */
    public void openToPage(Player player, int page) {
        setCurrentPage(player, page);
        open(player);
    }

    /**
     * Opens the menu to a specific page with context
     * @param player The player
     * @param page The page number (1-based)
     * @param context The menu context
     */
    public void openToPage(Player player, int page, MenuContext context) {
        setCurrentPage(player, page);
        open(player, context);
    }

    /**
     * Sets the current page for a player
     * @param player The player
     * @param page The page number (1-based)
     */
    public void setCurrentPage(Player player, int page) {
        int maxPages = getTotalPages();
        page = Math.max(1, Math.min(page, maxPages));
        playerPages.put(player.getUniqueId(), page);
    }

    /**
     * Gets the current page for a player
     * @param player The player
     * @return The current page (1-based)
     */
    public int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 1);
    }

    /**
     * Gets the total number of pages
     * @return The total pages
     */
    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) paginationItems.size() / itemsPerPage));
    }

    /**
     * Goes to the next page
     * @param player The player
     */
    public void nextPage(Player player) {
        int currentPage = getCurrentPage(player);
        if (currentPage < getTotalPages()) {
            setCurrentPage(player, currentPage + 1);
            refreshForPlayer(player);
        }
    }

    /**
     * Goes to the previous page
     * @param player The player
     */
    public void previousPage(Player player) {
        int currentPage = getCurrentPage(player);
        if (currentPage > 1) {
            setCurrentPage(player, currentPage - 1);
            refreshForPlayer(player);
        }
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public void open(Player player, MenuContext context) {
        // Set default page if not set
        if (!playerPages.containsKey(player.getUniqueId())) {
            playerPages.put(player.getUniqueId(), 1);
        }

        super.open(player, context);
    }

    @Override
    protected void processTitle() {
        if (titleTemplate != null && viewer != null) {
            int currentPage = getCurrentPage(viewer);
            int totalPages = getTotalPages();

            String processed = titleTemplate
                    .replace("{page}", String.valueOf(currentPage))
                    .replace("{pages}", String.valueOf(totalPages))
                    .replace("{total_items}", String.valueOf(paginationItems.size()));

            if (context != null) {
                processed = context.processPlaceholders(processed, viewer);
            }

            this.title = net.exylia.commons.utils.ColorUtils.parse(processed);
        }
    }

    @Override
    protected void populateInventory() {
        if (inventory == null || viewer == null) return;

        // Clear inventory first
        inventory.clear();

        // Apply base fillers
        super.populateInventory();

        // Add pagination items for current page
        populatePageItems();

        // Add navigation buttons
        populateNavigation();
    }

    /**
     * Populates the current page items
     */
    private void populatePageItems() {
        int currentPage = getCurrentPage(viewer);
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, paginationItems.size());

        // Clear item slots first
        for (int slot : itemSlots) {
            inventory.setItem(slot, null);
        }

        // Add page items
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < itemSlots.length) {
                MenuItem item = paginationItems.get(i).clone();

                int slot = itemSlots[slotIndex];
                inventory.setItem(slot, item.build());
                items.put(slot, item); // Store for click handling
            }
        }

        // Fill empty item slots with filler
        if (itemSlotFiller != null) {
            int itemsInPage = endIndex - startIndex;
            for (int i = itemsInPage; i < itemSlots.length; i++) {
                MenuItem filler = itemSlotFiller.clone();

                // Para el filler SÍ procesar con contexto (no tiene Game)
                filler.processWithContext(context, viewer);

                int slot = itemSlots[i];
                inventory.setItem(slot, filler.build());
                items.put(slot, filler);
            }
        }
    }

    /**
     * Populates navigation buttons
     */
    private void populateNavigation() {
        int currentPage = getCurrentPage(viewer);
        int totalPages = getTotalPages();

        // Clear navigation slots
        inventory.setItem(previousButtonSlot, null);
        inventory.setItem(nextButtonSlot, null);
        items.remove(previousButtonSlot);
        items.remove(nextButtonSlot);

        // Previous button
        if (currentPage > 1 && previousButton != null) {
            MenuItem prevBtn = previousButton.clone();
            prevBtn.setClickHandler(this::handlePreviousClick);
            prevBtn.processWithContext(context, viewer);

            inventory.setItem(previousButtonSlot, prevBtn.build());
            items.put(previousButtonSlot, prevBtn);
        } else {
            // Fill with global filler if no previous button is needed
            fillNavigationSlot(previousButtonSlot);
        }

        // Next button
        if (currentPage < totalPages && nextButton != null) {
            MenuItem nextBtn = nextButton.clone();
            nextBtn.setClickHandler(this::handleNextClick);
            nextBtn.processWithContext(context, viewer);

            inventory.setItem(nextButtonSlot, nextBtn.build());
            items.put(nextButtonSlot, nextBtn);
        } else {
            // Fill with global filler if no next button is needed
            fillNavigationSlot(nextButtonSlot);
        }
    }

    /**
     * Fills a navigation slot with the appropriate filler
     * @param slot The slot to fill
     */
    private void fillNavigationSlot(int slot) {
        MenuItem filler = getEffectiveFillerForSlot(slot);
        if (filler != null) {
            MenuItem fillerClone = filler.clone();
            fillerClone.processWithContext(context, viewer);
            inventory.setItem(slot, fillerClone.build());
            items.put(slot, fillerClone);
        }
    }

    /**
     * Gets the effective filler for a specific slot (considering border and global fillers)
     * @param slot The slot to get the filler for
     * @return The appropriate filler item
     */
    private MenuItem getEffectiveFillerForSlot(int slot) {
        // Apply border filler if this is a border slot and border filler is available
        if (borderFiller != null && isBorderSlot(slot)) {
            return borderFiller;
        }

        // Apply global filler
        return globalFiller;
    }

    /**
     * Handles previous button click
     * @param event The click event
     */
    private void handlePreviousClick(MenuClickEvent event) {
        previousPage(event.getPlayer());
    }

    /**
     * Handles next button click
     * @param event The click event
     */
    private void handleNextClick(MenuClickEvent event) {
        nextPage(event.getPlayer());
    }

    /**
     * Refreshes the menu for a specific player
     * @param player The player
     */
    private void refreshForPlayer(Player player) {
        if (viewer == player && isOpen()) {
            processTitle();
            populateInventory();
        }
    }

    /**
     * Refreshes the current page
     */
    private void refreshCurrentPage() {
        if (viewer != null && isOpen()) {
            refreshForPlayer(viewer);
        }
    }

    @Override
    protected void onClose() {
        if (viewer != null) {
            playerPages.remove(viewer.getUniqueId());
        }
        super.onClose();
    }

    // ==================== GETTERS ====================

    public List<MenuItem> getPaginationItems() {
        return new ArrayList<>(paginationItems);
    }

    public int[] getItemSlots() {
        return itemSlots.clone();
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }
}
