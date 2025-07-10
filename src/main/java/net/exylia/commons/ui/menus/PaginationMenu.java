// ==================== PAGINATION MENU ====================

package net.exylia.commons.ui.menus;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.utils.MenuUtils.parseSlots;

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

    public PaginationMenu(String title, int rows, String itemSlotsString) {
        super(title, rows);
        int[] itemSlots = parseSlots(itemSlotsString, rows);
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        this.titleTemplate = title;

        initializeDefaultNavigation(rows);
    }

    private void initializeDefaultNavigation(int rows) {
        this.previousButton = new MenuItem("headbase-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGExZDU1YjNmOTg5NDEwYTM0NzUyNjUwZTI0OGM5YjZjMTc4M2E3ZWMyYWEzZmQ3Nzg3YmRjNGQwZTYzN2QzOSJ9fX0=")
                .setName("{error}◀ Previous Page")
                .setLore("{letters}Click to go to the previous page");

        this.nextButton = new MenuItem("headurl-http://textures.minecraft.net/texture/fa87e3d96e1cfeb9ccfb3ba53a217faf5249e285533b271a2fb284c30dbd9829")
                .setName("{success}▶ Next Page")
                .setLore("{letters}Click to go to the next page");

        this.previousButtonSlot = rows * 9 - 6; // Center left
        this.nextButtonSlot = rows * 9 - 4;    // Center right
        this.globalFiller = new MenuItem("BLACK_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();
        this.itemSlotFiller = new MenuItem("LIGHT_GRAY_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();
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
    public void openToPage(Player player, int page, ExyliaContext context) {
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
    public void open(Player player, ExyliaContext context) {
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
                processed = PlaceholderSystemManager.getInstance().process(processed, viewer);
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

                if (context != null) {
                    filler.withContext(context);
                    filler.process(viewer);
                }

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
            if (context != null) {
                prevBtn.withContext(context);
                prevBtn.process(viewer);
            }

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
            if (context != null) {
                nextBtn.withContext(context);
                nextBtn.process(viewer);
            }

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
            if (context != null) {
                fillerClone.withContext(context);
                fillerClone.process(viewer);
            }
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
