package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.model.SectionData;
import net.exylia.commons.v2.ui.pagination.PageCalculator;
import net.exylia.commons.v2.ui.pagination.PaginationTracker;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class MultiPaginationMenu extends MenuBase {

    public MultiPaginationMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menuData.getSize(), processTitle(menuData.getTitle()));
    }

    @Override
    protected void populateItems() {
        applyFillers();
        applySections();
        applyStaticItems();
    }

    @Override
    protected void handleClickInternal(int slot, ClickType clickType) {
        for (SectionData section : menuData.getSections()) {
            NavigationData nav = section.getNavigation();
            if (nav == null) {
                continue;
            }

            if (nav.hasPreviousButton() && slot == nav.getPreviousButtonSlot()) {
                previousPage(section.getName());
                return;
            } else if (nav.hasNextButton() && slot == nav.getNextButtonSlot()) {
                nextPage(section.getName());
                return;
            }
        }
    }

    private void applySections() {
        if (!menuData.hasSections()) {
            return;
        }

        for (SectionData section : menuData.getSections()) {
            applySection(section);
        }
    }

    private void applySection(SectionData section) {
        applySectionFiller(section);
        applySectionItems(section);
        applySectionNavigation(section);
    }

    private void applySectionFiller(SectionData section) {
        if (!section.hasFillerItem()) {
            return;
        }

        for (int slot : section.getSlots()) {
            setItem(slot, section.getFillerItem());
        }
    }

    private void applySectionItems(SectionData section) {
        int currentPage = getSectionPage(section.getName());
        List<Integer> slots = section.getSlots();
        List<ItemData> allItems = section.getItems();

        if (allItems.isEmpty() || slots.isEmpty()) {
            return;
        }

        int itemsPerPage = slots.size();
        List<ItemData> pageItems = PageCalculator.getPageItems(allItems, currentPage, itemsPerPage);

        for (int i = 0; i < pageItems.size() && i < slots.size(); i++) {
            int slot = slots.get(i);
            ItemData itemData = pageItems.get(i);

            int globalIndex = PageCalculator.getStartIndex(currentPage, itemsPerPage) + i;
            int totalPages = section.getTotalPages(allItems.size());

            PlaceholderContext itemContext = context.copy()
                    .put("index", globalIndex)
                    .put("page_index", i)
                    .put("section_name", section.getName())
                    .put("section_page", currentPage)
                    .put("section_total_pages", totalPages)
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData enhancedItemData = itemData.toBuilder()
                    .context(itemContext)
                    .build();

            Integer selectedIndex = PaginationTracker.getSelectedIndex(player.getUniqueId(), section.getName());
            if (selectedIndex != null && selectedIndex == globalIndex && section.hasSelectedTemplate()) {
                ItemData selectedTemplate = section.getSelectedItemTemplate().toBuilder()
                        .context(itemContext)
                        .build();

                setItem(slot, selectedTemplate);
            } else {
                setItem(slot, enhancedItemData);
            }
        }
    }

    private void applySectionNavigation(SectionData section) {
        NavigationData nav = section.getNavigation();
        if (nav == null) {
            return;
        }

        int currentPage = getSectionPage(section.getName());
        int totalPages = section.getTotalPages(section.getItems().size());

        if (currentPage > 1 && nav.hasPreviousButton()) {
            PlaceholderContext navContext = context.copy()
                    .put("section_name", section.getName())
                    .put("section_page", currentPage)
                    .put("section_total_pages", totalPages)
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData prevButtonData = nav.getPreviousButton().toBuilder()
                    .context(navContext)
                    .build();

            setItem(nav.getPreviousButtonSlot(), prevButtonData);
        }

        if (currentPage < totalPages && nav.hasNextButton()) {
            PlaceholderContext navContext = context.copy()
                    .put("section_name", section.getName())
                    .put("section_page", currentPage)
                    .put("section_total_pages", totalPages)
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData nextButtonData = nav.getNextButton().toBuilder()
                    .context(navContext)
                    .build();

            setItem(nav.getNextButtonSlot(), nextButtonData);
        }

        if (nav.hasInfoItem()) {
            PlaceholderContext infoContext = context.copy()
                    .put("section_name", section.getName())
                    .put("section_page", currentPage)
                    .put("section_total_pages", totalPages)
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            ItemData infoItemData = nav.getInfoItem().toBuilder()
                    .context(infoContext)
                    .build();

            setItem(nav.getInfoItemSlot(), infoItemData);
        }
    }

    public void nextPage(String sectionName) {
        SectionData section = getSection(sectionName);
        if (section == null) {
            return;
        }

        int currentPage = getSectionPage(sectionName);
        int totalPages = section.getTotalPages(section.getItems().size());

        if (currentPage < totalPages) {
            setSectionPage(sectionName, currentPage + 1);
            refresh();
        }
    }

    public void previousPage(String sectionName) {
        SectionData section = getSection(sectionName);
        if (section == null) {
            return;
        }

        int currentPage = getSectionPage(sectionName);

        if (currentPage > 1) {
            setSectionPage(sectionName, currentPage - 1);
            refresh();
        }
    }

    public void setPage(String sectionName, int page) {
        SectionData section = getSection(sectionName);
        if (section == null) {
            return;
        }

        int totalPages = section.getTotalPages(section.getItems().size());
        page = PageCalculator.clampPage(page, totalPages);
        setSectionPage(sectionName, page);
        refresh();
    }

    public void setSelectedIndex(String sectionName, Integer index) {
        PaginationTracker.setSelectedIndex(player.getUniqueId(), sectionName, index);
        refresh();
    }

    private void refresh() {
        itemsBySlot.clear();
        populateItems();
        updateInventoryDisplay();

        if (inventory != null) {
            Inventory newInventory = Bukkit.createInventory(null, menuData.getSize(), processTitle(menuData.getTitle()));
            itemsBySlot.forEach((slot, item) -> {
                if (slot >= 0 && slot < newInventory.getSize()) {
                    newInventory.setItem(slot, item.getItemStack());
                }
            });

            player.openInventory(newInventory);
            this.inventory = newInventory;
        }
    }

    private net.kyori.adventure.text.Component processTitle(String title) {
        return ColorAPI.parse(title);
    }

    private int getSectionPage(String sectionName) {
        return PaginationTracker.getSectionPage(player.getUniqueId(), sectionName);
    }

    private void setSectionPage(String sectionName, int page) {
        PaginationTracker.setSectionPage(player.getUniqueId(), sectionName, page);
    }

    private SectionData getSection(String name) {
        return menuData.getSections().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        PaginationTracker.clearPlayer(player.getUniqueId());
    }
}
