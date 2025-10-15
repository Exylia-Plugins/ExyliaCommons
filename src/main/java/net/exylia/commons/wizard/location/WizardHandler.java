package net.exylia.commons.wizard.location;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface WizardHandler<T> {

    void onStart(Player player, int positionsNeeded);

    WizardResult onLocationSelected(Player player, Location location, List<Location> selectedLocations, int remainingPositions);
}
