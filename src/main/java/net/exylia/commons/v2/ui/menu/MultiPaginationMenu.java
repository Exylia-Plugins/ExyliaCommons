package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.ui.animation.AnimationExecutor;
import net.exylia.commons.v2.ui.animation.AnimationSettings;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.NavigationData;
import net.exylia.commons.v2.ui.model.SectionData;
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
import java.util.Objects;

public class MultiPaginationMenu extends MenuBase {

    public MultiPaginationMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected Inventory createInventory() {
        String processedTitle = processTitle(menuData.getTitle());
        return Bukkit.createInventory(null, menuData.getSize(), ColorAPI.parse(processedTitle));
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
                    .merge(itemData.getContext())
                    .put("index", globalIndex)
                    .put("page_index", i)
                    .put("section_name", section.getName())
                    .put("section_page", currentPage)
                    .put("section_total_pages", totalPages)
                    .put("current_page", currentPage)
                    .put("total_pages", totalPages);

            Integer selectedIndex = PaginationTracker.getSelectedIndex(player.getUniqueId(), section.getName());
            boolean isSelected = selectedIndex != null && selectedIndex == globalIndex;

            if (isSelected && section.hasSelectedTemplate()) {
                ItemData selectedTemplate = section.resolveSelectedTemplate();
                setItem(slot, applyTemplate(selectedTemplate, itemData, itemContext));
            } else if (itemData.getTemplateKey() != null && section.hasTemplate(itemData.getTemplateKey())) {
                ItemData template = section.getTemplate(itemData.getTemplateKey());
                setItem(slot, applyTemplate(template, itemData, itemContext));
            } else {
                setItem(slot, itemData.toBuilder().context(itemContext).build());
            }
        }
    }

    private ItemData applyTemplate(ItemData template, ItemData sourceItem, PlaceholderContext itemContext) {
        PlaceholderContext mergedContext = itemContext.copy()
                .merge(template.getContext());

        ItemData.ItemDataBuilder builder = template.toBuilder()
                .context(mergedContext);

        if (!sourceItem.getActions().isEmpty()) {
            builder.actions(sourceItem.getActions());
        }
        if (!sourceItem.getCommands().isEmpty()) {
            builder.commands(sourceItem.getCommands());
        }

        return builder.build();
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

    public void updateSectionItems(String sectionName, List<ItemData> items) {
        SectionData section = getSection(sectionName);
        if (section != null) {
            section.setItems(items);
            if (state.get() == MenuState.OPEN) {
                Tasks.sync(this::refresh);
            }
        }
    }

    public void refresh() {
        Map<Integer, ProcessedItem> oldItems = new HashMap<>(itemsBySlot);
        itemsBySlot.clear();
        populateItemsWithoutDisplay();

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
            for (Integer slot : oldItems.keySet()) {
                if (!itemsBySlot.containsKey(slot) && slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, null);
                }
            }
            itemsBySlot.forEach((slot, item) -> {
                if (slot >= 0 && slot < inventory.getSize()) {
                    ProcessedItem old = oldItems.get(slot);
                    org.bukkit.inventory.ItemStack newStack = item.getItemStack();
                    org.bukkit.inventory.ItemStack oldStack = old != null ? old.getItemStack() : null;
                    if (!Objects.equals(oldStack, newStack)) {
                        inventory.setItem(slot, newStack);
                    }
                }
            });
        }

        refreshTitle();
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
