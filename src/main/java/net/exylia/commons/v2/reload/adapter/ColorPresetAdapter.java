package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.visual.color.ColorPresetManager;

public class ColorPresetAdapter extends ReloadableSystemAdapter {

    public ColorPresetAdapter() {
        super("ColorPresetManager", ReloadPriority.LOW);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        ColorPresetManager.getInstance().reload();
    }

    @Override
    public boolean isAvailable() {
        return ColorPresetManager.getInstance().isInitialized();
    }
}
