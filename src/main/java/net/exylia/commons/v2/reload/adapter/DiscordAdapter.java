package net.exylia.commons.v2.reload.adapter;

import net.exylia.commons.v2.discord.core.WebhookManager;
import net.exylia.commons.v2.reload.core.ReloadPriority;

public class DiscordAdapter extends ReloadableSystemAdapter {

    public DiscordAdapter() {
        super("DiscordWebhooks", ReloadPriority.LOW);
    }

    @Override
    protected void performCacheClear() throws Exception {
    }

    @Override
    protected void performReload() throws Exception {
        WebhookManager instance = WebhookManager.getInstance();
        if (instance != null && instance.getPlugin() != null) {
            instance.reload();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            WebhookManager instance = WebhookManager.getInstance();
            return instance != null && instance.getPlugin() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
