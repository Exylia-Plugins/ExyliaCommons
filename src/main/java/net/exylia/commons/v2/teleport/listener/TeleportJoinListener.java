package net.exylia.commons.v2.teleport.listener;

import net.exylia.commons.v2.teleport.core.TeleportManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TeleportJoinListener implements Listener {

    private final TeleportManager manager;

    public TeleportJoinListener(TeleportManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        manager.handlePendingTeleport(event.getPlayer());
    }
}
