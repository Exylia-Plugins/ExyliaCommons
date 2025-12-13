package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class HologramAdapter extends ReloadableSystemAdapter {

    public HologramAdapter() {
        super("HologramManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
        HologramManager instance = HologramManager.getInstance();
        if (instance != null) {
            instance.removeAllHolograms();
        }
    }

    @Override
    protected void performReload() throws Exception {
        HologramManager instance = HologramManager.getInstance();
        if (instance != null) {
            instance.reload();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return HologramManager.getInstance() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
