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
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class MessageAPI {
    private MessageAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<Void> send(Player player, String message) {
        return send(player, message, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> send(Player player, String message, PlaceholderContext context) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, context);
    }

    public static void sendSync(Player player, String message) {
        sendSync(player, message, PlaceholderContext.create());
    }

    public static void sendSync(Player player, String message, PlaceholderContext context) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .build();

        MessageRenderer.getInstance().renderSync(player, config, context);
    }

    public static CompletableFuture<Void> send(Player player, List<String> messages) {
        return send(player, messages, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> send(Player player, List<String> messages, PlaceholderContext context) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, context);
    }

    public static CompletableFuture<Void> send(UUID playerUUID, String message) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return CompletableFuture.completedFuture(null);
        }
        return send(player, message);
    }

    public static CompletableFuture<Void> send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (sender instanceof Player player) {
            return send(player, message);
        } else {
            sender.sendMessage(ColorAPI.parse(message));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Void> broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcast(List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastExcluding(String message, Player excludePlayer) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayer)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastExcluding(String message, Collection<Player> excludePlayers) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayers)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToFiltered(Predicate<Player> filter, String message) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .filter(filter)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToFiltered(Predicate<Player> filter, List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .filter(filter)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToRecipients(Collection<Player> recipients, String message) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(recipients)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToRecipients(Collection<Player> recipients, List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(recipients)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendInRadius(Location origin, double radius, String message) {
        if (origin == null || message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return sendToFiltered(filter, message);
    }

    public static CompletableFuture<Void> sendInRadius(Location origin, double radius, List<String> messages) {
        if (origin == null || messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return sendToFiltered(filter, messages);
    }

    public static CompletableFuture<Void> sendCentered(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendCentered(Player player, List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastCentered(String message) {
        if (message == null || message.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastCentered(List<String> messages) {
        if (messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static MessageBuilder builder() {
        return MessageBuilder.create();
    }

    public static CompletableFuture<Void> sendRoute(Player player, String route) {
        return sendRoute(player, route, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendRoute(Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route, context);
            return send(player, messages, context);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route, context);
            return send(player, message, context);
        }
    }

    public static CompletableFuture<Void> sendRoute(String filePath, Player player, String route) {
        return sendRoute(filePath, player, route, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendRoute(String filePath, Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route, context);
            return send(player, messages, context);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route, context);
            return send(player, message, context);
        }
    }

    public static CompletableFuture<Void> sendRoute(CommandSender sender, String route) {
        if (sender instanceof Player player) {
            return sendRoute(player, route);
        } else {
            Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
            if (messageObj instanceof List) {
                List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
                messages.forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
            } else {
                String message = net.exylia.commons.v2.config.Messages.get(route);
                if (message != null && !message.isEmpty()) {
                    sender.sendMessage(ColorAPI.parse(message));
                }
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Void> sendRoute(String filePath, CommandSender sender, String route) {
        if (sender instanceof Player player) {
            return sendRoute(filePath, player, route);
        } else {
            Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
            if (messageObj instanceof List) {
                List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
                messages.forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
            } else {
                String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
                if (message != null && !message.isEmpty()) {
                    sender.sendMessage(ColorAPI.parse(message));
                }
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Void> broadcastRoute(String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            return broadcast(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            return broadcast(message);
        }
    }

    public static CompletableFuture<Void> broadcastRoute(String filePath, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
            return broadcast(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
            return broadcast(message);
        }
    }

    public static CompletableFuture<Void> broadcastRouteExcluding(String route, Player excludePlayer) {
        String message = net.exylia.commons.v2.config.Messages.get(route);
        return broadcastExcluding(message, excludePlayer);
    }

    public static CompletableFuture<Void> broadcastRouteExcluding(String filePath, String route, Player excludePlayer) {
        String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
        return broadcastExcluding(message, excludePlayer);
    }

    public static CompletableFuture<Void> broadcastRouteExcluding(String route, Collection<Player> excludePlayers) {
        String message = net.exylia.commons.v2.config.Messages.get(route);
        return broadcastExcluding(message, excludePlayers);
    }

    public static CompletableFuture<Void> broadcastRouteExcluding(String filePath, String route, Collection<Player> excludePlayers) {
        String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
        return broadcastExcluding(message, excludePlayers);
    }

    public static CompletableFuture<Void> sendRouteToFiltered(Predicate<Player> filter, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            return sendToFiltered(filter, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            return sendToFiltered(filter, message);
        }
    }

    public static CompletableFuture<Void> sendRouteToFiltered(String filePath, Predicate<Player> filter, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
            return sendToFiltered(filter, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
            return sendToFiltered(filter, message);
        }
    }

    public static CompletableFuture<Void> sendRouteToRecipients(Collection<Player> recipients, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            return sendToRecipients(recipients, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            return sendToRecipients(recipients, message);
        }
    }

    public static CompletableFuture<Void> sendRouteToRecipients(String filePath, Collection<Player> recipients, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
            return sendToRecipients(recipients, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
            return sendToRecipients(recipients, message);
        }
    }

    public static CompletableFuture<Void> sendRouteInRadius(Location origin, double radius, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            return sendInRadius(origin, radius, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            return sendInRadius(origin, radius, message);
        }
    }

    public static CompletableFuture<Void> sendRouteInRadius(String filePath, Location origin, double radius, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
            return sendInRadius(origin, radius, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
            return sendInRadius(origin, radius, message);
        }
    }

    public static CompletableFuture<Void> sendRouteCentered(Player player, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            return sendCentered(player, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            return sendCentered(player, message);
        }
    }

    public static CompletableFuture<Void> sendRouteCentered(String filePath, Player player, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
            return sendCentered(player, messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
            return sendCentered(player, message);
        }
    }

    public static CompletableFuture<Void> broadcastRouteCentered(String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(route);
            return broadcastCentered(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(route);
            return broadcastCentered(message);
        }
    }

    public static CompletableFuture<Void> broadcastRouteCentered(String filePath, String route) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(filePath, route);
        if (messageObj instanceof List) {
            List<String> messages = net.exylia.commons.v2.config.Messages.getList(filePath, route);
            return broadcastCentered(messages);
        } else {
            String message = net.exylia.commons.v2.config.Messages.get(filePath, route);
            return broadcastCentered(message);
        }
    }
}
