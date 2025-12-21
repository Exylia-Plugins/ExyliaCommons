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
public class MultiPaginationMenuV2 extends MenuV2 {
    private final Map<String, PaginationSection> sections = new ConcurrentHashMap<>();

    public MultiPaginationMenuV2(String title, int rows) {
        super(UUID.randomUUID().toString(), MenuType.MULTI_PAGINATION);
        this.rawTitle = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
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

        sections.forEach((sectionId, section) -> {
            int currentPage = section.getCurrentPage(viewer);
            int totalPages = section.getTotalPages();

            context.getPlaceholderContext()
                .put("section_" + sectionId + "_page", currentPage)
                .put("section_" + sectionId + "_pages", totalPages)
                .put("section_" + sectionId + "_total_items", section.getItems().size())
                .put("section_" + sectionId + "_items_on_page", section.getItemsOnCurrentPage(viewer));
        });

        context.getPlaceholderContext()
            .put("sections_count", sections.size());
    }

    @Override
    public void populateInventory() {
        if (inventory == null) {
            return;
        }

        items.forEach((slot, item) -> {
            boolean isInSection = false;
            for (PaginationSection section : sections.values()) {
                for (int sectionSlot : section.getItemSlots()) {
                    if (slot == sectionSlot) {
                        isInSection = true;
                        break;
                    }
                }
                if (isInSection) break;
            }

            if (!isInSection) {
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
            }
        });

        sections.values().forEach(this::populateSectionItems);
        sections.values().forEach(this::populateSectionNavigation);
    }

    private void populateSectionItems(PaginationSection section) {
        int currentPage = section.getCurrentPage(viewer);
        int startIndex = (currentPage - 1) * section.getItemsPerPage();
        int endIndex = Math.min(startIndex + section.getItemsPerPage(), section.getItems().size());

        int[] itemSlots = section.getItemSlots();

        for (int i = 0; i < itemSlots.length; i++) {
            int itemIndex = startIndex + i;
            int slot = itemSlots[i];

            if (itemIndex < endIndex) {
                MenuItemV2 item = section.getItems().get(itemIndex).clone();
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
                items.put(slot, item);
            } else if (section.getItemSlotFiller() != null) {
                inventory.setItem(slot, section.getItemSlotFiller().build(viewer, context.getPlaceholderContext()));
            }
        }
    }

    private void populateSectionNavigation(PaginationSection section) {
        int currentPage = section.getCurrentPage(viewer);
        int totalPages = section.getTotalPages();

        if (currentPage > 1 && section.getPreviousButton() != null && section.getPreviousButtonSlot() >= 0) {
            MenuItemV2 prevBtn = section.getPreviousButton().clone();
            prevBtn.addAction(ClickType.LEFT, "ui:section_prev section:" + section.getId());
            inventory.setItem(section.getPreviousButtonSlot(), prevBtn.build(viewer, context.getPlaceholderContext()));
            items.put(section.getPreviousButtonSlot(), prevBtn);
        }

        if (currentPage < totalPages && section.getNextButton() != null && section.getNextButtonSlot() >= 0) {
            MenuItemV2 nextBtn = section.getNextButton().clone();
            nextBtn.addAction(ClickType.LEFT, "ui:section_next section:" + section.getId());
            inventory.setItem(section.getNextButtonSlot(), nextBtn.build(viewer, context.getPlaceholderContext()));
            items.put(section.getNextButtonSlot(), nextBtn);
        }
    }

    public void addSection(PaginationSection section) {
        if (section != null) {
            sections.put(section.getId(), section);

            if (state == MenuState.OPEN) {
                refresh();
            }
        }
    }

    public void removeSection(String sectionId) {
        sections.remove(sectionId);

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public Optional<PaginationSection> getSection(String sectionId) {
        return Optional.ofNullable(sections.get(sectionId));
    }

    public void addItemToSection(String sectionId, MenuItemV2 item) {
        PaginationSection section = sections.get(sectionId);
        if (section != null && item != null) {
            section.addItem(item);

            if (state == MenuState.OPEN) {
                refresh();
            }
        }
    }

    public void addItemsToSection(String sectionId, Collection<MenuItemV2> items) {
        PaginationSection section = sections.get(sectionId);
        if (section != null && items != null) {
            section.addItems(items);

            if (state == MenuState.OPEN) {
                refresh();
            }
        }
    }

    public void clearSection(String sectionId) {
        PaginationSection section = sections.get(sectionId);
        if (section != null) {
            section.clearItems();

            if (state == MenuState.OPEN) {
                refresh();
            }
        }
    }

    public void nextPage(String sectionId) {
        PaginationSection section = sections.get(sectionId);
        if (section != null && viewer != null) {
            section.nextPage(viewer);
            refresh();
        }
    }

    public void previousPage(String sectionId) {
        PaginationSection section = sections.get(sectionId);
        if (section != null && viewer != null) {
            section.previousPage(viewer);
            refresh();
        }
    }

    public void setCurrentPage(String sectionId, int page) {
        PaginationSection section = sections.get(sectionId);
        if (section != null && viewer != null) {
            section.setCurrentPage(viewer, page);
            refresh();
        }
    }

    public int getCurrentPage(String sectionId) {
        PaginationSection section = sections.get(sectionId);
        if (section != null && viewer != null) {
            return section.getCurrentPage(viewer);
        }
        return 1;
    }

    public int getTotalPages(String sectionId) {
        PaginationSection section = sections.get(sectionId);
        if (section != null) {
            return section.getTotalPages();
        }
        return 1;
    }
}
