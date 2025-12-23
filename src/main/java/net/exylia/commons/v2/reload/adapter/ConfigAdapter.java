package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class ConfigAdapter extends ReloadableSystemAdapter {

    public ConfigAdapter() {
        super("Config", ReloadPriority.CRITICAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        Configs.reloadAll();
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}