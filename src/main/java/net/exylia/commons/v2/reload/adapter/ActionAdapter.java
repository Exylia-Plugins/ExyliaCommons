package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class ActionAdapter extends ReloadableSystemAdapter {

    public ActionAdapter() {
        super("ActionManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
        if (ActionManager.isInitialized()) {
            ActionManager.getInstance().clearCache();
        }
    }

    @Override
    protected void performReload() throws Exception {
        if (ActionManager.isInitialized()) {
            ActionManager.getInstance().reload();
        }
    }

    @Override
    public boolean isAvailable() {
        return ActionManager.isInitialized();
    }
}
