package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.visual.core.VisualManager;

public class VisualAdapter extends ReloadableSystemAdapter {

    public VisualAdapter() {
        super("VisualManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
        VisualManager instance = VisualManager.getInstance();
        if (instance != null && instance.isInitialized()) {
            instance.shutdown();
        }
    }

    @Override
    protected void performReload() throws Exception {
    }

    @Override
    public boolean isAvailable() {
        try {
            return VisualManager.getInstance() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
