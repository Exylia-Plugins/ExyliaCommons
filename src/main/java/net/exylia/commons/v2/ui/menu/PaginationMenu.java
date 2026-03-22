package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.animation.AnimationExecutor;
import net.exylia.commons.v2.ui.animation.AnimationSettings;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.ui.packet.PacketEventsSupport;
import net.exylia.commons.v2.ui.pagination.PageCalculator;
import net.exylia.commons.v2.ui.pagination.PaginationTracker;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaginationMenu extends MenuBase {

    private static final long NAVIGATION_DEBOUNCE_MILLIS = 150L;
    private long lastNavigationMillis;

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
        List<Integer> slots = menuData.getPaginationSlots();
        if (slots.isEmpty()) {
            return;
        }

        if (menuData.getPaginationItemsSupplier() != null) {
            menuData.setPaginationItems(menuData.getPaginationItemsSupplier().get());
        }

        int totalPages = getTotalPages();
        int currentPage = getCurrentPage();
        if (currentPage > totalPages) {
            setCurrentPage(Math.max(1, totalPages));
            currentPage = getCurrentPage();
        }

        List<ItemData> allItems = menuData.getPaginationItems();

        int itemsPerPage = slots.size();
        List<ItemData> pageItems = allItems.isEmpty()
                ? List.of()
                : PageCalculator.getPageItems(allItems, currentPage, itemsPerPage);

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

        if (menuData.hasPaginationFiller()) {
            for (int i = pageItems.size(); i < slots.size(); i++) {
                int slot = slots.get(i);
                setItem(slot, menuData.getPaginationFiller());
            }
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

        if (currentPage < totalPages && canNavigate()) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " navigating to next page: " + (currentPage + 1) + "/" + totalPages);
            setCurrentPage(currentPage + 1);
            refresh();
        }
    }

    public void previousPage() {
        int currentPage = getCurrentPage();

        if (currentPage > 1 && canNavigate()) {
            DebugAPI.logLibDebug(DebugCategory.UI, "Menu " + menuId + " navigating to previous page: " + (currentPage - 1) + "/" + getTotalPages());
            setCurrentPage(currentPage - 1);
            refresh();
        }
    }

    private boolean canNavigate() {
        long now = System.currentTimeMillis();
        if (now - lastNavigationMillis < NAVIGATION_DEBOUNCE_MILLIS) {
            return false;
        }
        lastNavigationMillis = now;
        return true;
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
        Map<Integer, ProcessedItem> oldItems = new HashMap<>(itemsBySlot);
        itemsBySlot.clear();
        populateItems();

        if (inventory == null) {
            return;
        }

        AnimationSettings animSettings = menuData.getAnimationSettings();
        if (animSettings != null && animSettings.hasPageAnimation()) {
            AnimationExecutor.executeWithTransition(
                    inventory,
                    oldItems,
                    itemsBySlot,
                    animSettings.getPageAnimation(),
                    animSettings.getSpeed(),
                    animationCancelFlag
            );
        } else {
            inventory.clear();
            itemsBySlot.forEach((slot, item) -> {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item.getItemStack());
                }
            });
        }
        player.updateInventory();

        Tasks.later(() -> {
            if (state.get() == MenuState.OPEN && inventory != null) {
                refreshTitle();
            }
        }, 1L);
    }

    @Override
    protected void refreshTitle() {
        if (inventory == null) {
            return;
        }
        PacketEventsSupport.updateTitle(player, inventory, processPaginationTitle(menuData.getTitle()));
    }

    private net.kyori.adventure.text.Component processPaginationTitle(String title) {
        int currentPage = getCurrentPage();
        int totalPages = getTotalPages();

        PlaceholderContext titleContext = context.copy()
                .put("current_page", currentPage)
                .put("total_pages", totalPages);

        String processedTitle = Placeholders.process(title, player, titleContext);

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
