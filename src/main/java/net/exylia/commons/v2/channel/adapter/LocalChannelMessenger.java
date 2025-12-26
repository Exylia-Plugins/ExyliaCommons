package net.exylia.commons.v2.channel.adapter;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.channel.cache.PermissionCache;
import net.exylia.commons.v2.channel.core.ChannelRegistry;
import net.exylia.commons.v2.channel.model.Channel;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocalChannelMessenger implements ChannelMessenger {

    private final ChannelRegistry registry;
    private final PermissionCache permissionCache;
    private final net.exylia.commons.v2.channel.core.CooldownManager cooldownManager;

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

        PlaceholderContext context = PlaceholderContext.create()
                .withPlayer(sender)
                .put("player", sender.getName())
                .put("message", message);

        CompletableFuture.runAsync(() -> {
            Collection<Player> recipients = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> permissionCache.hasPermission(p, channel.getPermission()))
                    .collect(Collectors.toList());

            for (Player recipient : recipients) {
                MessageAPI.send(recipient, channel.getFormat(), context);
            }
        });
    }

    @Override
    public void broadcast(String channelId, String formattedMessage) {
        Channel channel = registry.get(channelId).orElse(null);
        if (channel == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            Collection<Player> recipients = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> permissionCache.hasPermission(p, channel.getPermission()))
                    .collect(Collectors.toList());

            for (Player recipient : recipients) {
                recipient.sendMessage(formattedMessage);
            }
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }
}
