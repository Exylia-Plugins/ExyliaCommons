package net.exylia.commons.v2.channel.adapter;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.channel.cache.PermissionCache;
import net.exylia.commons.v2.channel.core.ChannelRegistry;
import net.exylia.commons.v2.channel.model.Channel;
import net.exylia.commons.v2.channel.model.ChannelMessage;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.redis.SimpleRedis;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RedisChannelMessenger implements ChannelMessenger {

    private static final String CHANNEL_PREFIX = "exyliacommons:channel:";

    private final Plugin plugin;
    private final ChannelRegistry registry;
    private final PermissionCache permissionCache;
    private final net.exylia.commons.v2.channel.core.CooldownManager cooldownManager;
    private final SimpleRedis redis;

    @Override
    public void initialize() {
        for (String channelId : registry.getAllChannelIds()) {
            subscribeToChannel(channelId);
        }
    }

    public void subscribeToChannel(String channelId) {
        String redisChannel = CHANNEL_PREFIX + channelId;

        redis.pubSub().subscribeObject(
                redisChannel,
                ChannelMessage.class,
                this::handleMessage
        );
    }

    @Override
    public void sendMessage(String channelId, Player sender, String message) {
        Channel channel = registry.get(channelId).orElse(null);
        if (channel == null) {
            return;
        }

        if (!permissionCache.hasPermission(sender, channel.getPermission())) {
            return;
        }

        if (channel.hasCooldown()) {
            boolean hasBypass = permissionCache.hasPermission(sender, channel.getBypassPermission());

            if (!hasBypass && cooldownManager.isOnCooldown(channelId, sender.getUniqueId())) {
                if (channel.getCooldownMessage() != null && !channel.getCooldownMessage().isEmpty()) {
                    sender.sendMessage(channel.getCooldownMessage());
                }
                return;
            }

            if (!hasBypass) {
                cooldownManager.setCooldown(channelId, sender.getUniqueId());
            }
        }

        ChannelMessage msg = new ChannelMessage(
                channelId,
                sender.getName(),
                sender.getUniqueId(),
                message,
                getServerName(),
                System.currentTimeMillis()
        );

        String redisChannel = CHANNEL_PREFIX + channelId;
        redis.publishObjectAsync(redisChannel, msg);
    }

    @Override
    public void broadcast(String channelId, String formattedMessage) {
        Channel channel = registry.get(channelId).orElse(null);
        if (channel == null) {
            return;
        }

        Collection<Player> recipients = Bukkit.getOnlinePlayers().stream()
                .filter(p -> permissionCache.hasPermission(p, channel.getPermission()))
                .collect(Collectors.toList());

        for (Player recipient : recipients) {
            recipient.sendMessage(formattedMessage);
        }
    }

    private void handleMessage(ChannelMessage msg) {
        Channel channel = registry.get(msg.getChannelId()).orElse(null);
        if (channel == null) {
            return;
        }

        Collection<Player> recipients = Bukkit.getOnlinePlayers().stream()
                .filter(p -> permissionCache.hasPermission(p, channel.getPermission()))
                .collect(Collectors.toList());

        PlaceholderContext context = PlaceholderContext.create()
                .put("player", msg.getSenderName())
                .put("message", msg.getMessage())
                .put("server", msg.getServerName());

        for (Player recipient : recipients) {
            MessageAPI.send(recipient, channel.getFormat(), context);
        }
    }

    private String getServerName() {
        return plugin.getServer().getName();
    }

    @Override
    public void shutdown() {
        redis.pubSub().unsubscribeAll();
    }
}
