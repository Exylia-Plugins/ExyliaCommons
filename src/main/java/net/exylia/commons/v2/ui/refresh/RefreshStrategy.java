package net.exylia.commons.v2.ui.refresh;

import net.exylia.commons.v2.ui.model.MenuV2;

public interface RefreshStrategy {
    void refresh(MenuV2 menu, int triggeredSlot);

    default String getName() {
        return this.getClass().getSimpleName();
    }
}
