package net.exylia.commons.wizard.generic;

import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.utils.visuals.ActionBarUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class GenericWizard implements Listener {

    private static GenericWizard instance;
    private static JavaPlugin plugin;

    private final Map<UUID, GenericWizardSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInteractionTime = new ConcurrentHashMap<>();
    private static final long INTERACTION_COOLDOWN_MS = 50;

    private GenericWizard() {}

    public static void init(JavaPlugin pluginInstance) {
        if (instance != null) {
            return;
        }

        plugin = pluginInstance;
        instance = new GenericWizard();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    public static <T> CompletableFuture<T> startWizard(Player player, GenericWizardHandler<T> handler) {
        if (instance == null) {
            throw new IllegalStateException("GenericWizard not initialized");
        }

        cancelWizard(player);

        GenericWizardSession session = new GenericWizardSession(player, handler);
        instance.activeSessions.put(player.getUniqueId(), session);

        handler.onStart(player);

        return session.getFuture();
    }

    public static <T> CompletableFuture<T> startWizard(Player player, GenericWizardHandler<T> handler,
                                                       TitleConfig titleConfig, ActionBarConfig actionBarConfig, ExyliaContext context) {
        CompletableFuture<T> future = startWizard(player, handler);

        updateDisplay(player, titleConfig, actionBarConfig, context);

        return future;
    }

    public static void updateDisplay(Player player, TitleConfig titleConfig, ActionBarConfig actionBarConfig, ExyliaContext context) {
        if (!hasActiveWizard(player)) {
            return;
        }
        if (context == null) {
            context = ExyliaContext.create()
                    .withPlayer(player)
                    .put("wizard_active", true);
        } else {
            context.put("wizard_active", true);
        }

        if (titleConfig != null && titleConfig.isEnabled()) {
            TitleUtils.sendTitle(player, "generic_wizard", titleConfig, context);
        }

        if (actionBarConfig != null && actionBarConfig.isEnabled()) {
            ActionBarUtils.sendActionBar(player, "generic_wizard", actionBarConfig, context);
        }
    }

    public static void updateTitle(Player player, TitleConfig titleConfig) {
        updateDisplay(player, titleConfig, null, null);
    }

    public static void updateActionBar(Player player, ActionBarConfig actionBarConfig) {
        updateDisplay(player, null, actionBarConfig, null);
    }

    public static boolean cancelWizard(Player player) {
        if (instance == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        GenericWizardSession session = instance.activeSessions.remove(playerId);
        if (session != null) {
            instance.lastInteractionTime.remove(playerId);
            session.cancel();
            clearDisplay(player);
            return true;
        }
        return false;
    }

    public static boolean completeWizard(Player player, Object result) {
        if (instance == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        GenericWizardSession session = instance.activeSessions.remove(playerId);
        if (session != null) {
            instance.lastInteractionTime.remove(playerId);
            session.complete(result);
            clearDisplay(player);
            return true;
        }
        return false;
    }

    public static boolean hasActiveWizard(Player player) {
        return instance != null && instance.activeSessions.containsKey(player.getUniqueId());
    }

    public static GenericWizardSession getSession(Player player) {
        if (instance == null) {
            return null;
        }
        return instance.activeSessions.get(player.getUniqueId());
    }

    private static void clearDisplay(Player player) {
        player.resetTitle();
         
        TitleUtils.cancelTitle(player, "generic_wizard");
        ActionBarUtils.cancelActionBar(player, "generic_wizard");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        GenericWizardSession session = activeSessions.get(playerId);

        if (session == null) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteractionTime.get(playerId);

        if (lastTime != null && (currentTime - lastTime) < INTERACTION_COOLDOWN_MS) {
            return;
        }

        lastInteractionTime.put(playerId, currentTime);

        GenericWizardHandler<?> handler = session.getHandler();
        WizardActionResult result = null;

        try {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
                event.setCancelled(true);
                result = handler.onLeftClick(player, event);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);
                result = handler.onRightClick(player, event);
            }

            if (result != null) {
                handleActionResult(player, session, result);
            }

        } catch (Exception e) {
            activeSessions.remove(playerId);
            lastInteractionTime.remove(playerId);
            clearDisplay(player);
            session.completeExceptionally(e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelWizard(event.getPlayer());
    }

    @SuppressWarnings("unchecked")
    private void handleActionResult(Player player, GenericWizardSession session, WizardActionResult result) {
        UUID playerId = player.getUniqueId();

        switch (result.getType()) {
            case CONTINUE:
                 
                if (result.getTitleConfig() != null || result.getActionBarConfig() != null) {
                    updateDisplay(player, result.getTitleConfig(), result.getActionBarConfig(), result.getContext());
                }
                break;

            case COMPLETE:
                activeSessions.remove(playerId);
                lastInteractionTime.remove(playerId);
                clearDisplay(player);
                session.complete(result.getValue());
                break;

            case CANCEL:
                activeSessions.remove(playerId);
                lastInteractionTime.remove(playerId);
                clearDisplay(player);
                session.cancel();
                break;
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.activeSessions.values().forEach(session -> {
                clearDisplay(session.getPlayer());
                session.cancel();
            });
            instance.activeSessions.clear();
            instance = null;
            plugin = null;
        }
    }
}
