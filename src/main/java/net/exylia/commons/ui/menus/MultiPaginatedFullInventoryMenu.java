package net.exylia.commons.ui.menus;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MultiPaginatedFullInventoryMenu extends FullInventoryMenu {

    private final Map<String, PaginationSection> sections = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerPages = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Map<Integer, Integer>>> playerItemTasks = new ConcurrentHashMap<>();

    private BiConsumer<String, Integer> onSectionUpdate;
    private Consumer<Player> externalCloseHandler;

    public MultiPaginatedFullInventoryMenu(String title, int rows) {
        super(title, rows);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    public MultiPaginatedFullInventoryMenu(String title, int rows, ExyliaContext context) {
        super(title, rows, context);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    public static class PaginationSection {
        private final String name;
        private final List<MenuItem> allItems = new ArrayList<>();
        private final List<MenuItem> filteredItems = new ArrayList<>();
        private final int[] slots;
        private final int itemsPerPage;

        private MenuItem previousButton;
        private MenuItem nextButton;
        private int previousButtonSlot = -1;
        private int nextButtonSlot = -1;

        private MenuItem fillerItem;
        private MenuItem selectedItemTemplate;
        private BiFunction<MenuClickEvent, Integer, Boolean> onItemSelect;
        private Consumer<Integer> onPageChange;
        private Integer selectedIndex = null;

        private Predicate<MenuItem> currentFilter = null;
        private boolean filterDirty = true;

        public PaginationSection(String name, int... slots) {
            this.name = name;
            this.slots = slots.clone();
            this.itemsPerPage = slots.length;
        }

        public PaginationSection addItem(MenuItem item) {
            allItems.add(item);
            filterDirty = true;
            return this;
        }

        public PaginationSection addItems(Collection<MenuItem> items) {
            this.allItems.addAll(items);
            filterDirty = true;
            return this;
        }

        public PaginationSection setItems(Collection<MenuItem> newItems) {
            allItems.clear();
            allItems.addAll(newItems);
            selectedIndex = null;
            filterDirty = true;
            return this;
        }

        public PaginationSection clearItems() {
            allItems.clear();
            filteredItems.clear();
            selectedIndex = null;
            filterDirty = false;
            return this;
        }

        public PaginationSection updateItem(int index, MenuItem item) {
            if (index >= 0 && index < allItems.size()) {
                allItems.set(index, item);
                filterDirty = true;
            }
            return this;
        }

        public PaginationSection removeItem(int index) {
            if (index >= 0 && index < allItems.size()) {
                allItems.remove(index);
                adjustSelectionAfterRemoval(index);
                filterDirty = true;
            }
            return this;
        }

        public PaginationSection setFilter(Predicate<MenuItem> filter) {
            this.currentFilter = filter;
            this.filterDirty = true;
            this.selectedIndex = null;
            return this;
        }

        public PaginationSection clearFilter() {
            return setFilter(null);
        }

        private void applyFilter() {
            if (!filterDirty) return;

            filteredItems.clear();
            if (currentFilter == null) {
                filteredItems.addAll(allItems);
            } else {
                allItems.stream()
                        .filter(currentFilter)
                        .forEach(filteredItems::add);
            }
            filterDirty = false;
        }

        public List<MenuItem> getVisibleItems() {
            applyFilter();
            return new ArrayList<>(filteredItems);
        }

        public PaginationSection setPreviousButton(MenuItem button, int slot) {
            this.previousButton = button;
            this.previousButtonSlot = slot;
            return this;
        }

        public PaginationSection setNextButton(MenuItem button, int slot) {
            this.nextButton = button;
            this.nextButtonSlot = slot;
            return this;
        }

        public PaginationSection setFillerItem(MenuItem filler) {
            this.fillerItem = filler;
            return this;
        }

        public PaginationSection setSelectedItemTemplate(MenuItem template) {
            this.selectedItemTemplate = template;
            return this;
        }

        public PaginationSection setOnItemSelect(BiFunction<MenuClickEvent, Integer, Boolean> handler) {
            this.onItemSelect = handler;
            return this;
        }

        public PaginationSection setOnPageChange(Consumer<Integer> handler) {
            this.onPageChange = handler;
            return this;
        }

        public PaginationSection setSelectedIndex(Integer index) {
            this.selectedIndex = index;
            return this;
        }

        public Integer getSelectedIndex() {
            return selectedIndex;
        }

        public MenuItem getSelectedItem() {
            applyFilter();
            return selectedIndex != null && selectedIndex < filteredItems.size()
                    ? filteredItems.get(selectedIndex) : null;
        }

        public int getTotalPages() {
            applyFilter();
            return Math.max(1, (int) Math.ceil((double) filteredItems.size() / itemsPerPage));
        }

        public List<MenuItem> getItemsForPage(int page) {
            applyFilter();
            int start = (page - 1) * itemsPerPage;
            int end = Math.min(start + itemsPerPage, filteredItems.size());

            if (start >= filteredItems.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(filteredItems.subList(start, end));
        }

        public boolean isItemSelected(int filteredIndex) {
            return selectedIndex != null && selectedIndex.equals(filteredIndex);
        }

        private void adjustSelectionAfterRemoval(int removedIndex) {
            if (selectedIndex != null) {
                if (selectedIndex.equals(removedIndex)) {
                    selectedIndex = null;
                } else if (selectedIndex > removedIndex) {
                    selectedIndex--;
                }
            }
        }

        public String getName() { return name; }
        public List<MenuItem> getAllItems() { return new ArrayList<>(allItems); }
        public int getItemCount() {
            applyFilter();
            return filteredItems.size();
        }
        public int getTotalItemCount() { return allItems.size(); }
        public int[] getSlots() { return slots.clone(); }
        public int getItemsPerPage() { return itemsPerPage; }
        public boolean hasFilter() { return currentFilter != null; }

        MenuItem getPreviousButton() { return previousButton; }
        MenuItem getNextButton() { return nextButton; }
        int getPreviousButtonSlot() { return previousButtonSlot; }
        int getNextButtonSlot() { return nextButtonSlot; }
        MenuItem getFillerItem() { return fillerItem; }
        MenuItem getSelectedItemTemplate() { return selectedItemTemplate; }
        BiFunction<MenuClickEvent, Integer, Boolean> getOnItemSelect() { return onItemSelect; }
        Consumer<Integer> getOnPageChange() { return onPageChange; }
    }

    public PaginationSection addSection(String name, int... slots) {
        PaginationSection section = new PaginationSection(name, slots);
        sections.put(name, section);
        return section;
    }

    public PaginationSection getSection(String name) {
        return sections.get(name);
    }

    public MultiPaginatedFullInventoryMenu removeSection(String name) {
        sections.remove(name);
        return this;
    }

    public Set<String> getSectionNames() {
        return new HashSet<>(sections.keySet());
    }

    public MultiPaginatedFullInventoryMenu setOnSectionUpdate(BiConsumer<String, Integer> handler) {
        this.onSectionUpdate = handler;
        return this;
    }

    public int getCurrentPage(Player player, String sectionName) {
        Map<String, Integer> pages = playerPages.get(player.getUniqueId());
        return pages != null ? pages.getOrDefault(sectionName, 1) : 1;
    }

    public MultiPaginatedFullInventoryMenu setCurrentPage(Player player, String sectionName, int page) {
        PaginationSection section = sections.get(sectionName);
        if (section != null) {
            int maxPages = section.getTotalPages();
            page = Math.max(1, Math.min(page, maxPages));

            playerPages.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(sectionName, page);

            if (onSectionUpdate != null) {
                onSectionUpdate.accept(sectionName, page);
            }

            if (section.getOnPageChange() != null) {
                section.getOnPageChange().accept(page);
            }
        }
        return this;
    }

    public MultiPaginatedFullInventoryMenu nextPage(Player player, String sectionName) {
        int currentPage = getCurrentPage(player, sectionName);
        PaginationSection section = sections.get(sectionName);

        if (section != null && currentPage < section.getTotalPages()) {
            setCurrentPage(player, sectionName, currentPage + 1);

            if (viewer == player && isOpen()) {
                refreshSectionSynchronous(player, sectionName);
            }
        }
        return this;
    }

    public MultiPaginatedFullInventoryMenu previousPage(Player player, String sectionName) {
        int currentPage = getCurrentPage(player, sectionName);

        if (currentPage > 1) {
            setCurrentPage(player, sectionName, currentPage - 1);

            if (viewer == player && isOpen()) {
                refreshSectionSynchronous(player, sectionName);
            }
        }
        return this;
    }

    private void refreshSectionSynchronous(Player player, String sectionName) {
        PaginationSection section = sections.get(sectionName);
        if (section != null) {
            int currentPage = getCurrentPage(player, sectionName);
            processSectionForPlayer(player, section, currentPage);
            updateInventoryDisplay();
        }
    }

    @Override
    public void open(Player player, ExyliaContext context) {
        cleanupPlayerResources(player);
        initializePlayerPages(player);

        super.open(player, context);

        updateAllSectionsSynchronous(player);
    }

    private void updateAllSectionsSynchronous(Player player) {
        if (viewer != player || !isOpen()) {
            return;
        }

        try {
            super.applyFillers();

            Map<String, Integer> pages = playerPages.get(player.getUniqueId());
            if (pages != null) {
                sections.forEach((sectionName, section) -> {
                    int currentPage = pages.getOrDefault(sectionName, 1);
                    processSectionForPlayer(player, section, currentPage);
                });
            }

            updateInventoryDisplay();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void forceRefreshAllSections() {
        if (viewer != null && isOpen()) {
            updateAllSectionsSynchronous(viewer);
        }
    }

    public void refreshAllSections() {
        if (viewer != null && isOpen()) {
            updateAllSectionsAsync(viewer);
        }
    }

    public void refreshSectionWithReset(String sectionName) {
        if (viewer != null && isOpen()) {
            setCurrentPage(viewer, sectionName, 1);
            refreshSectionSynchronous(viewer, sectionName);
        }
    }

    private void initializePlayerPages(Player player) {
        Map<String, Integer> pages = new ConcurrentHashMap<>();
        sections.keySet().forEach(sectionName -> pages.put(sectionName, 1));
        playerPages.put(player.getUniqueId(), pages);
    }

    private CompletableFuture<Void> updateAllSectionsAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                super.applyFillers();

                Map<String, Integer> pages = playerPages.get(player.getUniqueId());
                if (pages != null) {
                    sections.forEach((sectionName, section) -> {
                        int currentPage = pages.getOrDefault(sectionName, 1);
                        processSectionForPlayer(player, section, currentPage);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory != null && viewer == player) {
                    updateInventoryDisplay();
                }
            });
        });
    }

    private void placeSectionItems(Player player, PaginationSection section, int currentPage) {
        List<MenuItem> pageItems = section.getItemsForPage(currentPage);
        int[] slots = section.getSlots();

        for (int i = 0; i < pageItems.size() && i < slots.length; i++) {
            int slot = slots[i];
            MenuItem item = pageItems.get(i).clone();
            int filteredIndex = (currentPage - 1) * section.getItemsPerPage() + i;

            if (section.isItemSelected(filteredIndex) && section.getSelectedItemTemplate() != null) {
                MenuItem selectedTemplate = section.getSelectedItemTemplate().clone();
                if (item.getContext() != null && !item.getContext().isEmpty()) {
                    selectedTemplate.mergeContext(item.getContext());
                }
                item = selectedTemplate;
            }

            ExyliaContext combinedContext = prepareItemContext(item);

            item.withContext(combinedContext);
            item.process(player);

            setupItemClickHandler(item, section, filteredIndex);
            items.put(slot, item);
        }
    }

    @Override
    protected ExyliaContext prepareItemContext(MenuItem item) {
        ExyliaContext combinedContext = this.context != null ? this.context.createChild() : ExyliaContext.create();

        if (item.getContext() != null && !item.getContext().isEmpty()) {
            combinedContext.merge(item.getContext());
        }

        return combinedContext;
    }

    private void placeSectionNavigation(Player player, PaginationSection section, int currentPage) {
        String sectionName = section.getName();
        int totalPages = section.getTotalPages();

        if (currentPage > 1 && section.getPreviousButton() != null && section.getPreviousButtonSlot() != -1) {
            MenuItem prevButton = section.getPreviousButton().clone();

            ExyliaContext buttonContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
            buttonContext.put("current_page", currentPage);
            buttonContext.put("total_pages", totalPages);
            buttonContext.put("section_name", sectionName);

            prevButton.withContext(buttonContext);
            prevButton.process(player);

            prevButton.setClickHandler(event -> {
                previousPage(event.getPlayer(), sectionName);
            });

            items.put(section.getPreviousButtonSlot(), prevButton);
        } else {
            if (section.getPreviousButtonSlot() != -1) {
                items.remove(section.getPreviousButtonSlot());

                MenuItem filler = getEffectiveFillerForSlot(section.getPreviousButtonSlot());
                if (filler != null) {
                    MenuItem fillerClone = filler.clone();
                    ExyliaContext fillerContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
                    fillerClone.withContext(fillerContext);
                    fillerClone.process(player);
                    items.put(section.getPreviousButtonSlot(), fillerClone);
                }
            }
        }

        if (currentPage < totalPages && section.getNextButton() != null && section.getNextButtonSlot() != -1) {
            MenuItem nextButton = section.getNextButton().clone();

            ExyliaContext buttonContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
            buttonContext.put("current_page", currentPage);
            buttonContext.put("total_pages", totalPages);
            buttonContext.put("section_name", sectionName);

            nextButton.withContext(buttonContext);
            nextButton.process(player);

            nextButton.setClickHandler(event -> {
                nextPage(event.getPlayer(), sectionName);
            });

            items.put(section.getNextButtonSlot(), nextButton);
        } else {
            if (section.getNextButtonSlot() != -1) {
                items.remove(section.getNextButtonSlot());

                MenuItem filler = getEffectiveFillerForSlot(section.getNextButtonSlot());
                if (filler != null) {
                    MenuItem fillerClone = filler.clone();
                    ExyliaContext fillerContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
                    fillerClone.withContext(fillerContext);
                    fillerClone.process(player);
                    items.put(section.getNextButtonSlot(), fillerClone);
                }
            }
        }
    }

    private void processSectionForPlayer(Player player, PaginationSection section, int currentPage) {
        applySectionFiller(player, section);
        placeSectionItems(player, section, currentPage);
        placeSectionNavigation(player, section, currentPage);
    }

    public void forceRefreshNavigation() {
        if (viewer != null && isOpen()) {
            Map<String, Integer> pages = playerPages.get(viewer.getUniqueId());
            if (pages != null) {
                sections.forEach((sectionName, section) -> {
                    int currentPage = pages.getOrDefault(sectionName, 1);
                    placeSectionNavigation(viewer, section, currentPage);
                });
                updateInventoryDisplay();
            }
        }
    }

    private MenuItem getEffectiveFillerForSlot(int slot) {
        if (borderFiller != null && isBorderSlot(slot)) {
            return borderFiller;
        }
        return globalFiller;
    }

    private void applySectionFiller(Player player, PaginationSection section) {
        if (section.getFillerItem() == null) return;

        for (int slot : section.getSlots()) {
            MenuItem filler = section.getFillerItem().clone();
            ExyliaContext fillerContext = this.context != null ? this.context.createChild() : ExyliaContext.create();
            filler.withContext(fillerContext);
            filler.process(player);
            items.put(slot, filler);
        }
    }

    private void setupItemClickHandler(MenuItem item, PaginationSection section, int filteredIndex) {
        Consumer<MenuClickEvent> originalHandler = item.getClickHandler();

        item.setClickHandler(event -> {
            try {
                if (originalHandler != null) {
                    originalHandler.accept(event);
                }

                boolean shouldSelect = true;
                if (section.getOnItemSelect() != null) {
                    shouldSelect = section.getOnItemSelect().apply(event, filteredIndex);
                }

                if (shouldSelect) {
                    section.setSelectedIndex(filteredIndex);
                } else {
                    return;
                }

                if (viewer != null && isOpen()) {
                    refreshSectionSynchronous(event.getPlayer(), section.getName());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateInventoryDisplay() {
        if (inventory == null) {
            return;
        }

        AtomicInteger itemsSet = new AtomicInteger();
        items.forEach((slot, item) -> {
            if (item != null) {
                inventory.setItem(slot, item.build());
                itemsSet.getAndIncrement();
            } else {
                inventory.setItem(slot, null);
            }
        });
    }

    private void onPlayerCloseMenu(Player player) {
        cleanupPlayerResources(player);
        if (externalCloseHandler != null) {
            externalCloseHandler.accept(player);
        }
    }

    private void cleanupPlayerResources(Player player) {
        UUID playerId = player.getUniqueId();
        playerPages.remove(playerId);

        Map<String, Map<Integer, Integer>> sectionTasks = playerItemTasks.remove(playerId);
        if (sectionTasks != null) {
            sectionTasks.values().forEach(itemTasks ->
                    itemTasks.values().forEach(taskId -> {
                        if (taskId != null && taskId != -1) {
                            Bukkit.getScheduler().cancelTask(taskId);
                        }
                    })
            );
        }
    }

    @Override
    public MultiPaginatedFullInventoryMenu setCloseHandler(Consumer<Player> closeHandler) {
        this.externalCloseHandler = closeHandler;
        super.setCloseHandler(this::onPlayerCloseMenu);
        return this;
    }

    @Override
    public MultiPaginatedFullInventoryMenu enableDynamicUpdates(JavaPlugin plugin, long tickInterval) {
        super.enableDynamicUpdates(plugin, tickInterval);
        return this;
    }

    @Override
    public MultiPaginatedFullInventoryMenu disableDynamicUpdates() {
        super.disableDynamicUpdates();

        playerItemTasks.values().forEach(sectionTasks ->
                sectionTasks.values().forEach(itemTasks ->
                        itemTasks.values().forEach(taskId -> {
                            if (taskId != null && taskId != -1) {
                                Bukkit.getScheduler().cancelTask(taskId);
                            }
                        })
                )
        );
        playerItemTasks.clear();

        return this;
    }

    @Override
    public MultiPaginatedFullInventoryMenu setGlobalFiller(MenuItem filler) {
        super.setGlobalFiller(filler);
        return this;
    }

    @Override
    public MultiPaginatedFullInventoryMenu setBorderFiller(MenuItem borderItem) {
        super.setBorderFiller(borderItem);
        return this;
    }
}
