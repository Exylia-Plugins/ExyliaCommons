package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.OldColorUtils;
import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.visual.api.ColorAPI;

public class ColorAdapter extends ReloadableSystemAdapter {

    public ColorAdapter() {
        super("ColorSystem", ReloadPriority.LOW);
    }

    @Override
    protected void performCacheClear() throws Exception {
        try {
            ColorUtils.clearCache();
        } catch (Exception ignored) {
        }

        try {
            OldColorUtils.clearCache();
        } catch (Exception ignored) {
        }

        try {
            ColorAPI.clearCache();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void performReload() throws Exception {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
