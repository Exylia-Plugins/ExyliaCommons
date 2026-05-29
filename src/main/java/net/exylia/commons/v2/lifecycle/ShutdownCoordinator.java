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

    private void shutdownManagers() {
        if (TaskAPI.isInitialized()) {
            TaskAPI.shutdown();
        }

        try {
            net.exylia.commons.v2.database.api.Database.shutdown();
        } catch (IllegalStateException ignored) {
        }
    }
}
