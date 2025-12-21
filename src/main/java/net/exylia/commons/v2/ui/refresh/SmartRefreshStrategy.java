package net.exylia.commons.v2.ui.refresh;

import net.exylia.commons.v2.ui.model.MenuItemV2;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.MenuV2;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmartRefreshStrategy implements RefreshStrategy {
    @Override
    public void refresh(MenuV2 menu, int triggeredSlot) {
        if (menu == null || menu.getState() != MenuState.OPEN) {
            return;
        }

        menu.updateSlot(triggeredSlot);

        Set<Integer> slotsToUpdate = new HashSet<>();

        for (Map.Entry<Integer, MenuItemV2> entry : menu.getItems().entrySet()) {
            if (entry.getKey() == triggeredSlot) {
                continue;
            }

            MenuItemV2 item = entry.getValue();
            if (shouldRefresh(item)) {
                slotsToUpdate.add(entry.getKey());
            }
        }

        if (slotsToUpdate.size() > menu.getSize() / 2) {
            menu.populateInventory();
        } else {
            for (int slot : slotsToUpdate) {
                menu.updateSlot(slot);
            }
        }
    }

    private boolean shouldRefresh(MenuItemV2 item) {
        if (item.isDynamicUpdate()) {
            return true;
        }

        if (item.getLoreDynamicSupplier() != null) {
            return true;
        }

        if (hasPlaceholders(item.getRawName())) {
            return true;
        }

        if (hasPlaceholders(item.getRawLore())) {
            return true;
        }

        return false;
    }

    private boolean hasPlaceholders(String text) {
        return text != null && (text.contains("{") || text.contains("%"));
    }

    private boolean hasPlaceholders(List<String> lore) {
        return lore != null && lore.stream().anyMatch(this::hasPlaceholders);
    }
}
