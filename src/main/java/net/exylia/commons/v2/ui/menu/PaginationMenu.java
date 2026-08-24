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
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.packet.PacketEventsSupport;
import net.exylia.commons.v2.ui.pagination.PageCalculator;
import net.exylia.commons.v2.ui.pagination.PaginationTracker;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PaginationMenu extends MenuBase {

    private static final long NAVIGATION_DEBOUNCE_MILLIS = 150L;
    private long lastNavigationMillis;

    public PaginationMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menuData.getSize(), processPaginationTitle());
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

        PlaceholderContext basePageContext = context.copy()
                .put("current_page", currentPage)
                .put("total_pages", totalPages);

        int startIndex = PageCalculator.getStartIndex(currentPage, itemsPerPage);

        for (int i = 0; i < pageItems.size() && i < slots.size(); i++) {
            int slot = slots.get(i);
            ItemData itemData = pageItems.get(i);

            PlaceholderContext itemContext = basePageContext.copy()
                    .put("index", startIndex + i)
                    .put("page_index", i);

            PlaceholderContext mergedContext = itemData.getContext() != null
                    ? itemContext.copyAndMerge(itemData.getContext())
                    : itemContext;

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

        PlaceholderContext navContext = context.copy()
                .put("current_page", currentPage)
                .put("total_pages", totalPages);

        if (currentPage > 1 && nav.hasPreviousButton()) {
            setItem(nav.getPreviousButtonSlot(), nav.getPreviousButton().toBuilder().context(navContext).build());
        }

        if (currentPage < totalPages && nav.hasNextButton()) {
            setItem(nav.getNextButtonSlot(), nav.getNextButton().toBuilder().context(navContext).build());
        }

        if (nav.hasInfoItem()) {
            setItem(nav.getInfoItemSlot(), nav.getInfoItem().toBuilder().context(navContext).build());
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
        AnimationSettings animSettings = menuData.getAnimationSettings();
        boolean hasPageAnimation = animSettings != null && animSettings.hasPageAnimation();

        Map<Integer, ProcessedItem> oldItems = new HashMap<>(itemsBySlot);
        itemsBySlot.clear();
        populateItemsWithoutDisplay();

        if (inventory == null) {
            return;
        }

        if (hasPageAnimation) {
            AnimationExecutor.executeWithTransition(
                    player,
                    inventory,
                    oldItems,
                    itemsBySlot,
                    animSettings.getPageAnimation(),
                    animSettings.getSpeed(),
                    animationCancelFlag
            );
        } else {
            for (Integer slot : oldItems.keySet()) {
                if (!itemsBySlot.containsKey(slot) && slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, null);
                }
            }
            itemsBySlot.forEach((slot, item) -> {
                if (slot >= 0 && slot < inventory.getSize()) {
                    ProcessedItem old = oldItems.get(slot);
                    ItemStack newStack = item.getItemStack();
                    ItemStack oldStack = old != null ? old.getItemStack() : null;
                    if (!Objects.equals(oldStack, newStack)) {
                        inventory.setItem(slot, newStack);
                    }
                }
            });
        }

        refreshTitle();
    }

    @Override
    protected String computeCurrentTitle() {
        return processRawPaginationTitle();
    }

    @Override
    protected void refreshTitle() {
        if (inventory == null) {
            return;
        }
        String raw = processRawPaginationTitle();
        if (raw.equals(lastRenderedTitle)) {
            return;
        }
        lastRenderedTitle = raw;
        PacketEventsSupport.updateTitle(player, inventory, ColorAPI.parse(raw));
    }

    private String processRawPaginationTitle() {
        int currentPage = getCurrentPage();
        int totalPages = getTotalPages();

        PlaceholderContext titleContext = context.copy()
                .put("current_page", currentPage)
                .put("total_pages", totalPages);

        return Placeholders.process(menuData.getTitle(), player, titleContext)
                .replace("%current_page%", String.valueOf(currentPage))
                .replace("%total_pages%", String.valueOf(totalPages));
    }

    private net.kyori.adventure.text.Component processPaginationTitle() {
        return ColorAPI.parse(processRawPaginationTitle());
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
