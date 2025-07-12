package net.exylia.commons.wizard.generic;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handler interface for generic wizard actions
 */
public interface GenericWizardHandler<T> {

    /**
     * Called when wizard starts
     * Initialize your wizard state and display here
     *
     * @param player The player
     */
    void onStart(Player player);

    /**
     * Called when player performs SHIFT + LEFT CLICK
     *
     * @param player The player
     * @param event The interaction event
     * @return WizardActionResult indicating what to do next
     */
    WizardActionResult onLeftClick(Player player, PlayerInteractEvent event);

    /**
     * Called when player performs SHIFT + RIGHT CLICK
     *
     * @param player The player
     * @param event The interaction event
     * @return WizardActionResult indicating what to do next
     */
    WizardActionResult onRightClick(Player player, PlayerInteractEvent event);
}