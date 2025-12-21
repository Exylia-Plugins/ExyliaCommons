package net.exylia.commons.ui.menus;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.ui.commons.SlotUtils.parseSlots;

public class PaginationMenu extends Menu {

    private final List<MenuItem> paginationItems = new ArrayList<>();
    private final int[] itemSlots;
    @Getter
    private final int itemsPerPage;

    private MenuItem previousButton;
    private MenuItem nextButton;
    private int previousButtonSlot;
    private int nextButtonSlot;

    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    private MenuItem itemSlotFiller;
    private String titleTemplate;

    public PaginationMenu(String title, int rows, int... itemSlots) {
        super(title, rows);
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        this.titleTemplate = title;
    }

    public PaginationMenu(String title, int rows, String itemSlotsString) {
        super(title, rows);
        int[] itemSlots = parseSlots(itemSlotsString, rows);
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        this.titleTemplate = title;
    }

    public PaginationMenu(String title, int rows, int[] itemSlots, ExyliaContext context) {
        super(title, rows, context);
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        this.titleTemplate = title;
    }

    public PaginationMenu(String title, int rows, String itemSlotsString, ExyliaContext context) {
        super(title, rows, context);
        int[] itemSlots = parseSlots(itemSlotsString, rows);
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        this.titleTemplate = title;
    }

    public void initializeDefaultNavigation(int rows) {
        this.previousButton = new MenuItem("headbase-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGExZDU1YjNmOTg5NDEwYTM0NzUyNjUwZTI0OGM5YjZjMTc4M2E3ZWMyYWEzZmQ3Nzg3YmRjNGQwZTYzN2QzOSJ9fX0=")
                .setName("{error}◀ Previous Page")
                .setLore("{letters}Click to go to the previous page");

        this.nextButton = new MenuItem("headbase-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmE4N2UzZDk2ZTFjZmViOWNjZmIzYmE1M2EyMTdmYWY1MjQ5ZTI4NTUzM2IyNzFhMmZiMjg0YzMwZGJkOTgyOSJ9fX0=")
                .setName("{success}▶ Next Page")
                .setLore("{letters}Click to go to the next page");

        this.previousButtonSlot = rows * 9 - 6;
        this.nextButtonSlot = rows * 9 - 4;
        this.globalFiller = new MenuItem("BLACK_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();
        this.itemSlotFiller = new MenuItem("LIGHT_GRAY_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();
    }

    public PaginationMenu addItem(MenuItem item) {
        paginationItems.add(item);
        return this;
    }

    public PaginationMenu addItems(Collection<MenuItem> items) {
        paginationItems.addAll(items);
        return this;
    }

    public PaginationMenu setItems(Collection<MenuItem> items) {
        paginationItems.clear();
        paginationItems.addAll(items);
        return this;
    }

    public PaginationMenu clearItems() {
        paginationItems.clear();
        return this;
    }

    public PaginationMenu updateItem(int index, MenuItem item) {
        if (index >= 0 && index < paginationItems.size()) {
            paginationItems.set(index, item);

            if (isOpen() && viewer != null) {
                refreshCurrentPage();
            }
        }
        return this;
    }

    public PaginationMenu setPreviousButton(MenuItem button, int slot) {
        this.previousButton = button;
        this.previousButtonSlot = slot;
        return this;
    }

    public PaginationMenu setNextButton(MenuItem button, int slot) {
        this.nextButton = button;
        this.nextButtonSlot = slot;
        return this;
    }

    public PaginationMenu setItemSlotFiller(MenuItem filler) {
        this.itemSlotFiller = filler;
        return this;
    }

    public void openToPage(Player player, int page) {
        setCurrentPage(player, page);
        open(player);
    }

    public void openToPage(Player player, int page, ExyliaContext context) {
        setCurrentPage(player, page);
        open(player, context);
    }

    public void setCurrentPage(Player player, int page) {
        int maxPages = getTotalPages();
        page = Math.max(1, Math.min(page, maxPages));
        playerPages.put(player.getUniqueId(), page);
    }

    public int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 1);
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) paginationItems.size() / itemsPerPage));
    }

    public void nextPage(Player player) {
        int currentPage = getCurrentPage(player);
        if (currentPage < getTotalPages()) {
            setCurrentPage(player, currentPage + 1);
            refreshForPlayer(player);
        }
    }

    public void previousPage(Player player) {
        int currentPage = getCurrentPage(player);
        if (currentPage > 1) {
            setCurrentPage(player, currentPage - 1);
            refreshForPlayer(player);
        }
    }

    @Override
    public void open(Player player, ExyliaContext context) {
         
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

            this.title = ColorAPI.parse(processed);
        }
    }

    @Override
    protected void populateInventory() {
        if (inventory == null || viewer == null) return;

        inventory.clear();

        super.populateInventory();

        populatePageItems();

        populateNavigation();
    }

    private void populatePageItems() {
        int currentPage = getCurrentPage(viewer);
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, paginationItems.size());

        for (int slot : itemSlots) {
            inventory.setItem(slot, null);
        }

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < itemSlots.length) {
                MenuItem item = paginationItems.get(i).clone();

                int slot = itemSlots[slotIndex];
                inventory.setItem(slot, item.build());
                items.put(slot, item);  
            }
        }

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

    private void populateNavigation() {
        int currentPage = getCurrentPage(viewer);
        int totalPages = getTotalPages();

        inventory.setItem(previousButtonSlot, null);
        inventory.setItem(nextButtonSlot, null);
        items.remove(previousButtonSlot);
        items.remove(nextButtonSlot);

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
             
            fillNavigationSlot(previousButtonSlot);
        }

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
             
            fillNavigationSlot(nextButtonSlot);
        }
    }

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

    private MenuItem getEffectiveFillerForSlot(int slot) {
         
        if (borderFiller != null && isBorderSlot(slot)) {
            return borderFiller;
        }

        return globalFiller;
    }

    private void handlePreviousClick(MenuClickEvent event) {
        previousPage(event.getPlayer());
    }

    private void handleNextClick(MenuClickEvent event) {
        nextPage(event.getPlayer());
    }

    private void refreshForPlayer(Player player) {
        if (viewer == player && isOpen()) {
            processTitle();
            populateInventory();
        }
    }

    public void refreshCurrentPage() {
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

    public List<MenuItem> getPaginationItems() {
        return new ArrayList<>(paginationItems);
    }

    public int[] getItemSlots() {
        return itemSlots.clone();
    }
}
