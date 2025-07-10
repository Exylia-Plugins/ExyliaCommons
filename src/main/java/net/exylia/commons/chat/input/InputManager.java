package net.exylia.commons.chat.input;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.commons.utils.TitleUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static Input Manager for handling custom player inputs
 * Simple, lightweight and customizable
 */
public final class InputManager implements Listener {

    private static InputManager instance;
    private static ExyliaPlugin plugin;

    private final Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();

    private InputManager() {}

    /**
     * Initialize the InputManager
     */
    public static void init(ExyliaPlugin pluginInstance) {
        if (instance != null) {
            return;
        }

        plugin = pluginInstance;
        instance = new InputManager();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    /**
     * Request custom input from player
     *
     * @param player The player to request input from
     * @param handler Custom handler to process the input
     * @return CompletableFuture with the result
     */
    public static <T> CompletableFuture<T> requestInput(Player player, InputHandler<T> handler) {
        if (instance == null) {
            throw new IllegalStateException("InputManager not initialized");
        }

        // Cancel existing session if any
        cancelInput(player);

        InputSession session = new InputSession(player, handler);
        instance.activeSessions.put(player.getUniqueId(), session);

        // Start the input process
        handler.onStart(player);

        TitleUtils.create().fadeIn(0).fadeOut(0).countdown(30).title("{primary}Enter your input in chat.").subtitle("{info}%time%s remaining.").id("input_timeout").sendAsync(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            InputSession timeoutSession = instance.activeSessions.get(player.getUniqueId());
            if (timeoutSession == session) {
                instance.activeSessions.remove(player.getUniqueId());
                session.cancel();
                TitleUtils.cancelTitle(player, "input_timeout");
            }
        }, 30 * 20L);

        return session.getFuture();
    }

    /**
     * Cancel active input for player
     */
    public static boolean cancelInput(Player player) {
        if (instance == null) {
            return false;
        }

        InputSession session = instance.activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
            return true;
        }
        return false;
    }

    /**
     * Check if player has active input
     */
    public static boolean hasActiveInput(Player player) {
        return instance != null && instance.activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Get active session for player (internal use)
     */
    static InputSession getActiveSession(Player player) {
        return instance != null ? instance.activeSessions.get(player.getUniqueId()) : null;
    }

    // ===== EVENT HANDLERS =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        InputSession session = activeSessions.get(player.getUniqueId());

        if (session != null) {
            event.setCancelled(true);

            // Handle on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Check if session still exists (might have been cancelled)
                if (activeSessions.containsKey(player.getUniqueId())) {
                    handleInput(player, event.getMessage());
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelInput(event.getPlayer());
    }

    // ===== PRIVATE METHODS =====

    @SuppressWarnings("unchecked")
    private void handleInput(Player player, String input) {
        InputSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        try {
            InputHandler<Object> handler = (InputHandler<Object>) session.getHandler();
            InputResult result = handler.onInput(player, input);

            switch (result.getType()) {
                case SUCCESS:
                    activeSessions.remove(player.getUniqueId());
                    session.complete(result.getValue());
                    TitleUtils.cancelTitle(player, "input_timeout");
                    break;

                case INVALID:
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessageAsync(player, result.getMessage());
                    }
                    TitleUtils.cancelTitle(player, "input_timeout");
                    break;

                case CANCEL:
                    activeSessions.remove(player.getUniqueId());
                    session.cancel();
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessageAsync(player, result.getMessage());
                    }
                    TitleUtils.cancelTitle(player, "input_timeout");
                    break;
            }
        } catch (Exception e) {
            activeSessions.remove(player.getUniqueId());
            session.completeExceptionally(e);
        }
    }

    /**
     * Shutdown the InputManager
     */
    public static void shutdown() {
        if (instance != null) {
            instance.activeSessions.values().forEach(InputSession::cancel);
            instance.activeSessions.clear();
            instance = null;
            plugin = null;
        }
    }
}