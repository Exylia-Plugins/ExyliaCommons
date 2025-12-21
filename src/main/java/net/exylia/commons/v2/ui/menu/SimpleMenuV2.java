package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.ui.model.MenuContext;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.MenuV2;
import net.exylia.commons.v2.ui.refresh.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SimpleMenuV2 extends MenuV2 {

    public SimpleMenuV2(String title, int rows) {
        super(UUID.randomUUID().toString(), MenuType.SIMPLE);
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
        this.state = net.exylia.commons.v2.ui.model.MenuState.OPEN;

        handleOpen();

        if (dynamicUpdates) {
            startDynamicUpdates();
        }
    }

    @Override
    public void populateInventory() {
        if (inventory == null) {
            return;
        }

        items.forEach((slot, item) -> {
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
            }
        });
    }
}
