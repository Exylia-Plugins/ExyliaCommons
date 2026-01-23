package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class ConfigSchemaAdapter extends ReloadableSystemAdapter {

    public ConfigSchemaAdapter() {
        super("ConfigSchema", ReloadPriority.CRITICAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        ConfigSchemaRegistry.reloadAll();
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return !ConfigSchemaRegistry.getRegisteredSchemas().isEmpty();
    }
}
