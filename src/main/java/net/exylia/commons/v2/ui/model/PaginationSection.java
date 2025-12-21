package net.exylia.commons.v2.ui.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class PaginationSection {
    private final String id;
    private final List<MenuItemV2> items = new ArrayList<>();
    private final int[] itemSlots;
    private final int itemsPerPage;

    private MenuItemV2 previousButton;
    private MenuItemV2 nextButton;
    private int previousButtonSlot = -1;
    private int nextButtonSlot = -1;

    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private MenuItemV2 itemSlotFiller;

    public PaginationSection(String id, int[] itemSlots) {
        this.id = id;
        this.itemSlots = itemSlots != null ? itemSlots.clone() : new int[0];
        this.itemsPerPage = this.itemSlots.length;
    }

    public void addItem(MenuItemV2 item) {
        if (item != null) {
            items.add(item);
        }
    }

    public void addItems(Collection<MenuItemV2> items) {
        if (items != null) {
            this.items.addAll(items);
        }
    }

    public void clearItems() {
        items.clear();
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
        return Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
    }

    public void nextPage(Player player) {
        int current = getCurrentPage(player);
        if (current < getTotalPages()) {
            setCurrentPage(player, current + 1);
        }
    }

    public void previousPage(Player player) {
        int current = getCurrentPage(player);
        if (current > 1) {
            setCurrentPage(player, current - 1);
        }
    }

    public int getItemsOnCurrentPage(Player player) {
        int currentPage = getCurrentPage(player);
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        return endIndex - startIndex;
    }

    public List<MenuItemV2> getItemsForPage(Player player) {
        int currentPage = getCurrentPage(player);
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        if (startIndex >= items.size()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(items.subList(startIndex, endIndex));
    }

    public PaginationSection setPreviousButton(MenuItemV2 button, int slot) {
        this.previousButton = button;
        this.previousButtonSlot = slot;
        return this;
    }

    public PaginationSection setNextButton(MenuItemV2 button, int slot) {
        this.nextButton = button;
        this.nextButtonSlot = slot;
        return this;
    }

    public PaginationSection setItemSlotFiller(MenuItemV2 filler) {
        this.itemSlotFiller = filler;
        return this;
    }
}
