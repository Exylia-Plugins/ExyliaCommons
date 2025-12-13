package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.formatter.api.FormatterAPI;
import net.exylia.commons.v2.formatter.core.FormatterRegistry;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class FormatterAdapter extends ReloadableSystemAdapter {

    public FormatterAdapter() {
        super("FormatterRegistry", ReloadPriority.LOW);
    }

    @Override
    protected void performCacheClear() throws Exception {
        FormatterAPI.invalidateAllCaches();
    }

    @Override
    protected void performReload() throws Exception {
        FormatterRegistry.reload();
    }
}
