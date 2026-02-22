package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class PlaceholderAdapter extends ReloadableSystemAdapter {

    public PlaceholderAdapter() {
        super("PlaceholderSystem", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
        try {
            PlaceholderRegistry instance = PlaceholderRegistry.getInstance();
            if (instance != null) {
                instance.clearCache();
            }
        } catch (IllegalStateException ignored) {
        }

        try {
            PlaceholderSystemManager managerV1 = PlaceholderSystemManager.getInstance();
            if (managerV1 != null) {
                managerV1.clearCache();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void performReload() throws Exception {
    }

    @Override
    public boolean isAvailable() {
        try {
            return PlaceholderRegistry.getInstance() != null ||
                   PlaceholderSystemManager.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
