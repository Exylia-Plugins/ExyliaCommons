package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.visual.builder.MessageBuilder;
import net.exylia.commons.v2.visual.config.MessageConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.renderer.MessageRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class MessageAPI {
    private MessageAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void send(Player player, String message) {
        send(player, message, PlaceholderContext.create());
    }

    public static void send(Player player, String message, PlaceholderContext context) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .build();

        MessageRenderer.getInstance().render(player, config, context);
    }

    public static void send(Player player, List<String> messages) {
        send(player, messages, PlaceholderContext.create());
    }

    public static void send(Player player, List<String> messages, PlaceholderContext context) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .build();

        MessageRenderer.getInstance().render(player, config, context);
    }

    public static void send(UUID playerUUID, String message) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }
        send(player, message);
    }

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        if (sender instanceof Player player) {
            send(player, message);
        } else {
            sender.sendMessage(ColorAPI.parse(message));
        }
    }

    public static void broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void broadcast(List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void broadcastExcluding(String message, Player excludePlayer) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayer)
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void broadcastExcluding(String message, Collection<Player> excludePlayers) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayers)
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void sendToFiltered(Predicate<Player> filter, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .filter(filter)
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void sendToFiltered(Predicate<Player> filter, List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .filter(filter)
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void sendToRecipients(Collection<Player> recipients, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(recipients)
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void sendToRecipients(Collection<Player> recipients, List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(recipients)
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void sendInRadius(Location origin, double radius, String message) {
        if (origin == null || message == null || message.isEmpty()) {
            return;
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        sendToFiltered(filter, message);
    }

    public static void sendInRadius(Location origin, double radius, List<String> messages) {
        if (origin == null || messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        sendToFiltered(filter, messages);
    }

    public static void sendCentered(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .centered()
                .build();

        MessageRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void sendCentered(Player player, List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .centered()
                .build();

        MessageRenderer.getInstance().render(player, config, PlaceholderContext.create());
    }

    public static void broadcastCentered(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .centered()
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static void broadcastCentered(List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .centered()
                .build();

        MessageRenderer.getInstance().render(null, config, PlaceholderContext.create());
    }

    public static MessageBuilder builder() {
        return MessageBuilder.create();
    }

    public static void sendRoute(Player player, String route) {
        sendRoute(player, route, PlaceholderContext.create());
    }

    public static void sendRoute(Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route, context);
            send(player, messages, context);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route, context);
            send(player, message, context);
        }
    }

    public static void sendRoute(String filePath, Player player, String route) {
        sendRoute(filePath, player, route, PlaceholderContext.create());
    }

    public static void sendRoute(String filePath, Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context);
            send(player, messages, context);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context);
            send(player, message, context);
        }
    }

    public static void sendRoute(CommandSender sender, String route) {
        sendRoute(sender, route, PlaceholderContext.create());
    }

    public static void sendRoute(CommandSender sender, String route, PlaceholderContext context) {
        if (sender instanceof Player player) {
            sendRoute(player, route, context);
        } else {
            Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
            if (messageObj instanceof List) {
                List<String> messages = net.exylia.commons.v2.config.Messages.getList(route, context);
                messages.forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
            } else {
                String message = net.exylia.commons.v2.config.Messages.get(route, context);
                if (message != null && !message.isEmpty()) {
                    sender.sendMessage(ColorAPI.parse(message));
                }
            }
        }
    }

    public static void sendRoute(String filePath, CommandSender sender, String route) {
        sendRoute(filePath, sender, route, PlaceholderContext.create());
    }

    public static void sendRoute(String filePath, CommandSender sender, String route, PlaceholderContext context) {
        if (sender instanceof Player player) {
            sendRoute(filePath, player, route, context);
        } else {
            Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
            if (messageObj instanceof List) {
                List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context);
                messages.forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
            } else {
                String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context);
                if (message != null && !message.isEmpty()) {
                    sender.sendMessage(ColorAPI.parse(message));
                }
            }
        }
    }

    public static void broadcastRoute(String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            broadcast(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            broadcast(message);
        }
    }

    public static void broadcastRoute(String filePath, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route);
            broadcast(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
            broadcast(message);
        }
    }

    public static void broadcastRouteExcluding(String route, Player excludePlayer) {
        String message = net.exylia.commons.v2.config.Messages.get(route);
        broadcastExcluding(message, excludePlayer);
    }

    public static void broadcastRouteExcluding(String filePath, String route, Player excludePlayer) {
        String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
        broadcastExcluding(message, excludePlayer);
    }

    public static void broadcastRouteExcluding(String route, Collection<Player> excludePlayers) {
        String message = net.exylia.commons.v2.config.Messages.get(route);
        broadcastExcluding(message, excludePlayers);
    }

    public static void broadcastRouteExcluding(String filePath, String route, Collection<Player> excludePlayers) {
        String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
        broadcastExcluding(message, excludePlayers);
    }

    public static void sendRouteToFiltered(Predicate<Player> filter, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            sendToFiltered(filter, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            sendToFiltered(filter, message);
        }
    }

    public static void sendRouteToFiltered(String filePath, Predicate<Player> filter, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route);
            sendToFiltered(filter, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
            sendToFiltered(filter, message);
        }
    }

    public static void sendRouteToRecipients(Collection<Player> recipients, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            sendToRecipients(recipients, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            sendToRecipients(recipients, message);
        }
    }

    public static void sendRouteToRecipients(String filePath, Collection<Player> recipients, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route);
            sendToRecipients(recipients, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
            sendToRecipients(recipients, message);
        }
    }

    public static void sendRouteInRadius(Location origin, double radius, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            sendInRadius(origin, radius, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            sendInRadius(origin, radius, message);
        }
    }

    public static void sendRouteInRadius(String filePath, Location origin, double radius, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route);
            sendInRadius(origin, radius, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
            sendInRadius(origin, radius, message);
        }
    }

    public static void sendRouteCentered(Player player, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            sendCentered(player, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            sendCentered(player, message);
        }
    }

    public static void sendRouteCentered(String filePath, Player player, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route);
            sendCentered(player, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
            sendCentered(player, message);
        }
    }

    public static void broadcastRouteCentered(String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            broadcastCentered(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            broadcastCentered(message);
        }
    }

    public static void broadcastRouteCentered(String filePath, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getListFrom(filePath, route);
            broadcastCentered(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route);
            broadcastCentered(message);
        }
    }
}
