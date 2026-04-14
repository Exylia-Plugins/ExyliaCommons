package net.exylia.commons.v2.clientapi.team.adapter;

import net.exylia.commons.v2.clientapi.team.model.TrackingTeam;
import org.bukkit.entity.Player;

public interface TeamTrackerAdapter {

    boolean isAvailable();

    boolean supportsPlayer(Player player);

    void onMemberAdded(TrackingTeam team, Player player);

    void onMemberRemoved(TrackingTeam team, Player player);

    void refresh(TrackingTeam team);

    void dissolve(TrackingTeam team);
}
