package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.ui.model.*;
import net.exylia.commons.v2.ui.refresh.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class MultiPaginatedFullInventoryMenuV2 extends MenuV2 {
    private final Map<UUID, ItemStack[]> inventorySnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> armorSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> offHandSnapshots = new ConcurrentHashMap<>();
    private final Map<Integer, MenuItemV2> playerInventoryItems = new ConcurrentHashMap<>();

    private boolean allowPlayerInventoryInteraction = false;
    private boolean allowHotbarSwap = false;
    private boolean allowDropItems = false;
    private boolean clearPlayerInventory = true;
    private boolean restoreOnClose = true;
    private boolean closeOnMove = false;

    private final Map<String, PaginationSection> sections = new ConcurrentHashMap<>();

    public MultiPaginatedFullInventoryMenuV2(String title, int rows) {
        super(UUID.randomUUID().toString(), MenuType.MULTI_PAGINATED_FULL_INVENTORY);
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

        snapshotPlayerInventory(player);

        enrichContext();
        processTitle();
        createInventory();
        applyFillers();
        populateInventory();

        if (clearPlayerInventory) {
            clearPlayerInventoryContents(player);
        }

        populatePlayerInventory(player);

        player.openInventory(inventory);
        this.state = MenuState.OPEN;

        handleOpen();

        if (dynamicUpdates) {
            startDynamicUpdates();
        }
    }

    @Override
    public void close() {
        if (state == MenuState.CLOSED || state == MenuState.CLOSING) {
            return;
        }

        this.state = MenuState.CLOSING;

        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (closeSound != null && viewer != null) {
            closeSound.playAsync(viewer);
        }

        if (viewer != null && restoreOnClose) {
            restorePlayerInventory(viewer);
        }

        if (viewer != null && inventory != null) {
            Schedulers.sync(() -> viewer.closeInventory());
        }

        if (closeHandler != null && viewer != null) {
            closeHandler.accept(viewer);
        }

        this.state = MenuState.CLOSED;
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
            .put("sections_count", sections.size())
            .put("player_inventory_slots", playerInventoryItems.size())
            .put("allow_interaction", allowPlayerInventoryInteraction);
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

    private void snapshotPlayerInventory(Player player) {
        if (player == null) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        UUID playerId = player.getUniqueId();

        ItemStack[] contents = inv.getContents();
        ItemStack[] contentsCopy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            contentsCopy[i] = contents[i] != null ? contents[i].clone() : null;
        }
        inventorySnapshots.put(playerId, contentsCopy);

        ItemStack[] armor = inv.getArmorContents();
        ItemStack[] armorCopy = new ItemStack[armor.length];
        for (int i = 0; i < armor.length; i++) {
            armorCopy[i] = armor[i] != null ? armor[i].clone() : null;
        }
        armorSnapshots.put(playerId, armorCopy);

        ItemStack offHand = inv.getItemInOffHand();
        offHandSnapshots.put(playerId, offHand != null ? offHand.clone() : null);
    }

    private void clearPlayerInventoryContents(Player player) {
        if (player == null) {
            return;
        }

        Schedulers.sync(() -> {
            PlayerInventory inv = player.getInventory();
            inv.clear();
            inv.setArmorContents(new ItemStack[4]);
            inv.setItemInOffHand(null);
        });
    }

    private void populatePlayerInventory(Player player) {
        if (player == null || playerInventoryItems.isEmpty()) {
            return;
        }

        Schedulers.sync(() -> {
            PlayerInventory inv = player.getInventory();
            playerInventoryItems.forEach((slot, menuItem) -> {
                ItemStack item = menuItem.build(player, context.getPlaceholderContext());
                inv.setItem(slot, item);
            });
        });
    }

    private void restorePlayerInventory(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();

        ItemStack[] contents = inventorySnapshots.remove(playerId);
        ItemStack[] armor = armorSnapshots.remove(playerId);
        ItemStack offHand = offHandSnapshots.remove(playerId);

        if (contents == null) {
            return;
        }

        Schedulers.sync(() -> {
            PlayerInventory inv = player.getInventory();
            inv.setContents(contents);

            if (armor != null) {
                inv.setArmorContents(armor);
            }

            if (offHand != null) {
                inv.setItemInOffHand(offHand);
            }

            player.updateInventory();
        });
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

    public void setPlayerInventoryItem(int slot, MenuItemV2 item) {
        if (slot < 0 || slot >= 41) {
            return;
        }

        if (item != null) {
            playerInventoryItems.put(slot, item);
        } else {
            playerInventoryItems.remove(slot);
        }

        if (state == MenuState.OPEN && viewer != null) {
            Schedulers.sync(() -> {
                PlayerInventory inv = viewer.getInventory();
                if (item != null) {
                    inv.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
                } else {
                    inv.setItem(slot, null);
                }
            });
        }
    }

    public boolean isPlayerInventorySlot(int rawSlot) {
        if (inventory == null || viewer == null) {
            return false;
        }

        int topSize = inventory.getSize();
        return rawSlot >= topSize;
    }
}
