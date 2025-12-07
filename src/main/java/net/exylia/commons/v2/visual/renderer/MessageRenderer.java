package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.v2.visual.color.MessageCenterer;
import net.exylia.commons.v2.visual.config.MessageConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MessageRenderer implements VisualRenderer<MessageConfig> {
    private static final MessageRenderer INSTANCE = new MessageRenderer();

    private MessageRenderer() {
    }

    public static MessageRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> renderAsync(Player player, MessageConfig config, PlaceholderContext context) {
        return AsyncExecutor.getInstance()
                .supplyAsync(() -> processMessages(config), false)
                .thenAcceptAsync(components -> {
                    SchedulerManager.getInstance().runSync(() -> {
                        if (config.isBroadcast()) {
                            sendToAll(components, config);
                        } else if (config.getFilter() != null) {
                            sendToFiltered(components, config);
                        } else if (config.getRecipients() != null) {
                            sendToRecipients(components, config);
                        } else if (player != null && player.isOnline()) {
                            sendToPlayer(player, components);
                        }
                    });
                }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    private List<Component> processMessages(MessageConfig config) {
        List<Component> components = new ArrayList<>();
        List<String> messages = config.getMessages();

        for (String message : messages) {
            String processed = message;

            if (config.isCentered()) {
                processed = MessageCenterer.center(processed);
            }

            Component component = ColorAPI.parse(processed);
            components.add(component);
        }

        return components;
    }

    private void sendToPlayer(Player player, List<Component> components) {
        for (Component component : components) {
            player.sendMessage(component);
        }
    }

    private void sendToRecipients(List<Component> components, MessageConfig config) {
        Collection<Player> recipients = config.getRecipients();
        Collection<Player> excluded = config.getExcluded();

        for (Player player : recipients) {
            if (excluded != null && excluded.contains(player)) {
                continue;
            }
            if (player.isOnline()) {
                sendToPlayer(player, components);
            }
        }
    }

    private void sendToAll(List<Component> components, MessageConfig config) {
        Collection<Player> excluded = config.getExcluded();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (excluded != null && excluded.contains(player)) {
                continue;
            }
            sendToPlayer(player, components);
        }
    }

    private void sendToFiltered(List<Component> components, MessageConfig config) {
        Collection<Player> excluded = config.getExcluded();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (excluded != null && excluded.contains(player)) {
                continue;
            }
            if (config.getFilter().test(player)) {
                sendToPlayer(player, components);
            }
        }
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }
}
