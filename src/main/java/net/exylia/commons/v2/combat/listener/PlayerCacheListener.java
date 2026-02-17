package net.exylia.commons.v2.combat.listener;

import lombok.AllArgsConstructor;
import net.exylia.commons.v2.combat.core.CombatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public class PlayerCacheListener implements Listener {

    private final CombatManager manager;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.getCombatDataCache().invalidate(event.getPlayer().getUniqueId());
    }
}
