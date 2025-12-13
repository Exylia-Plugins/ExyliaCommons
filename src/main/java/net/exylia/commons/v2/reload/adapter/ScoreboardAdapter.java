package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;

public class ScoreboardAdapter extends ReloadableSystemAdapter {

    public ScoreboardAdapter() {
        super("ScoreboardManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
        ScoreboardManager instance = ScoreboardManager.getInstance();
        if (instance != null) {
            instance.clearCache();
        }
    }

    @Override
    protected void performReload() throws Exception {
        ScoreboardManager instance = ScoreboardManager.getInstance();
        if (instance != null) {
            instance.hideAll();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return ScoreboardManager.getInstance() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
