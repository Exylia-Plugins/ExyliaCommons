package net.exylia.commons.v2.clientapi.waypoint.adapter;

import net.exylia.commons.v2.clientapi.waypoint.model.WaypointDefinition;
import org.bukkit.entity.Player;

public interface WaypointAdapter {

    boolean isAvailable();

    boolean supportsPlayer(Player player);

    String show(Player player, WaypointDefinition definition);

    void remove(Player player, String handle);

    void removeAll(Player player);

    default boolean needsResendOnWorldChange() {
        return false;
    }
}
