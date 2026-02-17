package net.exylia.commons.v2.crafting.reload;

import net.exylia.commons.v2.crafting.core.CraftingManager;
import net.exylia.commons.v2.reload.adapter.ReloadableSystemAdapter;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class CraftingAdapter extends ReloadableSystemAdapter {

    public CraftingAdapter() {
        super("CraftingSystem", ReloadPriority.NORMAL);
    }

    @Override
    protected void performReload() throws Exception {
        if (CraftingManager.isInitialized()) {
            CraftingManager.getInstance().reload();
        }
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    public boolean isAvailable() {
        return CraftingManager.isInitialized();
    }
}
