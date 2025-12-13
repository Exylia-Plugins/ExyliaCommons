package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.region.RegionManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class RegionAdapter extends ReloadableSystemAdapter {

    public RegionAdapter() {
        super("RegionManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
        RegionManager instance = RegionManager.getInstance();
        if (instance != null && instance.getCacheManager() != null) {
            instance.getCacheManager().invalidateAll();
        }
    }

    @Override
    protected void performReload() throws Exception {
        RegionManager instance = RegionManager.getInstance();
        if (instance != null) {
            instance.cleanup();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return RegionManager.getInstance() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
