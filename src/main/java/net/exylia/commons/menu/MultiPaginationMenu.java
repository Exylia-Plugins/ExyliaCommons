package net.exylia.commons.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Menú optimizado con múltiples secciones paginables independientes
 */
@Deprecated
public class MultiPaginationMenu extends Menu {

    /**
     * Sección paginable optimizada
     */
    public static class PaginationSection {
        private final String name;
        private final List<MenuItem> items;
        private final int[] slots;
        private int currentPage = 1;

        // Botones de navegación
        private MenuItem previousButton;
        private MenuItem nextButton;
        private int previousButtonSlot;
        private int nextButtonSlot;

        // Configuración de la sección
        private MenuItem fillerItem;
        private BiConsumer<MenuClickInfo, Integer> onItemSelect;
        private Integer selectedIndex = null;
        private MenuItem selectedItemTemplate;

        public PaginationSection(String name, int[] slots) {
            this.name = name;
            this.items = new ArrayList<>();
            this.slots = slots.clone(); // Defensive copy
        }

        // ==================== CONFIGURACIÓN DE ITEMS ====================

        public PaginationSection addItem(MenuItem item) {
            items.add(item);
            return this;
        }

        public PaginationSection addItems(List<MenuItem> items) {
            this.items.addAll(items);
            return this;
        }

        public PaginationSection setItems(List<MenuItem> newItems) {
            items.clear();
            items.addAll(newItems);
            currentPage = 1;
            selectedIndex = null;
            return this;
        }

        public PaginationSection clearItems() {
            items.clear();
            currentPage = 1;
            selectedIndex = null;
            return this;
        }

        public PaginationSection removeItem(int index) {
            if (index >= 0 && index < items.size()) {
                items.remove(index);
                adjustSelectionAfterRemoval(index);
            }
            return this;
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

        // ==================== CONFIGURACIÓN DE NAVEGACIÓN ====================

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

        // ==================== CONFIGURACIÓN DE SELECCIÓN ====================

        public PaginationSection setOnItemSelect(BiConsumer<MenuClickInfo, Integer> handler) {
            this.onItemSelect = handler;
            return this;
        }

        public PaginationSection setSelectedItemTemplate(MenuItem template) {
            this.selectedItemTemplate = template;
            return this;
        }

        public void setSelectedIndex(Integer index) {
            this.selectedIndex = index;
        }

        // ==================== GETTERS ====================

        public String getName() { return name; }
        public Integer getSelectedIndex() { return selectedIndex; }
        public int getTotalPages() {
            return Math.max(1, (int) Math.ceil((double) items.size() / slots.length));
        }
        public int getItemCount() { return items.size(); }
        public List<MenuItem> getAllItems() { return new ArrayList<>(items); }
        public int[] getSlots() { return slots.clone(); }

        public List<MenuItem> getItemsForPage(int page) {
            int start = (page - 1) * slots.length;
            int end = Math.min(start + slots.length, items.size());

            if (start >= items.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(items.subList(start, end));
        }

        public MenuItem getItem(int index) {
            return (index >= 0 && index < items.size()) ? items.get(index) : null;
        }

        public boolean isItemSelected(int globalIndex) {
            return selectedIndex != null && selectedIndex == globalIndex;
        }

        // ==================== GETTERS INTERNOS ====================

        MenuItem getPreviousButton() { return previousButton; }
        MenuItem getNextButton() { return nextButton; }
        int getPreviousButtonSlot() { return previousButtonSlot; }
        int getNextButtonSlot() { return nextButtonSlot; }
        MenuItem getFillerItem() { return fillerItem; }
        BiConsumer<MenuClickInfo, Integer> getOnItemSelect() { return onItemSelect; }
        MenuItem getSelectedItemTemplate() { return selectedItemTemplate; }
    }

    // ==================== PROPIEDADES DEL MENÚ ====================

    private final Map<String, PaginationSection> sections = new LinkedHashMap<>();
    private final Map<Player, Map<String, Integer>> playerPages = new ConcurrentHashMap<>();
    private final Map<Player, Map<String, Map<Integer, Integer>>> playerItemTasks = new ConcurrentHashMap<>();

    private BiConsumer<String, Integer> onSectionUpdate;
    private Consumer<Player> externalCloseHandler;

    public MultiPaginationMenu(String title, int rows) {
        super(title, rows);
        super.setCloseHandler(this::onPlayerCloseMenu);
    }

    // ==================== CONFIGURACIÓN DE SECCIONES ====================

    public PaginationSection addSection(String name, int... slots) {
        PaginationSection section = new PaginationSection(name, slots);
        sections.put(name, section);
        return section;
    }

    public PaginationSection getSection(String name) {
        return sections.get(name);
    }

    public MultiPaginationMenu setOnSectionUpdate(BiConsumer<String, Integer> callback) {
        this.onSectionUpdate = callback;
        return this;
    }

    // ==================== APERTURA Y ACTUALIZACIÓN ====================

    @Override
    public void open(Player player) {
        try {
            cleanupPlayerResources(player);
            initializePlayerPages(player);

            // Primero abrimos el inventario en el hilo principal
            super.open(player);

            updateMenuAsync(player).thenRun(() -> {
                if (super.dynamicUpdates && super.plugin != null) {
                    scheduleItemUpdatesAsync(player);
                }
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializePlayerPages(Player player) {
        if (!playerPages.containsKey(player)) {
            Map<String, Integer> pages = new ConcurrentHashMap<>();
            sections.keySet().forEach(sectionName -> pages.put(sectionName, 1));
            playerPages.put(player, pages);
        }
    }

    public void updateSection(Player player, String sectionName, int page) {
        PaginationSection section = sections.get(sectionName);
        if (section == null) return;

        Map<String, Integer> pages = playerPages.get(player);
        if (pages == null) return;

        int maxPages = section.getTotalPages();
        page = Math.max(1, Math.min(page, maxPages));
        pages.put(sectionName, page);

        int finalPage = page;
        updateMenuAsync(player).thenRun(() -> {
            if (onSectionUpdate != null) {
                onSectionUpdate.accept(sectionName, finalPage);
            }
        });
    }

    private CompletableFuture<Void> updateMenuAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Integer> pages = playerPages.get(player);
                if (pages == null) return;

                // Aplicar fillers globales primero
                super.applyFillers(player);

                // Procesar cada sección
                sections.forEach((sectionName, section) -> {
                    int currentPage = pages.getOrDefault(sectionName, 1);
                    processSectionForPlayer(player, section, currentPage);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> {
            try {
                // Actualizar inventario en el hilo principal - ahora que el inventario ya existe
                if (super.getViewer() == player && super.inventory != null) {
                    Bukkit.getScheduler().runTask(super.plugin, () -> {
                        try {
                            super.items.forEach((slot, item) -> {
                                if (item != null && item.getItemStack() != null) {
                                    super.inventory.setItem(slot, item.getItemStack());
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    // Si el inventario sigue siendo null, intentar actualizar después de un tick
                    if (super.inventory == null && super.plugin != null) {
                        Bukkit.getScheduler().runTask(super.plugin, () -> {
                            if (super.inventory != null && super.getViewer() == player) {
                                super.items.forEach((slot, item) -> {
                                    if (item != null && item.getItemStack() != null) {
                                        super.inventory.setItem(slot, item.getItemStack());
                                    }
                                });
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void applySectionFiller(Player player, PaginationSection section) {
        if (section.getFillerItem() == null) return;

        for (int slot : section.getSlots()) {
            try {
                MenuItem filler = section.getFillerItem().clone();
                if (filler.usesPlaceholders()) {
                    filler.updatePlaceholders(player);
                }
                super.items.put(slot, filler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void placeSectionItems(Player player, PaginationSection section, int currentPage) {
        List<MenuItem> pageItems = section.getItemsForPage(currentPage);
        int[] slots = section.getSlots();

        for (int i = 0; i < pageItems.size() && i < slots.length; i++) {
            try {
                int slot = slots[i];
                MenuItem item = pageItems.get(i).clone();
                int globalIndex = (currentPage - 1) * slots.length + i;

                // Aplicar template de selección si es necesario
                if (section.isItemSelected(globalIndex) && section.getSelectedItemTemplate() != null) {
                    item = section.getSelectedItemTemplate().clone();
                }

                if (item.usesPlaceholders()) {
                    item.updatePlaceholders(player);
                }

                // Configurar click handler
                setupItemClickHandler(item, section, globalIndex);
                super.items.put(slot, item);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupItemClickHandler(MenuItem item, PaginationSection section, int globalIndex) {
        Consumer<MenuClickInfo> originalHandler = item.getClickHandler();

        item.setClickHandler(clickInfo -> {
            try {
                if (section.getOnItemSelect() != null) {
                    section.getOnItemSelect().accept(clickInfo, globalIndex);
                }
                section.setSelectedIndex(globalIndex);
                updateMenuAsync(clickInfo.player());

                if (originalHandler != null) {
                    originalHandler.accept(clickInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void placeSectionNavigation(Player player, PaginationSection section, int currentPage) {
        String sectionName = section.getName();
        int totalPages = section.getTotalPages();

        super.items.remove(section.getPreviousButtonSlot());
        super.items.remove(section.getNextButtonSlot());
        if (currentPage <= 1) {
            applyFillerToSlot(player, section.getPreviousButtonSlot());
        }

        if (currentPage >= totalPages) {
            applyFillerToSlot(player, section.getNextButtonSlot());
        }

        if (currentPage > 1 && section.getPreviousButton() != null) {
            try {
                MenuItem prevButton = section.getPreviousButton().clone();
                if (prevButton.usesPlaceholders()) {
                    prevButton.updatePlaceholders(player);
                }
                prevButton.setClickHandler(info -> updateSection(player, sectionName, currentPage - 1));
                super.items.put(section.getPreviousButtonSlot(), prevButton);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (currentPage < totalPages && section.getNextButton() != null) {
            try {
                MenuItem nextButton = section.getNextButton().clone();
                if (nextButton.usesPlaceholders()) {
                    nextButton.updatePlaceholders(player);
                }
                nextButton.setClickHandler(info -> updateSection(player, sectionName, currentPage + 1));
                super.items.put(section.getNextButtonSlot(), nextButton);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processSectionForPlayer(Player player, PaginationSection section, int currentPage) {
        try {
            clearSectionSlots(section);
            applySectionFiller(player, section);
            placeSectionItems(player, section, currentPage);
            placeSectionNavigation(player, section, currentPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFillerToSlot(Player player, int slot) {
        if (super.globalFiller != null) {
            MenuItem filler = super.globalFiller.clone();
            if (filler.usesPlaceholders()) {
                filler.updatePlaceholders(player);
            }
            super.items.put(slot, filler);
        }
    }

    private void clearSectionSlots(PaginationSection section) {
        for (int slot : section.getSlots()) {
            super.items.remove(slot);
        }

        super.items.remove(section.getPreviousButtonSlot());
        super.items.remove(section.getNextButtonSlot());
    }

    // ==================== ACTUALIZACIONES DINÁMICAS ====================

    private CompletableFuture<Void> scheduleItemUpdatesAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            if (!playerItemTasks.containsKey(player)) {
                playerItemTasks.put(player, new ConcurrentHashMap<>());
            }

            Map<String, Map<Integer, Integer>> sectionTasks = playerItemTasks.get(player);

            sections.forEach((sectionName, section) -> {
                if (!sectionTasks.containsKey(sectionName)) {
                    sectionTasks.put(sectionName, new ConcurrentHashMap<>());
                }

                Map<Integer, Integer> itemTasks = sectionTasks.get(sectionName);

                for (int slot : section.getSlots()) {
                    MenuItem item = super.getItem(slot);
                    if (item != null && item.needsDynamicUpdate() && item.usesPlaceholders()) {
                        scheduleItemUpdate(player, slot, item, itemTasks);
                    }
                }
            });
        });
    }

    private void scheduleItemUpdate(Player player, int slot, MenuItem item, Map<Integer, Integer> itemTasks) {
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(super.plugin, () -> {
            if (player.isOnline() && super.getViewer() == player) {
                MenuItem currentItem = super.getItem(slot);
                if (currentItem != null) {
                    currentItem.updatePlaceholders(player);
                    super.inventory.setItem(slot, currentItem.getItemStack());
                }
            } else {
                Integer existingTaskId = itemTasks.remove(slot);
                if (existingTaskId != null) {
                    Bukkit.getScheduler().cancelTask(existingTaskId);
                }
            }
        }, item.getUpdateInterval(), item.getUpdateInterval());

        itemTasks.put(slot, taskId);
    }

    // ==================== LIMPIEZA ====================

    private void onPlayerCloseMenu(Player player) {
        cleanupPlayerResources(player);
        if (externalCloseHandler != null) {
            externalCloseHandler.accept(player);
        }
    }

    private void cleanupPlayerResources(Player player) {
        playerPages.remove(player);

        Map<String, Map<Integer, Integer>> sectionTasks = playerItemTasks.remove(player);
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

    // ==================== UTILIDADES PÚBLICAS ====================

    public int getCurrentPage(Player player, String sectionName) {
        Map<String, Integer> pages = playerPages.get(player);
        return pages != null ? pages.getOrDefault(sectionName, 1) : 1;
    }

    public Integer getSelectedIndex(String sectionName) {
        PaginationSection section = sections.get(sectionName);
        return section != null ? section.getSelectedIndex() : null;
    }

    public MenuItem getSelectedItem(String sectionName) {
        PaginationSection section = sections.get(sectionName);
        if (section != null && section.getSelectedIndex() != null) {
            return section.getItem(section.getSelectedIndex());
        }
        return null;
    }

    public Set<String> getSectionNames() {
        return new HashSet<>(sections.keySet());
    }

    // ==================== OVERRIDE MÉTODOS DE ENCADENAMIENTO ====================

    @Override
    public MultiPaginationMenu setCloseHandler(Consumer<Player> closeHandler) {
        this.externalCloseHandler = closeHandler;
        super.setCloseHandler(this::onPlayerCloseMenu);
        return this;
    }

    @Override
    public MultiPaginationMenu setReturnMenu(Menu returnMenu) {
        super.setReturnMenu(returnMenu);
        return this;
    }

    @Override
    public MultiPaginationMenu enableDynamicUpdates(JavaPlugin plugin, long tickInterval) {
        super.enableDynamicUpdates(plugin, tickInterval);
        return this;
    }

    @Override
    public MultiPaginationMenu disableDynamicUpdates() {
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
    public MultiPaginationMenu setGlobalFiller(MenuItem filler) {
        super.setGlobalFiller(filler);
        return this;
    }

    @Override
    public MultiPaginationMenu setBorderFiller(MenuItem borderItem) {
        super.setBorderFiller(borderItem);
        return this;
    }
}