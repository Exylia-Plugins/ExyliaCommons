package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.command.core.CommandManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class CommandAdapter extends ReloadableSystemAdapter {

    public CommandAdapter() {
        super("CommandManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        try {
            CommandManager.getInstance().reload();
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return CommandManager.getInstance() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
