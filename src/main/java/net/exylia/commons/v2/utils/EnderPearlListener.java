package net.exylia.commons.v2.utils;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class EnderPearlListener implements Listener {

    @EventHandler
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        PlayerUtils.trackPearl(player.getUniqueId(), pearl);
    }

    @EventHandler
    public void onPearlHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        PlayerUtils.untrackPearl(player.getUniqueId(), pearl);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerUtils.clearPlayerSession(event.getPlayer().getUniqueId());
    }



    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PlayerUtils.clearPlayerEnderPearls(event.getPlayer());
    }
}
