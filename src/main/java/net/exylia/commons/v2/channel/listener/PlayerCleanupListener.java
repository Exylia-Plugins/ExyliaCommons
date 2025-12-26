package net.exylia.commons.v2.channel.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.channel.core.ChannelManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@RequiredArgsConstructor
public class PlayerCleanupListener implements Listener {

    private final ChannelManager manager;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        manager.getWriteModeTracker().cleanup(playerId);
        manager.getPermissionCache().invalidate(playerId);
    }
}
