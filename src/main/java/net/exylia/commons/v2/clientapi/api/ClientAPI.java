package net.exylia.commons.v2.clientapi.api;

import com.lunarclient.apollo.Apollo;
import lombok.Getter;
import net.exylia.commons.v2.clientapi.cooldown.api.CooldownAPI;
import net.exylia.commons.v2.clientapi.cooldown.core.CooldownManager;
import net.exylia.commons.v2.clientapi.team.api.ClientTeamAPI;
import net.exylia.commons.v2.clientapi.team.api.TeamTrackerAPI;
import net.exylia.commons.v2.clientapi.team.core.ClientTeamManager;
import net.exylia.commons.v2.clientapi.team.core.TeamTrackerManager;
import net.exylia.commons.v2.clientapi.waypoint.api.WaypointAPI;
import net.exylia.commons.v2.clientapi.waypoint.core.WaypointManager;
import net.exylia.commons.v2.cooldown.api.ItemCooldownAPI;
import net.exylia.commons.v2.cooldown.core.ItemCooldownListener;
import net.exylia.commons.v2.cooldown.core.ItemCooldownManager;
import net.exylia.commons.v2.cooldown.core.ItemCooldownRegistry;
import net.exylia.commons.v2.cooldown.core.ItemCooldownStorage;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@Getter
public final class ClientAPI {

    private static WaypointManager waypointManager;
    private static TeamTrackerManager teamTrackerManager;
    private static ClientTeamManager clientTeamManager;
    private static CooldownManager cooldownManager;
    private static ItemCooldownManager itemCooldownManager;
    private static ItemCooldownRegistry itemCooldownRegistry;
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

        if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            clientTeamManager = new ClientTeamManager();
            ClientTeamAPI.initialize(clientTeamManager);
        }

        cooldownManager = new CooldownManager();
        cooldownManager.initialize(plugin);
        CooldownAPI.initialize(cooldownManager);

        ItemCooldownStorage itemCooldownStorage = new ItemCooldownStorage(plugin.getDataFolder());
        itemCooldownManager = new ItemCooldownManager(itemCooldownStorage);
        itemCooldownRegistry = new ItemCooldownRegistry();
        ItemCooldownListener itemCooldownListener = new ItemCooldownListener(itemCooldownManager, itemCooldownRegistry);
        ItemCooldownAPI.initialize(itemCooldownManager, itemCooldownRegistry, itemCooldownListener);

        plugin.getServer().getPluginManager().registerEvents(new ClientListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(itemCooldownListener, plugin);

        initialized = true;
        DebugAPI.logLibInfo("ClientAPI initialized.");
    }

    public static boolean isLunarPlayer(Player player) {
        if (!isApolloAvailable()) return false;
        try {
            return Apollo.getPlayerManager().getPlayer(player.getUniqueId()).isPresent();
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    private static boolean isApolloAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Apollo") ||
               Bukkit.getPluginManager().isPluginEnabled("Apollo-Folia") ||
               Bukkit.getPluginManager().isPluginEnabled("Apollo-Bukkit");
    }

    public static void shutdown() {
        if (teamTrackerManager != null) {
            teamTrackerManager.shutdown();
            teamTrackerManager = null;
        }
        if (clientTeamManager != null) {
            clientTeamManager.shutdown();
            clientTeamManager = null;
        }
        waypointManager = null;
        if (cooldownManager != null) {
            cooldownManager.shutdown();
            cooldownManager = null;
        }
        if (itemCooldownRegistry != null) {
            itemCooldownRegistry.clear();
            itemCooldownRegistry = null;
        }
        if (itemCooldownManager != null) {
            itemCooldownManager.shutdown();
            itemCooldownManager = null;
        }
        initialized = false;
    }

    private static class ClientListener implements Listener {

        private final Plugin plugin;

        ClientListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            var player = event.getPlayer();
            UUID joinedId = player.getUniqueId();
            TaskAPI.io(() -> itemCooldownManager.loadPlayer(joinedId));
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) waypointManager.handleJoin(player);
            }, null, 20L);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            UUID quitId = player.getUniqueId();
            waypointManager.cleanupPlayer(player);
            teamTrackerManager.removeMember(player);
            TaskAPI.io(() -> itemCooldownManager.saveAndCleanup(quitId));
            cooldownManager.cleanupPlayer(quitId);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onWorldChange(PlayerChangedWorldEvent event) {
            Player player = event.getPlayer();
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) waypointManager.handleWorldChange(player);
            }, null, 5L);
        }
    }
}
