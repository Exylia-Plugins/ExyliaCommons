package net.exylia.commons.v2.chat.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Getter;
import net.exylia.commons.v2.chat.config.ChatInputConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.TitleAPI;
import net.exylia.commons.v2.visual.builder.TitleBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class ChatInputManager implements Listener {

    @Getter
    private static ChatInputManager instance;

    private static Plugin plugin;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private ChatInputManager() {}

    public static void init(Plugin pluginInstance) {
        if (instance != null) return;
        plugin = pluginInstance;
        instance = new ChatInputManager();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    public static void shutdown() {
        if (instance == null) return;
        instance.sessions.values().forEach(Session::cancel);
        instance.sessions.clear();
        HandlerList.unregisterAll(instance);
        instance = null;
        plugin = null;
    }

    public void startSession(
        Player player,
        ChatInputConfig config,
        Consumer<String> callback
    ) {
        cancelSession(player);

        Session session = new Session(player, config, callback);
        sessions.put(player.getUniqueId(), session);

        if (config.isCloseInventory()) {
            player.closeInventory();
        }

        if (config.getPrompt() != null) {
            MessageAPI.send(player, config.getPrompt());
        }

        if (config.isShowTitle()) {
            startTitleCountdown(player, session);
        }

        session.timeoutTask = TaskAPI.syncLater(
            () -> {
                if (sessions.remove(player.getUniqueId()) != null) {
                    session.cancelled = true;
                    if (config.getTimeoutMessage() != null) {
                        MessageAPI.send(player, config.getTimeoutMessage());
                    }
                    if (config.getOnTimeout() != null) {
                        config.getOnTimeout().run();
                    }
                    TitleAPI.cancelAll(player);
                }
            },
            config.getTimeout() * 20L
        );
    }

    public void cancelSession(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
            TitleAPI.cancelAll(player);
        }
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        String input = event.getMessage();

        TaskAPI.sync(() -> handleInput(event.getPlayer(), input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSession(event.getPlayer());
    }

    private void handleInput(Player player, String input) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.cancelled) return;

        ChatInputConfig config = session.config;

        if (input.equalsIgnoreCase(config.getCancelWord())) {
            sessions.remove(player.getUniqueId());
            session.cancel();
            TitleAPI.cancelAll(player);
            if (config.getCancelMessage() != null) {
                MessageAPI.send(player, config.getCancelMessage());
            }
            if (config.getOnCancel() != null) {
                config.getOnCancel().run();
            }
            return;
        }

        if (
            config.getValidator() != null && !config.getValidator().test(input)
        ) {
            if (config.getInvalidMessage() != null) {
                MessageAPI.send(player, config.getInvalidMessage());
            }
            return;
        }

        sessions.remove(player.getUniqueId());
        session.cancel();
        TitleAPI.cancelAll(player);
        session.callback.accept(input);
    }

    private void startTitleCountdown(Player player, Session session) {
        ChatInputConfig config = session.config;
        final int[] remaining = { config.getTimeout() };

        session.titleTask = TaskAPI.syncTimer(
            () -> {
                if (
                    !sessions.containsKey(player.getUniqueId()) ||
                    session.cancelled
                ) {
                    if (session.titleTask != null) session.titleTask.cancel();
                    return;
                }

                PlaceholderContext ctx = PlaceholderContext.create().put(
                    "time",
                    remaining[0]
                );
                TitleAPI.send(
                    player,
                    TitleBuilder.create()
                        .title(config.getTitleText())
                        .subtitle(config.getSubtitleText())
                        .times(0, 25, 0)
                        .build(),
                    ctx
                );
                remaining[0]--;
            },
            0L,
            20L
        );
    }

    private static class Session {

        final Player player;
        final ChatInputConfig config;
        final Consumer<String> callback;
        ScheduledTask timeoutTask;
        ScheduledTask titleTask;
        volatile boolean cancelled = false;

        Session(
            Player player,
            ChatInputConfig config,
            Consumer<String> callback
        ) {
            this.player = player;
            this.config = config;
            this.callback = callback;
        }

        void cancel() {
            cancelled = true;
            if (timeoutTask != null) timeoutTask.cancel();
            if (titleTask != null) titleTask.cancel();
        }
    }
}
