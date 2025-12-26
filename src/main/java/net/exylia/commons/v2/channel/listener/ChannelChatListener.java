package net.exylia.commons.v2.channel.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.channel.core.ChannelManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ChannelChatListener implements Listener {

    private final ChannelManager manager;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderId = sender.getUniqueId();

        Optional<String> channelId = manager.getWriteModeTracker().getWriteMode(senderId);

        if (channelId.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        manager.getMessenger().sendMessage(channelId.get(), sender, event.getMessage());
    }
}
