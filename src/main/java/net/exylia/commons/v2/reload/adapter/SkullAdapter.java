package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.skull.core.SkullManager;

public class SkullAdapter extends ReloadableSystemAdapter {

    public SkullAdapter() {
        super("SkullManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        try {
            SkullManager.getInstance().reload();
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            SkullManager.getInstance();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
