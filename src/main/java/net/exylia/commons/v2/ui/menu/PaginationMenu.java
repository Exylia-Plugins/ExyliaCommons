package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ItemsAPI;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.pagination.PageCalculator;
import net.exylia.commons.v2.ui.pagination.PaginationTracker;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class PaginationMenu extends MenuBase {

    public PaginationMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menuData.getSize(), processPaginationTitle(menuData.getTitle()));
    }

    @Override
    protected void populateItems() {
        applyFillers();
        applyPaginationItems();
        applyNavigation();
        applyStaticItems();
    }

    @Override
    protected void handleClickInternal(int slot, ClickType clickType) {
        NavigationData nav = menuData.getPaginationNavigation();
        if (nav == null) {
            return;
        }

        if (nav.hasPreviousButton() && slot == nav.getPreviousButtonSlot()) {
            previousPage();
        } else if (nav.hasNextButton() && slot == nav.getNextButtonSlot()) {
            nextPage();
        }
    }

    private void applyPaginationItems() {
        if (!menuData.hasPagination()) {
            return;
        }

        int currentPage = getCurrentPage();
        List<Integer> slots = menuData.getPaginationSlots();
        List<ItemData> allItems = menuData.getPaginationItems();

        int itemsPerPage = slots.size();
        List<ItemData> pageItems = PageCalculator.getPageItems(allItems, currentPage, itemsPerPage);

        DebugAPI.logLibDebug(DebugCategory.UI, "Applying pagination items for menu " + menuId + ": page " + currentPage + ", displaying " + pageItems.size() + " items");

        for (int i = 0; i < pageItems.size() && i < slots.size(); i++) {
            int slot = slots.get(i);
            ItemData itemData = pageItems.get(i);

            int globalIndex = PageCalculator.getStartIndex(currentPage, itemsPerPage) + i;

            PlaceholderContext paginationContext = context.copy()
                    .put("index", globalIndex)
                    .put("page_index", i)
                    .put("current_page", currentPage)
                    .put("total_pages", getTotalPages());

            PlaceholderContext mergedContext = itemData.getContext() != null
                    ? paginationContext.copyAndMerge(itemData.getContext())
                    : paginationContext;

            ItemData enhancedItemData = itemData.toBuilder()
                    .context(mergedContext)
                    .build();

            setItem(slot, enhancedItemData);
        }
    }

    private void applyNavigation() {
        NavigationData nav = menuData.getPaginationNavigation();
        if (nav == null) {
            return;
        }

        int currentPage = getCurrentPage();
        int totalPages = getTotalPages();

        if (currentPage > 1 && nav.hasPreviousButton()) {
            PlaceholderContext navContext = context.copy()
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData prevButtonData = nav.getPreviousButton().toBuilder()
                    .context(navContext)
                    .build();

            setItem(nav.getPreviousButtonSlot(), prevButtonData);
        }

        if (currentPage < totalPages && nav.hasNextButton()) {
            PlaceholderContext navContext = context.copy()
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData nextButtonData = nav.getNextButton().toBuilder()
                    .context(navContext)
                    .build();

            setItem(nav.getNextButtonSlot(), nextButtonData);
        }

        if (nav.hasInfoItem()) {
            PlaceholderContext infoContext = context.copy()
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData infoItemData = nav.getInfoItem().toBuilder()
                    .context(infoContext)
                    .build();

            setItem(nav.getInfoItemSlot(), infoItemData);
        }
    }

    public void nextPage() {
        int currentPage = getCurrentPage();
        int totalPages = getTotalPages();

        if (currentPage < totalPages) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " navigating to next page: " + (currentPage + 1) + "/" + totalPages);
            setCurrentPage(currentPage + 1);
            refresh();
        }
    }

    public void previousPage() {
        int currentPage = getCurrentPage();

        if (currentPage > 1) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " navigating to previous page: " + (currentPage - 1) + "/" + getTotalPages());
            setCurrentPage(currentPage - 1);
            refresh();
        }
    }

    public void setPage(int page) {
        int totalPages = getTotalPages();
        int originalPage = page;
        page = PageCalculator.clampPage(page, totalPages);
        DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " setting page to " + page + " (requested: " + originalPage + ", total: " + totalPages + ")");
        setCurrentPage(page);
        refresh();
    }

    private void refresh() {
        itemsBySlot.clear();
        populateItems();
        updateInventoryDisplay();

        if (inventory != null) {
            Inventory newInventory = Bukkit.createInventory(null, menuData.getSize(), processPaginationTitle(menuData.getTitle()));
            itemsBySlot.forEach((slot, item) -> {
                if (slot >= 0 && slot < newInventory.getSize()) {
                    newInventory.setItem(slot, item.getItemStack());
                }
            });

            player.openInventory(newInventory);
            this.inventory = newInventory;
        }
    }

    private net.kyori.adventure.text.Component processPaginationTitle(String title) {
        int currentPage = getCurrentPage();
        int totalPages = getTotalPages();

        PlaceholderContext titleContext = context.copy()
                .put("current_page", currentPage)
                .put("total_pages", totalPages);

        String processedTitle = processTitle(title);

        String processed = processedTitle
                .replace("%current_page%", String.valueOf(currentPage))
                .replace("%total_pages%", String.valueOf(totalPages))
                .replace("{current_page}", String.valueOf(currentPage))
                .replace("{total_pages}", String.valueOf(totalPages));

        return ColorAPI.parse(processed);
    }

    private int getCurrentPage() {
        return PaginationTracker.getCurrentPage(player.getUniqueId());
    }

    private void setCurrentPage(int page) {
        PaginationTracker.setCurrentPage(player.getUniqueId(), page);
    }

    private int getTotalPages() {
        if (!menuData.hasPagination()) {
            return 1;
        }

        int itemsPerPage = menuData.getPaginationSlots().size();
        int totalItems = menuData.getPaginationItems().size();

        return PageCalculator.getTotalPages(totalItems, itemsPerPage);
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        PaginationTracker.clearPlayer(player.getUniqueId());
    }
}
