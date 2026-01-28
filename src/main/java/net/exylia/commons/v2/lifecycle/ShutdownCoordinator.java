package net.exylia.commons.v2.lifecycle;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.OldColorUtils;
import net.exylia.commons.utils.visuals.ActionBarUtils;
import net.exylia.commons.utils.visuals.BossbarUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

public class ShutdownCoordinator {

    public void executeOrderedShutdown(ExyliaPlugin plugin) {
        logInternalInfo("Starting ordered shutdown for: " + plugin.getDescription().getName());

        cleanupPlayerState();

        shutdownManagers();

        logInternalInfo("Ordered shutdown completed for: " + plugin.getDescription().getName());
    }

    private void cleanupPlayerState() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            TitleUtils.cancelAllTitles(player);
            ActionBarUtils.cancelAllActionBars(player);
            BossbarUtils.cancelAllBossBars(player);
        }
    }

    private void shutdownManagers() {
        if (TaskAPI.isInitialized()) {
            TaskAPI.shutdown();
        }

        if (DatabaseManager.getInstance() != null) {
            DatabaseManager.getInstance().shutdown();
        }

        try {
            net.exylia.commons.v2.database.core.DatabaseManager.getInstance();
            net.exylia.commons.v2.database.api.Database.shutdown();
        } catch (IllegalStateException ignored) {
        }

        RedisIntegration.shutdownRedis();

        ColorUtils.shutdown();
        OldColorUtils.shutdown();
        AdapterFactory.close();
    }
}
