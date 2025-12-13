package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.utils.DateFormatter;
import net.exylia.commons.utils.TimeFormatter;
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
        boolean success = ConfigManager.reloadAllAsync().join();
        if (!success) {
            throw new Exception("ConfigManager reload failed");
        }

        Configs.reloadAll();

        TimeFormatter.reload();
        DateFormatter.reload();

        FormatterRegistry.reload();
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}
