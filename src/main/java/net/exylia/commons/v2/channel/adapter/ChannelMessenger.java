package net.exylia.commons.v2.channel.adapter;

import org.bukkit.entity.Player;

public interface ChannelMessenger {

    void sendMessage(String channelId, Player sender, String message);

    void broadcast(String channelId, String formattedMessage);

    void initialize();

    void shutdown();
}
