package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.database.core.DatabaseManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class DatabaseV2Adapter extends ReloadableSystemAdapter {

    public DatabaseV2Adapter() {
        super("DatabaseV2", ReloadPriority.HIGH);
    }

    @Override
    protected void performCacheClear() throws Exception {
        DatabaseManager instance = DatabaseManager.getInstance();
        if (instance != null) {
            instance.getLocalCacheStrategy().clear();
        }
    }

    @Override
    protected void performReload() throws Exception {
    }

    @Override
    public boolean isAvailable() {
        try {
            DatabaseManager.getInstance();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
