package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.formatter.core.FormatterRegistry;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class ConfigSystemAdapter extends ReloadableSystemAdapter {

    public ConfigSystemAdapter() {
        super("ConfigSystem", ReloadPriority.CRITICAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        Configs.reloadAll();
        FormatterRegistry.reload();
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
