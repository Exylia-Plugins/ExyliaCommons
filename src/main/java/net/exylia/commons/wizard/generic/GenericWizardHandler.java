package net.exylia.commons.wizard.generic;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public interface GenericWizardHandler<T> {

    void onStart(Player player);

    WizardActionResult onLeftClick(Player player, PlayerInteractEvent event);

    WizardActionResult onRightClick(Player player, PlayerInteractEvent event);
}
