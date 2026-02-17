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

    private static PlaceholderContext ctx(PlaceholderContext context) {
        return context != null ? context : PlaceholderContext.create();
    }

    private static boolean isEmpty(String message) {
        return message == null || message.isEmpty();
    }

    private static boolean isEmpty(List<String> messages) {
        return messages == null || messages.isEmpty() || messages.stream().allMatch(msg -> msg == null || msg.isEmpty());
    }

    public static MessageBuilder builder() {
        return MessageBuilder.create();
    }

    // ─── send (Player) ───

    public static void send(Player player, String message) {
        send(player, message, PlaceholderContext.create());
    }

    public static void send(Player player, String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

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
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .build();

        MessageRenderer.getInstance().render(player, config, context);
    }

    // ─── send (UUID) ───

    public static void send(UUID playerUUID, String message) {
        send(playerUUID, message, PlaceholderContext.create());
    }

    public static void send(UUID playerUUID, String message, PlaceholderContext context) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        send(player, message, context);
    }

    public static void send(UUID playerUUID, List<String> messages) {
        send(playerUUID, messages, PlaceholderContext.create());
    }

    public static void send(UUID playerUUID, List<String> messages, PlaceholderContext context) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        send(player, messages, context);
    }

    // ─── send (CommandSender) ───

    public static void send(CommandSender sender, String message) {
        send(sender, message, PlaceholderContext.create());
    }

    public static void send(CommandSender sender, String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

        if (sender instanceof Player player) {
            send(player, message, context);
        } else {
            sender.sendMessage(ColorAPI.parse(message));
        }
    }

    public static void send(CommandSender sender, List<String> messages) {
        send(sender, messages, PlaceholderContext.create());
    }

    public static void send(CommandSender sender, List<String> messages, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        if (sender instanceof Player player) {
            send(player, messages, context);
        } else {
            messages.stream()
                    .filter(msg -> !isEmpty(msg))
                    .forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
        }
    }

    // ─── broadcast ───

    public static void broadcast(String message) {
        broadcast(message, PlaceholderContext.create());
    }

    public static void broadcast(String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void broadcast(List<String> messages) {
        broadcast(messages, PlaceholderContext.create());
    }

    public static void broadcast(List<String> messages, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    // ─── broadcastExcluding ───

    public static void broadcastExcluding(String message, Player excludePlayer) {
        broadcastExcluding(message, excludePlayer, PlaceholderContext.create());
    }

    public static void broadcastExcluding(String message, Player excludePlayer, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayer)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void broadcastExcluding(String message, Collection<Player> excludePlayers) {
        broadcastExcluding(message, excludePlayers, PlaceholderContext.create());
    }

    public static void broadcastExcluding(String message, Collection<Player> excludePlayers, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayers)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void broadcastExcluding(List<String> messages, Player excludePlayer) {
        broadcastExcluding(messages, excludePlayer, PlaceholderContext.create());
    }

    public static void broadcastExcluding(List<String> messages, Player excludePlayer, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .excluding(excludePlayer)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void broadcastExcluding(List<String> messages, Collection<Player> excludePlayers) {
        broadcastExcluding(messages, excludePlayers, PlaceholderContext.create());
    }

    public static void broadcastExcluding(List<String> messages, Collection<Player> excludePlayers, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .excluding(excludePlayers)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    // ─── sendToFiltered ───

    public static void sendToFiltered(Predicate<Player> filter, String message) {
        sendToFiltered(filter, message, PlaceholderContext.create());
    }

    public static void sendToFiltered(Predicate<Player> filter, String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .filter(filter)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void sendToFiltered(Predicate<Player> filter, List<String> messages) {
        sendToFiltered(filter, messages, PlaceholderContext.create());
    }

    public static void sendToFiltered(Predicate<Player> filter, List<String> messages, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .filter(filter)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    // ─── sendToRecipients ───

    public static void sendToRecipients(Collection<Player> recipients, String message) {
        sendToRecipients(recipients, message, PlaceholderContext.create());
    }

    public static void sendToRecipients(Collection<Player> recipients, String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(recipients)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void sendToRecipients(Collection<Player> recipients, List<String> messages) {
        sendToRecipients(recipients, messages, PlaceholderContext.create());
    }

    public static void sendToRecipients(Collection<Player> recipients, List<String> messages, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(recipients)
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    // ─── sendInRadius ───

    public static void sendInRadius(Location origin, double radius, String message) {
        sendInRadius(origin, radius, message, PlaceholderContext.create());
    }

    public static void sendInRadius(Location origin, double radius, String message, PlaceholderContext context) {
        if (origin == null || isEmpty(message)) return;

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        sendToFiltered(filter, message, context);
    }

    public static void sendInRadius(Location origin, double radius, List<String> messages) {
        sendInRadius(origin, radius, messages, PlaceholderContext.create());
    }

    public static void sendInRadius(Location origin, double radius, List<String> messages, PlaceholderContext context) {
        if (origin == null || isEmpty(messages)) return;

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        sendToFiltered(filter, messages, context);
    }

    // ─── sendCentered ───

    public static void sendCentered(Player player, String message) {
        sendCentered(player, message, PlaceholderContext.create());
    }

    public static void sendCentered(Player player, String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .centered()
                .build();

        MessageRenderer.getInstance().render(player, config, ctx(context));
    }

    public static void sendCentered(Player player, List<String> messages) {
        sendCentered(player, messages, PlaceholderContext.create());
    }

    public static void sendCentered(Player player, List<String> messages, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .centered()
                .build();

        MessageRenderer.getInstance().render(player, config, ctx(context));
    }

    // ─── broadcastCentered ───

    public static void broadcastCentered(String message) {
        broadcastCentered(message, PlaceholderContext.create());
    }

    public static void broadcastCentered(String message, PlaceholderContext context) {
        if (isEmpty(message)) return;

        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .centered()
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    public static void broadcastCentered(List<String> messages) {
        broadcastCentered(messages, PlaceholderContext.create());
    }

    public static void broadcastCentered(List<String> messages, PlaceholderContext context) {
        if (isEmpty(messages)) return;

        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .centered()
                .build();

        MessageRenderer.getInstance().render(null, config, ctx(context));
    }

    // ─── sendRoute ───

    public static void sendRoute(Player player, String route) {
        sendRoute(player, route, PlaceholderContext.create());
    }

    public static void sendRoute(Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            send(player, net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            send(player, net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void sendRoute(String filePath, Player player, String route) {
        sendRoute(filePath, player, route, PlaceholderContext.create());
    }

    public static void sendRoute(String filePath, Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            send(player, net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            send(player, net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
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
                net.exylia.commons.v2.config.Messages.getList(route, context)
                        .forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
            } else {
                String message = net.exylia.commons.v2.config.Messages.get(route, context);
                if (!isEmpty(message)) sender.sendMessage(ColorAPI.parse(message));
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
                net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context)
                        .forEach(msg -> sender.sendMessage(ColorAPI.parse(msg)));
            } else {
                String message = net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context);
                if (!isEmpty(message)) sender.sendMessage(ColorAPI.parse(message));
            }
        }
    }

    // ─── broadcastRoute ───

    public static void broadcastRoute(String route) {
        broadcastRoute(route, PlaceholderContext.create());
    }

    public static void broadcastRoute(String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            broadcast(net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            broadcast(net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void broadcastRoute(String filePath, String route) {
        broadcastRoute(filePath, route, PlaceholderContext.create());
    }

    public static void broadcastRoute(String filePath, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            broadcast(net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            broadcast(net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
        }
    }

    // ─── broadcastRouteExcluding ───

    public static void broadcastRouteExcluding(String route, Player excludePlayer) {
        broadcastRouteExcluding(route, excludePlayer, PlaceholderContext.create());
    }

    public static void broadcastRouteExcluding(String route, Player excludePlayer, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.getList(route, context), excludePlayer, context);
        } else {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.get(route, context), excludePlayer, context);
        }
    }

    public static void broadcastRouteExcluding(String filePath, String route, Player excludePlayer) {
        broadcastRouteExcluding(filePath, route, excludePlayer, PlaceholderContext.create());
    }

    public static void broadcastRouteExcluding(String filePath, String route, Player excludePlayer, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), excludePlayer, context);
        } else {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), excludePlayer, context);
        }
    }

    public static void broadcastRouteExcluding(String route, Collection<Player> excludePlayers) {
        broadcastRouteExcluding(route, excludePlayers, PlaceholderContext.create());
    }

    public static void broadcastRouteExcluding(String route, Collection<Player> excludePlayers, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.getList(route, context), excludePlayers, context);
        } else {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.get(route, context), excludePlayers, context);
        }
    }

    public static void broadcastRouteExcluding(String filePath, String route, Collection<Player> excludePlayers) {
        broadcastRouteExcluding(filePath, route, excludePlayers, PlaceholderContext.create());
    }

    public static void broadcastRouteExcluding(String filePath, String route, Collection<Player> excludePlayers, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), excludePlayers, context);
        } else {
            broadcastExcluding(net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), excludePlayers, context);
        }
    }

    // ─── sendRouteToFiltered ───

    public static void sendRouteToFiltered(Predicate<Player> filter, String route) {
        sendRouteToFiltered(filter, route, PlaceholderContext.create());
    }

    public static void sendRouteToFiltered(Predicate<Player> filter, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            sendToFiltered(filter, net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            sendToFiltered(filter, net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void sendRouteToFiltered(String filePath, Predicate<Player> filter, String route) {
        sendRouteToFiltered(filePath, filter, route, PlaceholderContext.create());
    }

    public static void sendRouteToFiltered(String filePath, Predicate<Player> filter, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            sendToFiltered(filter, net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            sendToFiltered(filter, net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
        }
    }

    // ─── sendRouteToRecipients ───

    public static void sendRouteToRecipients(Collection<Player> recipients, String route) {
        sendRouteToRecipients(recipients, route, PlaceholderContext.create());
    }

    public static void sendRouteToRecipients(Collection<Player> recipients, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            sendToRecipients(recipients, net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            sendToRecipients(recipients, net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void sendRouteToRecipients(String filePath, Collection<Player> recipients, String route) {
        sendRouteToRecipients(filePath, recipients, route, PlaceholderContext.create());
    }

    public static void sendRouteToRecipients(String filePath, Collection<Player> recipients, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            sendToRecipients(recipients, net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            sendToRecipients(recipients, net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
        }
    }

    // ─── sendRouteInRadius ───

    public static void sendRouteInRadius(Location origin, double radius, String route) {
        sendRouteInRadius(origin, radius, route, PlaceholderContext.create());
    }

    public static void sendRouteInRadius(Location origin, double radius, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            sendInRadius(origin, radius, net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            sendInRadius(origin, radius, net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void sendRouteInRadius(String filePath, Location origin, double radius, String route) {
        sendRouteInRadius(filePath, origin, radius, route, PlaceholderContext.create());
    }

    public static void sendRouteInRadius(String filePath, Location origin, double radius, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            sendInRadius(origin, radius, net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            sendInRadius(origin, radius, net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
        }
    }

    // ─── sendRouteCentered ───

    public static void sendRouteCentered(Player player, String route) {
        sendRouteCentered(player, route, PlaceholderContext.create());
    }

    public static void sendRouteCentered(Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            sendCentered(player, net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            sendCentered(player, net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void sendRouteCentered(String filePath, Player player, String route) {
        sendRouteCentered(filePath, player, route, PlaceholderContext.create());
    }

    public static void sendRouteCentered(String filePath, Player player, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            sendCentered(player, net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            sendCentered(player, net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
        }
    }

    // ─── broadcastRouteCentered ───

    public static void broadcastRouteCentered(String route) {
        broadcastRouteCentered(route, PlaceholderContext.create());
    }

    public static void broadcastRouteCentered(String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAny(route);
        if (messageObj instanceof List) {
            broadcastCentered(net.exylia.commons.v2.config.Messages.getList(route, context), context);
        } else {
            broadcastCentered(net.exylia.commons.v2.config.Messages.get(route, context), context);
        }
    }

    public static void broadcastRouteCentered(String filePath, String route) {
        broadcastRouteCentered(filePath, route, PlaceholderContext.create());
    }

    public static void broadcastRouteCentered(String filePath, String route, PlaceholderContext context) {
        Object messageObj = net.exylia.commons.v2.config.Messages.getAnyFrom(filePath, route);
        if (messageObj instanceof List) {
            broadcastCentered(net.exylia.commons.v2.config.Messages.getListFrom(filePath, route, context), context);
        } else {
            broadcastCentered(net.exylia.commons.v2.config.Messages.getFrom(filePath, route, context), context);
        }
    }
}
