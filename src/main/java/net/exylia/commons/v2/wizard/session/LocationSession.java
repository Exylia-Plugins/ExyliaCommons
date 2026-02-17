package net.exylia.commons.v2.wizard.session;

import lombok.Getter;
import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.handler.LocationHandler;
import net.exylia.commons.v2.wizard.model.WizardType;
import net.exylia.commons.v2.wizard.result.WizardResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class LocationSession<T> extends WizardSession<T> {
    private final LocationHandler<T> handler;
    private final int totalCount;
    private final List<Location> selectedLocations;

    public LocationSession(Player player, WizardConfig config, int count, LocationHandler<T> handler) {
        super(player, config);
        this.handler = handler;
        this.totalCount = count;
        this.selectedLocations = new ArrayList<>();
    }

    @Override
    public WizardType getType() {
        return WizardType.LOCATION;
    }

    @Override
    public void handleInteraction(PlayerInteractEvent event) {
        if (!player.isSneaking()) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR) return;

        event.setCancelled(true);

        Location location = player.getLocation().clone();
        selectedLocations.add(location);

        int remaining = totalCount - selectedLocations.size();
        WizardResult<T> result = handler.onLocation(player, location, getLocations(), remaining);

        processResult(result);
    }

    private void processResult(WizardResult<T> result) {
        switch (result.getType()) {
            case COMPLETE -> complete(result.getValue());
            case CANCEL -> cancel();
            case CONTINUE -> {}
        }
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(selectedLocations);
    }

    public int getRemaining() {
        return totalCount - selectedLocations.size();
    }

    public int getCurrent() {
        return selectedLocations.size() + 1;
    }
}
