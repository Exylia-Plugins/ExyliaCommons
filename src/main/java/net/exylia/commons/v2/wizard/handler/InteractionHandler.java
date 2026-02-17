package net.exylia.commons.v2.wizard.handler;

import net.exylia.commons.v2.wizard.model.InteractionType;
import net.exylia.commons.v2.wizard.result.WizardResult;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

@FunctionalInterface
public interface InteractionHandler<T> {
    WizardResult<T> onInteraction(Player player, InteractionType type, PlayerInteractEvent event);

    default void onStart(Player player) {}
}
