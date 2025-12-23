package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.reward.core.RewardManager;

public class RewardAdapter extends ReloadableSystemAdapter {

    public RewardAdapter() {
        super("RewardManager", ReloadPriority.NORMAL);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        RewardManager.getInstance().reload();
    }

    @Override
    public boolean isAvailable() {
        try {
            return RewardManager.getInstance() != null && RewardManager.getInstance().getPlugin() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
