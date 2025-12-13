package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class DatabaseV1Adapter extends ReloadableSystemAdapter {

    public DatabaseV1Adapter() {
        super("DatabaseV1", ReloadPriority.HIGH);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        DatabaseManager instance = DatabaseManager.getInstance();
        if (instance == null) {
            throw new Exception("DatabaseManager not initialized");
        }

        boolean success = instance.performCompleteReload();
        if (!success) {
            throw new Exception("DatabaseManager reload failed");
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            DatabaseManager instance = DatabaseManager.getInstance();
            return instance != null && instance.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}
