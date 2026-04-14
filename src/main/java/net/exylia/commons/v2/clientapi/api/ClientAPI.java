package net.exylia.commons.v2.clientapi.api;

import net.exylia.commons.v2.clientapi.team.api.TeamTrackerAPI;
import net.exylia.commons.v2.clientapi.team.core.TeamTrackerManager;
import net.exylia.commons.v2.clientapi.waypoint.api.WaypointAPI;
import net.exylia.commons.v2.clientapi.waypoint.core.WaypointManager;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class ClientAPI {

    private static WaypointManager waypointManager;
    private static TeamTrackerManager teamTrackerManager;
    private static boolean initialized = false;

    private ClientAPI() {}

    public static void initialize(Plugin plugin) {
        if (initialized) throw new IllegalStateException("ClientAPI is already initialized.");

        DebugAPI.logLibDebug("Initializing ClientAPI...");

        waypointManager = new WaypointManager();
        waypointManager.initialize();
        WaypointAPI.initialize(waypointManager);

        teamTrackerManager = new TeamTrackerManager();
        teamTrackerManager.initialize();
        TeamTrackerAPI.initialize(teamTrackerManager);

        plugin.getServer().getPluginManager().registerEvents(new ClientListener(plugin), plugin);

        initialized = true;
        DebugAPI.logLibInfo("ClientAPI initialized.");
    }

    public static void shutdown() {
        if (teamTrackerManager != null) {
            teamTrackerManager.shutdown();
            teamTrackerManager = null;
        }
        waypointManager = null;
        initialized = false;
    }

    public static WaypointManager getWaypointManager() {
        return waypointManager;
    }

    public static TeamTrackerManager getTeamTrackerManager() {
        return teamTrackerManager;
    }

    private static class ClientListener implements Listener {

        private final Plugin plugin;

        ClientListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            var player = event.getPlayer();
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) waypointManager.handleJoin(player);
            }, null, 20L);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            waypointManager.cleanupPlayer(player);
            teamTrackerManager.removeMember(player);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onWorldChange(PlayerChangedWorldEvent event) {
            waypointManager.handleWorldChange(event.getPlayer());
        }
    }
}
