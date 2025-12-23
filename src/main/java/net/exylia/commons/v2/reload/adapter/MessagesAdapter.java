package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class MessagesAdapter extends ReloadableSystemAdapter {

    public MessagesAdapter() {
        super("Messages", ReloadPriority.HIGH);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        Messages.reload();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
