package net.exylia.commons.v2.ui.refresh;

import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.MenuV2;

public class SlotOnlyRefreshStrategy implements RefreshStrategy {
    @Override
    public void refresh(MenuV2 menu, int triggeredSlot) {
        if (menu == null || menu.getState() != MenuState.OPEN) {
            return;
        }

        menu.updateSlot(triggeredSlot);
    }
}
