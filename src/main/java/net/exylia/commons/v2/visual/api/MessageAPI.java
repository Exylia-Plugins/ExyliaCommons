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
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, context);
    }

    public static CompletableFuture<Void> send(Player player, List<String> messages) {
        return send(player, messages, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> send(Player player, List<String> messages, PlaceholderContext context) {
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
        if (sender instanceof Player player) {
            return send(player, message);
        } else {
            sender.sendMessage(ColorAPI.parse(message));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Void> broadcast(String message) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcast(List<String> messages) {
        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .broadcast()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastExcluding(String message, Player excludePlayer) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayer)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastExcluding(String message, Collection<Player> excludePlayers) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .excluding(excludePlayers)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToFiltered(Predicate<Player> filter, String message) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .filter(filter)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToFiltered(Predicate<Player> filter, List<String> messages) {
        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .filter(filter)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToRecipients(Collection<Player> recipients, String message) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(recipients)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendToRecipients(Collection<Player> recipients, List<String> messages) {
        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(recipients)
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendInRadius(Location origin, double radius, String message) {
        if (origin == null) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return sendToFiltered(filter, message);
    }

    public static CompletableFuture<Void> sendInRadius(Location origin, double radius, List<String> messages) {
        if (origin == null) {
            return CompletableFuture.completedFuture(null);
        }

        double radiusSquared = radius * radius;
        Predicate<Player> filter = player ->
                player.getWorld().equals(origin.getWorld()) &&
                        player.getLocation().distanceSquared(origin) <= radiusSquared;

        return sendToFiltered(filter, messages);
    }

    public static CompletableFuture<Void> sendCentered(Player player, String message) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .to(player)
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> sendCentered(Player player, List<String> messages) {
        MessageConfig config = MessageBuilder.create()
                .messages(messages)
                .to(player)
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastCentered(String message) {
        MessageConfig config = MessageBuilder.create()
                .message(message)
                .broadcast()
                .centered()
                .build();

        return MessageRenderer.getInstance().renderAsync(null, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> broadcastCentered(List<String> messages) {
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
}
