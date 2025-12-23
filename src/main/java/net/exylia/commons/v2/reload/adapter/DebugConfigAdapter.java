package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.debug.config.DebugConfig;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class DebugConfigAdapter extends ReloadableSystemAdapter {

    public DebugConfigAdapter() {
        super("DebugConfig", ReloadPriority.CRITICAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        DebugConfig.reload();
    }

    @Override
    public boolean isAvailable() {
        try {
            return DebugConfig.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
