package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.ui.model.*;
import net.exylia.commons.v2.ui.refresh.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class PaginationMenuV2 extends MenuV2 {
    private final List<MenuItemV2> paginationItems = new ArrayList<>();
    private final int[] itemSlots;
    private final int itemsPerPage;

    private MenuItemV2 previousButton;
    private MenuItemV2 nextButton;
    private int previousButtonSlot = 48;
    private int nextButtonSlot = 50;

    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private MenuItemV2 itemSlotFiller;

    public PaginationMenuV2(String title, int rows, int[] itemSlots) {
        super(UUID.randomUUID().toString(), MenuType.PAGINATION);
        this.rawTitle = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
        this.itemSlots = itemSlots.clone();
        this.itemsPerPage = itemSlots.length;
        initializeRefreshStrategy();
    }

    private void initializeRefreshStrategy() {
        this.refreshStrategy = switch (refreshMode) {
            case DISABLED -> new DisabledRefreshStrategy();
            case SLOT_ONLY -> new SlotOnlyRefreshStrategy();
            case SMART -> new SmartRefreshStrategy();
            case FULL -> new FullRefreshStrategy();
        };
    }

    @Override
    public void open(Player player, MenuContext context) {
        this.viewer = player;
        this.context = context != null ? context : MenuContext.create(player);

        enrichContext();
        processTitle();
        createInventory();
        applyFillers();
        populateInventory();

        player.openInventory(inventory);
        this.state = MenuState.OPEN;

        handleOpen();

        if (dynamicUpdates) {
            startDynamicUpdates();
        }
    }

    @Override
    protected void enrichContext() {
        super.enrichContext();

        int currentPage = getCurrentPage(viewer);
        int totalPages = getTotalPages();

        context.getPlaceholderContext()
            .put("page", currentPage)
            .put("pages", totalPages)
            .put("total_items", paginationItems.size())
            .put("items_on_page", getItemsOnCurrentPage());
    }

    @Override
    public void populateInventory() {
        if (inventory == null) {
            return;
        }

        items.forEach((slot, item) -> {
            boolean isItemSlot = false;
            for (int iSlot : itemSlots) {
                if (slot == iSlot) {
                    isItemSlot = true;
                    break;
                }
            }

            if (!isItemSlot) {
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
            }
        });

        populatePageItems();
        populateNavigation();
    }

    private void populatePageItems() {
        int currentPage = getCurrentPage(viewer);
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, paginationItems.size());

        for (int i = 0; i < itemSlots.length; i++) {
            int itemIndex = startIndex + i;
            int slot = itemSlots[i];

            if (itemIndex < endIndex) {
                MenuItemV2 item = paginationItems.get(itemIndex).clone();
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
                items.put(slot, item);
            } else if (itemSlotFiller != null) {
                inventory.setItem(slot, itemSlotFiller.build(viewer, context.getPlaceholderContext()));
            }
        }
    }

    private void populateNavigation() {
        int currentPage = getCurrentPage(viewer);
        int totalPages = getTotalPages();

        if (currentPage > 1 && previousButton != null) {
            MenuItemV2 prevBtn = previousButton.clone();
            prevBtn.addAction(ClickType.LEFT, "ui:previous_page");
            inventory.setItem(previousButtonSlot, prevBtn.build(viewer, context.getPlaceholderContext()));
            items.put(previousButtonSlot, prevBtn);
        }

        if (currentPage < totalPages && nextButton != null) {
            MenuItemV2 nextBtn = nextButton.clone();
            nextBtn.addAction(ClickType.LEFT, "ui:next_page");
            inventory.setItem(nextButtonSlot, nextBtn.build(viewer, context.getPlaceholderContext()));
            items.put(nextButtonSlot, nextBtn);
        }
    }

    public void addItem(MenuItemV2 item) {
        paginationItems.add(item);

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public void addItems(Collection<MenuItemV2> items) {
        paginationItems.addAll(items);

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public void clearPaginationItems() {
        paginationItems.clear();

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public void nextPage(Player player) {
        int current = getCurrentPage(player);
        if (current < getTotalPages()) {
            setCurrentPage(player, current + 1);
            refresh();
        }
    }

    public void previousPage(Player player) {
        int current = getCurrentPage(player);
        if (current > 1) {
            setCurrentPage(player, current - 1);
            refresh();
        }
    }

    public int getCurrentPage(Player player) {
        if (player == null) {
            return 1;
        }
        return playerPages.getOrDefault(player.getUniqueId(), 1);
    }

    public void setCurrentPage(Player player, int page) {
        if (player != null) {
            playerPages.put(player.getUniqueId(), Math.max(1, Math.min(page, getTotalPages())));
        }
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) paginationItems.size() / itemsPerPage));
    }

    private int getItemsOnCurrentPage() {
        int currentPage = getCurrentPage(viewer);
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, paginationItems.size());
        return endIndex - startIndex;
    }

    public PaginationMenuV2 setPreviousButton(MenuItemV2 button, int slot) {
        this.previousButton = button;
        this.previousButtonSlot = slot;
        return this;
    }

    public PaginationMenuV2 setNextButton(MenuItemV2 button, int slot) {
        this.nextButton = button;
        this.nextButtonSlot = slot;
        return this;
    }

    public PaginationMenuV2 setItemSlotFiller(MenuItemV2 filler) {
        this.itemSlotFiller = filler;
        return this;
    }
}
