package net.exylia.commons.v2.clan.listener;

import lombok.AllArgsConstructor;
import net.exylia.commons.v2.clan.core.ClanManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public class PlayerCacheListener implements Listener {

    private final ClanManager manager;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.getCacheManager().invalidatePlayer(event.getPlayer().getUniqueId());
    }
}
