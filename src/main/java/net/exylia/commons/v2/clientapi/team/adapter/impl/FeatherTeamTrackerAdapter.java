package net.exylia.commons.v2.clientapi.team.adapter.impl;

import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.exylia.commons.v2.clientapi.team.adapter.TeamTrackerAdapter;
import net.exylia.commons.v2.clientapi.team.model.TrackingTeam;
import org.bukkit.entity.Player;

import java.util.List;

public class FeatherTeamTrackerAdapter implements TeamTrackerAdapter {

    private static final List<FeatherMod> TEAM_TRACKER_MOD = List.of(new FeatherMod("teamtracker"));

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean supportsPlayer(Player player) {
        return FeatherAPI.getPlayerService().getPlayer(player.getUniqueId()) != null;
    }

    @Override
    public void onMemberAdded(TrackingTeam team, Player player) {
        FeatherPlayer featherPlayer = FeatherAPI.getPlayerService().getPlayer(player.getUniqueId());
        if (featherPlayer == null) return;
        featherPlayer.enableMods(TEAM_TRACKER_MOD);
    }

    @Override
    public void onMemberRemoved(TrackingTeam team, Player player) {
        FeatherPlayer featherPlayer = FeatherAPI.getPlayerService().getPlayer(player.getUniqueId());
        if (featherPlayer == null) return;
        featherPlayer.disableMods(TEAM_TRACKER_MOD);
    }

    @Override
    public void refresh(TrackingTeam team) {
    }

    @Override
    public void dissolve(TrackingTeam team) {
        for (Player member : team.getMembers()) {
            if (!member.isOnline()) continue;
            FeatherPlayer featherPlayer = FeatherAPI.getPlayerService().getPlayer(member.getUniqueId());
            if (featherPlayer == null) continue;
            featherPlayer.disableMods(TEAM_TRACKER_MOD);
        }
    }
}
