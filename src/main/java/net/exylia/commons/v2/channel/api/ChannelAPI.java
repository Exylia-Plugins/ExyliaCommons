package net.exylia.commons.v2.channel.api;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.exylia.commons.v2.channel.adapter.ChannelMessenger;
import net.exylia.commons.v2.channel.adapter.RedisChannelMessenger;
import net.exylia.commons.v2.channel.cache.PermissionCache;
import net.exylia.commons.v2.channel.core.ChannelManager;
import net.exylia.commons.v2.channel.core.WriteModeTracker;
import net.exylia.commons.v2.channel.model.Channel;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ChannelAPI {

    private ChannelAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(Plugin plugin) {
        ChannelManager.initialize(plugin);
    }

    public static void create(String id, String permission, String format) {
        create(id, permission, format, 0.0);
    }

    public static void create(
        String id,
        String permission,
        String format,
        double cooldownSeconds
    ) {
        validateId(id);
        validatePermission(permission);
        validateFormat(format);

        Channel channel = new Channel(id, permission, format, cooldownSeconds);
        ChannelManager.getInstance().getRegistry().register(channel);

        if (cooldownSeconds > 0) {
            ChannelManager.getInstance()
                .getCooldownManager()
                .registerChannel(id, cooldownSeconds);
        }

        ChannelMessenger messenger =
            ChannelManager.getInstance().getMessenger();
        if (messenger instanceof RedisChannelMessenger) {
            ((RedisChannelMessenger) messenger).subscribeToChannel(id);
        }
    }

    public static void delete(String channelId) {
        ChannelManager.getInstance().getRegistry().unregister(channelId);
        ChannelManager.getInstance()
            .getWriteModeTracker()
            .clearAllForChannel(channelId);
        ChannelManager.getInstance()
            .getCooldownManager()
            .unregisterChannel(channelId);
    }

    public static void updateFormat(String channelId, String newFormat) {
        validateFormat(newFormat);

        Channel oldChannel = get(channelId).orElseThrow(() ->
            new IllegalArgumentException("Channel not found: " + channelId)
        );

        Channel newChannel = new Channel(
            oldChannel.getId(),
            oldChannel.getPermission(),
            newFormat,
            oldChannel.getCooldownSeconds()
        );
        newChannel.setCooldownMessage(oldChannel.getCooldownMessage());

        ChannelManager.getInstance().getRegistry().register(newChannel);
    }

    public static void setCooldownMessage(
        String channelId,
        String cooldownMessage
    ) {
        Channel channel = get(channelId).orElseThrow(() ->
            new IllegalArgumentException("Channel not found: " + channelId)
        );

        channel.setCooldownMessage(cooldownMessage);
    }

    public static Optional<Channel> get(String channelId) {
        return ChannelManager.getInstance().getRegistry().get(channelId);
    }

    public static boolean exists(String channelId) {
        return get(channelId).isPresent();
    }

    public static void setWriteMode(Player player, String channelId) {
        if (!exists(channelId)) {
            throw new IllegalArgumentException(
                "Channel does not exist: " + channelId
            );
        }

        WriteModeTracker tracker =
            ChannelManager.getInstance().getWriteModeTracker();
        tracker.setWriteMode(player.getUniqueId(), channelId);
    }

    public static void clearWriteMode(Player player) {
        WriteModeTracker tracker =
            ChannelManager.getInstance().getWriteModeTracker();
        tracker.clearWriteMode(player.getUniqueId());
    }

    public static Optional<String> getWriteMode(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        WriteModeTracker tracker =
            ChannelManager.getInstance().getWriteModeTracker();
        return tracker.getWriteMode(player.getUniqueId());
    }

    public static boolean isInWriteMode(Player player) {
        if (player == null) {
            return false;
        }

        WriteModeTracker tracker =
            ChannelManager.getInstance().getWriteModeTracker();
        return tracker.isInWriteMode(player.getUniqueId());
    }

    public static void sendMessage(
        String channelId,
        Player sender,
        String message
    ) {
        if (sender == null || message == null) {
            throw new IllegalArgumentException(
                "Sender and message cannot be null"
            );
        }

        ChannelManager.getInstance()
            .getMessenger()
            .sendMessage(channelId, sender, message);
    }

    public static void broadcast(String channelId, String message) {
        Channel channel = get(channelId).orElseThrow(() ->
            new IllegalArgumentException("Channel not found: " + channelId)
        );

        PermissionCache cache =
            ChannelManager.getInstance().getPermissionCache();

        PlaceholderContext context = PlaceholderContext.create().put(
            "message",
            message
        );

        Collection<Player> recipients = Bukkit.getOnlinePlayers()
            .stream()
            .filter(p -> cache.hasPermission(p, channel.getPermission()))
            .collect(Collectors.toList());

        for (Player recipient : recipients) {
            MessageAPI.send(recipient, channel.getFormat(), context);
        }
    }

    public static int getActiveChannelCount() {
        return ChannelManager.getInstance().getRegistry().getActiveCount();
    }

    public static Set<String> getAllChannelIds() {
        return ChannelManager.getInstance().getRegistry().getAllChannelIds();
    }

    public static void clearAll() {
        ChannelManager.getInstance().getRegistry().clear();
        ChannelManager.getInstance().getWriteModeTracker().clearAll();
    }

    private static void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Channel ID cannot be null or empty"
            );
        }

        if (!id.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException(
                "Channel ID can only contain lowercase letters, numbers, and hyphens"
            );
        }
    }

    private static void validatePermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Permission cannot be null or empty"
            );
        }
    }

    private static void validateFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Format cannot be null or empty"
            );
        }
    }
}
