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
        return manager.createTeam();
    }

    public static void deleteTeam(UUID teamId) {
        manager.deleteTeam(teamId);
    }

    public static void addMember(UUID teamId, Player player) {
        manager.addMember(teamId, player);
    }

    public static void removeMember(Player player) {
        manager.removeMember(player);
    }
}
