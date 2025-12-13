package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.clan.core.ClanManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class ClanAdapter extends ReloadableSystemAdapter {

    public ClanAdapter() {
        super("Clan System", ReloadPriority.NORMAL);
    }

    @Override
    protected void performReload() throws Exception {
        if (ClanManager.isInitialized()) {
            ClanManager.getInstance().reload();
        }
    }

    @Override
    protected void performCacheClear() throws Exception {
        if (ClanManager.isInitialized()) {
            ClanManager.getInstance().getCacheManager().invalidateAll();
        }
    }

    @Override
    public boolean isAvailable() {
        return ClanManager.isInitialized();
    }
}
