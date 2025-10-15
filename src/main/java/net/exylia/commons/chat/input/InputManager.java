package net.exylia.commons.chat.input;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.configSimple.Messages;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.utils.visuals.MessageUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import net.kyori.adventure.text.Component;
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

public final class InputManager implements Listener {

    private static InputManager instance;
    private static ExyliaPlugin plugin;

    private final Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();

    private InputManager() {}

    public static void init(ExyliaPlugin pluginInstance) {
        if (instance != null) {
            return;
        }

        plugin = pluginInstance;
        instance = new InputManager();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    public static <T> CompletableFuture<T> requestInput(Player player, InputHandler<T> handler) {
        if (instance == null) {
            throw new IllegalStateException("InputManager not initialized");
        }

        cancelInput(player);

        InputSession session = new InputSession(player, handler);
        instance.activeSessions.put(player.getUniqueId(), session);

        handler.onStart(player);

        TitleUtils.sendCountdownTitle(player, "input_timeout",
                new TitleConfig(Messages.get("system.input.title.title"), Messages.get("system.input.title.subtitle"), true),
                30,
                ExyliaContext.create()
        );
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

    public static boolean hasActiveInput(Player player) {
        return instance != null && instance.activeSessions.containsKey(player.getUniqueId());
    }

    static InputSession getActiveSession(Player player) {
        return instance != null ? instance.activeSessions.get(player.getUniqueId()) : null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        InputSession session = activeSessions.get(player.getUniqueId());

        if (session != null) {
            event.setCancelled(true);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                 
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
                        sendResultMessage(player, result);
                    }
                    break;

                case RETRY:
                    if (result.getMessage() != null) {
                        sendResultMessage(player, result);
                    }
                    handler.onStart(player);
                    break;

                case CANCEL:
                    activeSessions.remove(player.getUniqueId());
                    session.cancel();
                    if (result.getMessage() != null) {
                        sendResultMessage(player, result);
                    }
                    TitleUtils.cancelTitle(player, "input_timeout");
                    break;
            }
        } catch (Exception e) {
            activeSessions.remove(player.getUniqueId());
            session.completeExceptionally(e);
        }
    }

    private static void sendResultMessage(Player player, InputResult result) {
        if (result.hasStringMessage()) {
            MessageUtils.sendMessage(player, result.getMessageAsString());
        } else if (result.hasComponentMessage()) {
            MessageUtils.sendMessage(player, result.getMessageAsComponent());
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.activeSessions.values().forEach(InputSession::cancel);
            instance.activeSessions.clear();
            instance = null;
            plugin = null;
        }
    }
}
