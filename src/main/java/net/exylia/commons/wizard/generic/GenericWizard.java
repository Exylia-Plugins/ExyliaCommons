package net.exylia.commons.wizard.generic;

import net.exylia.commons.utils.ActionBarUtils;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.TitleUtils;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic customizable wizard for any type of interaction
 * Supports custom actions for SHIFT + LEFT CLICK and SHIFT + RIGHT CLICK
 */
public final class GenericWizard implements Listener {

    private static GenericWizard instance;
    private static ExyliaPlugin plugin;

    private final Map<UUID, GenericWizardSession> activeSessions = new ConcurrentHashMap<>();

    private GenericWizard() {}

    /**
     * Initialize the GenericWizard
     */
    public static void init(ExyliaPlugin pluginInstance) {
        if (instance != null) {
            return;
        }

        plugin = pluginInstance;
        instance = new GenericWizard();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    /**
     * Start a generic wizard session
     *
     * @param player The player
     * @param handler Custom handler for wizard actions
     * @return CompletableFuture with the result
     */
    public static <T> CompletableFuture<T> startWizard(Player player, GenericWizardHandler<T> handler) {
        if (instance == null) {
            throw new IllegalStateException("GenericWizard not initialized");
        }

        // Cancel existing session if any
        cancelWizard(player);

        GenericWizardSession session = new GenericWizardSession(player, handler);
        instance.activeSessions.put(player.getUniqueId(), session);

        // Start the wizard
        handler.onStart(player);

        return session.getFuture();
    }

    /**
     * Start wizard with initial display configuration
     */
    public static <T> CompletableFuture<T> startWizard(Player player, GenericWizardHandler<T> handler,
                                                       WizardDisplayConfig displayConfig) {
        CompletableFuture<T> future = startWizard(player, handler);

        if (displayConfig != null) {
            updateDisplay(player, displayConfig);
        }

        return future;
    }

    /**
     * Update display (title/actionbar) for active wizard
     */
    public static void updateDisplay(Player player, WizardDisplayConfig config) {
        if (!hasActiveWizard(player)) {
            return;
        }

        if (config.getTitleConfig() != null) {
            WizardDisplayConfig.TitleConfig titleConfig = config.getTitleConfig();
            TitleUtils.TitleBuilder builder = TitleUtils.create()
                    .id(titleConfig.getId())
                    .title(titleConfig.getTitle());

            if (titleConfig.getSubtitle() != null) {
                builder.subtitle(titleConfig.getSubtitle());
            }
            builder.permanent();
            builder.send(player);
        }

        if (config.getActionBarConfig() != null) {
            WizardDisplayConfig.ActionBarConfig actionBarConfig = config.getActionBarConfig();
            ActionBarUtils.ActionBarBuilder builder = ActionBarUtils.create()
                    .id(actionBarConfig.getId())
                    .text(actionBarConfig.getText());
            builder.permanent(20);

            builder.send(player);
        }
    }

    /**
     * Cancel active wizard for player
     */
    public static boolean cancelWizard(Player player) {
        if (instance == null) {
            return false;
        }

        GenericWizardSession session = instance.activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
            clearDisplay(player);
            return true;
        }
        return false;
    }

    /**
     * Complete active wizard for player
     */
    public static boolean completeWizard(Player player, Object result) {
        if (instance == null) {
            return false;
        }

        GenericWizardSession session = instance.activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.complete(result);
            clearDisplay(player);
            return true;
        }
        return false;
    }

    /**
     * Check if player has active wizard
     */
    public static boolean hasActiveWizard(Player player) {
        return instance != null && instance.activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Get active session for player
     */
    public static GenericWizardSession getSession(Player player) {
        if (instance == null) {
            return null;
        }
        return instance.activeSessions.get(player.getUniqueId());
    }

    /**
     * Clear all display elements for player
     */
    private static void clearDisplay(Player player) {
        player.resetTitle();
        // Clear common wizard display IDs
        TitleUtils.cancelTitle(player, "generic_wizard");
        ActionBarUtils.cancelActionBar(player, "generic_wizard");
    }

    // ===== EVENT HANDLERS =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GenericWizardSession session = activeSessions.get(player.getUniqueId());

        if (session == null) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

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
            activeSessions.remove(player.getUniqueId());
            clearDisplay(player);
            session.completeExceptionally(e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelWizard(event.getPlayer());
    }

    // ===== PRIVATE METHODS =====

    @SuppressWarnings("unchecked")
    private void handleActionResult(Player player, GenericWizardSession session, WizardActionResult result) {
        switch (result.getType()) {
            case CONTINUE:
                // Update display if provided
                if (result.getDisplayConfig() != null) {
                    updateDisplay(player, result.getDisplayConfig());
                }
                break;

            case COMPLETE:
                activeSessions.remove(player.getUniqueId());
                clearDisplay(player);
                session.complete(result.getValue());
                break;

            case CANCEL:
                activeSessions.remove(player.getUniqueId());
                clearDisplay(player);
                session.cancel();
                break;
        }
    }

    /**
     * Shutdown the GenericWizard
     */
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