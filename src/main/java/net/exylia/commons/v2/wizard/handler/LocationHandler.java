package net.exylia.commons.v2.wizard.handler;

import net.exylia.commons.v2.wizard.result.WizardResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

@FunctionalInterface
public interface LocationHandler<T> {
    WizardResult<T> onLocation(Player player, Location location, List<Location> allLocations, int remaining);

    default void onStart(Player player, int total) {}
}
