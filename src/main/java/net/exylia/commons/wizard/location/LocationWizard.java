package net.exylia.commons.wizard.location;

import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.utils.visuals.ActionBarUtils;
import net.exylia.commons.utils.visuals.MessageUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LocationWizard implements Listener {

    private static LocationWizard instance;
    private static JavaPlugin plugin;

    private final Map<UUID, WizardSession> activeSessions = new ConcurrentHashMap<>();

    private LocationWizard() {}

    public static void init(JavaPlugin pluginInstance) {
        if (instance != null) {
            return;
        }

        plugin = pluginInstance;
        instance = new LocationWizard();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    public static <T> CompletableFuture<T> startWizard(Player player, int positionsNeeded, WizardHandler<T> handler) {
        if (instance == null) {
            throw new IllegalStateException("LocationWizard not initialized");
        }

        if (positionsNeeded < 1) {
            throw new IllegalArgumentException("Positions needed must be at least 1");
        }

        cancelWizard(player);

        WizardSession session = new WizardSession(player, positionsNeeded, handler);
        instance.activeSessions.put(player.getUniqueId(), session);

        handler.onStart(player, positionsNeeded);
        sendInstructions(player, session);

        return session.getFuture();
    }

    public static boolean cancelWizard(Player player) {
        if (instance == null) {
            return false;
        }

        WizardSession session = instance.activeSessions.remove(player.getUniqueId());
        if (session != null) {
            TitleUtils.cancelTitle(player, "location_wizard");
            ActionBarUtils.cancelActionBar(player, "location_wizard");
            session.cancel();
            player.resetTitle();
            return true;
        }
        return false;
    }

    public static boolean hasActiveWizard(Player player) {
        return instance != null && instance.activeSessions.containsKey(player.getUniqueId());
    }

    public static int getRemainingPositions(Player player) {
        if (instance == null) {
            return 0;
        }

        WizardSession session = instance.activeSessions.get(player.getUniqueId());
        return session != null ? session.getRemainingPositions() : 0;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        WizardSession session = activeSessions.get(player.getUniqueId());

        if (session == null) {
            return;
        }

        if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) &&
                player.isSneaking()) {

            event.setCancelled(true);

            Location location = player.getLocation();
            handleLocationSelection(player, location);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelWizard(event.getPlayer());
    }

    @SuppressWarnings("unchecked")
    private void handleLocationSelection(Player player, Location location) {
        WizardSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        try {
            session.addLocation(location);

            TitleUtils.cancelTitle(player, "location_wizard");
            ActionBarUtils.cancelActionBar(player, "location_wizard");

            WizardHandler<Object> handler = (WizardHandler<Object>) session.getHandler();
            WizardResult result = handler.onLocationSelected(player, location, session.getSelectedLocations(), session.getRemainingPositions());

            switch (result.getType()) {
                case CONTINUE:
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessage(player, result.getMessage());
                    }
                    sendInstructions(player, session);
                    break;

                case COMPLETE:
                    activeSessions.remove(player.getUniqueId());
                    player.resetTitle();
                    session.complete(result.getValue());
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessage(player, result.getMessage());
                    }
                    break;

                case CANCEL:
                    activeSessions.remove(player.getUniqueId());
                    player.resetTitle();
                    session.cancel();
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessage(player, result.getMessage());
                    }
                    break;
            }
        } catch (Exception e) {
            activeSessions.remove(player.getUniqueId());
            player.resetTitle();
            session.completeExceptionally(e);
        }
    }

    private static void sendInstructions(Player player, WizardSession session) {
        int remaining = session.getRemainingPositions();
        int total = session.getTotalPositions();
        int current = total - remaining + 1;

        TitleUtils.sendTitle(player,
                "location_wizard",
                new TitleConfig(
                        "{warning}⚡ Use SHIFT + LEFT CLICK", "{info}Position " + current + "/" + total, true, 20L),
                ExyliaContext.create());

        ActionBarUtils.sendActionBar(player,
                "location_wizard",
                new ActionBarConfig(
                        "{warning}Remaining positions: {info}" + remaining,
                        20L),
                ExyliaContext.create());
    }

    public static void shutdown() {
        if (instance != null) {
            instance.activeSessions.values().forEach(WizardSession::cancel);
            instance.activeSessions.clear();
            instance = null;
            plugin = null;
        }
    }
}
