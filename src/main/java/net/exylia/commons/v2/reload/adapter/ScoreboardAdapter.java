package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;

public final class ScoreboardAdapter extends ReloadableSystemAdapter {
    public ScoreboardAdapter() {
        super("ScoreboardManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() {
        ScoreboardManager.getInstance().clearCache();
    }

    @Override
    protected void performReload() {
        ScoreboardManager.getInstance().reload();
    }

    @Override
    public boolean isAvailable() {
        try {
            return ScoreboardManager.getInstance().isInitialized();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }
}
