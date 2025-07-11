package net.exylia.commons.wizard.location;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Custom wizard handler interface
 * Implement this to create your own location selection logic
 */
public interface WizardHandler<T> {

    /**
     * Called when wizard starts
     * Send initial instructions to the player here
     *
     * @param player The player
     * @param positionsNeeded Total positions needed
     */
    void onStart(Player player, int positionsNeeded);

    /**
     * Process location selection
     *
     * @param player The player
     * @param location The selected location
     * @param selectedLocations All locations selected so far (including current)
     * @param remainingPositions How many positions are still needed
     * @return WizardResult indicating what to do next
     */
    WizardResult onLocationSelected(Player player, Location location, List<Location> selectedLocations, int remainingPositions);
}