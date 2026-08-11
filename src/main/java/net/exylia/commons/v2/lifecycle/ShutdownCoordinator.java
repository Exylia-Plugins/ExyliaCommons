package net.exylia.commons.v2.lifecycle;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.skull.api.SkullAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.ActionBarAPI;
import net.exylia.commons.v2.visual.api.BossBarAPI;
import net.exylia.commons.v2.visual.api.TitleAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ShutdownCoordinator {

    public void executeOrderedShutdown(ExyliaPlugin plugin) {
        executeOrderedShutdown((JavaPlugin) plugin);
    }

    public void executeOrderedShutdown(JavaPlugin plugin) {
        DebugAPI.logLibInfo("Starting ordered shutdown for: " + plugin.getDescription().getName());

        ChatInputManager.shutdown();
        SkullAPI.shutdown();
        cleanupPlayerState();
        shutdownManagers();

        DebugAPI.logLibInfo("Ordered shutdown completed for: " + plugin.getDescription().getName());
    }

    private void cleanupPlayerState() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            TitleAPI.cancelAll(player);
            ActionBarAPI.cancelAll(player);
            BossBarAPI.cancelAll(player);
        }
    }

    /**
     * Order matters: the database is drained before the task pools are torn down.
     * <p>
     * Write-behind repositories buffer saves in memory and flush on a timer, so at
     * any moment up to one flush interval of stats, ELO and loadout writes exists
     * only on the heap. {@code Database.shutdown()} performs the final flush that
     * commits them. Shutting the pools down first left that flush racing a dying
     * executor — and any write that did get through was landing on a database
     * already on its way out, which is how an embedded store ends up inconsistent.
     */
    private void shutdownManagers() {
        try {
            net.exylia.commons.v2.database.api.Database.shutdown();
        } catch (IllegalStateException ignored) {
        }

        if (TaskAPI.isInitialized()) {
            TaskAPI.shutdown();
        }
    }
}
