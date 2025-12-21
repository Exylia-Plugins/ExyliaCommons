package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.ui.model.*;
import net.exylia.commons.v2.ui.refresh.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;
import java.util.function.BiConsumer;

@Getter
@Setter
public class SelectionMenuV2 extends MenuV2 {
    private final List<MenuItemV2> options = new ArrayList<>();
    private BiConsumer<Player, Integer> onSelect;
    private boolean closeOnSelect = true;
    private int startSlot = 10;
    private int[] optionSlots;

    public SelectionMenuV2(String title, List<MenuItemV2> options) {
        super(UUID.randomUUID().toString(), MenuType.SELECTION);
        this.rawTitle = title;

        if (options != null) {
            this.options.addAll(options);
        }

        this.rows = calculateRows(this.options.size());
        this.size = this.rows * 9;

        generateDefaultSlots();
        initializeRefreshStrategy();
    }

    public SelectionMenuV2(String title, int rows, List<MenuItemV2> options) {
        super(UUID.randomUUID().toString(), MenuType.SELECTION);
        this.rawTitle = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;

        if (options != null) {
            this.options.addAll(options);
        }

        generateDefaultSlots();
        initializeRefreshStrategy();
    }

    private int calculateRows(int optionCount) {
        if (optionCount <= 7) return 3;
        if (optionCount <= 14) return 4;
        if (optionCount <= 21) return 5;
        return 6;
    }

    private void generateDefaultSlots() {
        List<Integer> slots = new ArrayList<>();

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }

        optionSlots = slots.stream().mapToInt(i -> i).toArray();
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

        context.getPlaceholderContext()
            .put("options_count", options.size());
    }

    @Override
    public void populateInventory() {
        if (inventory == null) {
            return;
        }

        items.forEach((slot, item) -> {
            boolean isOptionSlot = false;
            for (int optSlot : optionSlots) {
                if (slot == optSlot) {
                    isOptionSlot = true;
                    break;
                }
            }

            if (!isOptionSlot) {
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
            }
        });

        populateOptions();
    }

    private void populateOptions() {
        for (int i = 0; i < Math.min(options.size(), optionSlots.length); i++) {
            int slot = optionSlots[i];
            MenuItemV2 option = options.get(i).clone();

            final int index = i;
            option.addAction(ClickType.LEFT, "ui:select index:" + index);

            inventory.setItem(slot, option.build(viewer, context.getPlaceholderContext()));
            items.put(slot, option);
        }
    }

    public void handleSelection(Player player, int index) {
        if (index < 0 || index >= options.size()) {
            return;
        }

        if (onSelect != null) {
            onSelect.accept(player, index);
        }

        if (closeOnSelect) {
            close();
        }
    }

    public void addOption(MenuItemV2 option) {
        if (option != null) {
            options.add(option);

            if (state == MenuState.OPEN) {
                refresh();
            }
        }
    }

    public void removeOption(int index) {
        if (index >= 0 && index < options.size()) {
            options.remove(index);

            if (state == MenuState.OPEN) {
                refresh();
            }
        }
    }

    public void clearOptions() {
        options.clear();

        if (state == MenuState.OPEN) {
            refresh();
        }
    }

    public SelectionMenuV2 setOnSelect(BiConsumer<Player, Integer> callback) {
        this.onSelect = callback;
        return this;
    }

    public SelectionMenuV2 setCloseOnSelect(boolean close) {
        this.closeOnSelect = close;
        return this;
    }

    public SelectionMenuV2 setOptionSlots(int[] slots) {
        this.optionSlots = slots != null ? slots.clone() : new int[0];
        return this;
    }
}
