package net.exylia.commons.v2.clientapi.team.api;

import net.exylia.commons.v2.clientapi.team.core.TeamTrackerManager;
import net.exylia.commons.v2.clientapi.team.model.TrackingTeam;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TeamTrackerAPI {

    private static TeamTrackerManager manager;

    private TeamTrackerAPI() {}

    public static void initialize(TeamTrackerManager teamTrackerManager) {
        manager = teamTrackerManager;
    }

    public static TrackingTeam createTeam() {
        if (manager == null) return null;
        return manager.createTeam();
    }

    public static void deleteTeam(UUID teamId) {
        if (manager == null) return;
        manager.deleteTeam(teamId);
    }

    public static void addMember(UUID teamId, Player player) {
        if (manager == null) return;
        manager.addMember(teamId, player);
    }

    public static void removeMember(Player player) {
        if (manager == null) return;
        manager.removeMember(player);
    }
}
