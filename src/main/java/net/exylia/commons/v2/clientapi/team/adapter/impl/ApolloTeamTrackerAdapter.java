package net.exylia.commons.v2.clientapi.team.adapter.impl;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.location.ApolloLocation;
import com.lunarclient.apollo.mods.impl.ModMarkers;
import com.lunarclient.apollo.mods.impl.ModTeamView;
import com.lunarclient.apollo.module.modsetting.ModSettingModule;
import com.lunarclient.apollo.module.team.TeamMember;
import com.lunarclient.apollo.module.team.TeamModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import net.exylia.commons.v2.clientapi.team.adapter.TeamTrackerAdapter;
import net.exylia.commons.v2.clientapi.team.model.TrackingTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApolloTeamTrackerAdapter implements TeamTrackerAdapter {

    private final TeamModule teamModule;
    private final ModSettingModule modSettingModule;

    public ApolloTeamTrackerAdapter() {
        this.teamModule = Apollo.getModuleManager().getModule(TeamModule.class);
        this.modSettingModule = Apollo.getModuleManager().getModule(ModSettingModule.class);
    }

    @Override
    public boolean isAvailable() {
        return teamModule != null;
    }

    @Override
    public boolean supportsPlayer(Player player) {
        return Apollo.getPlayerManager().getPlayer(player.getUniqueId()).isPresent();
    }

    @Override
    public void onMemberAdded(TrackingTeam team, Player player) {
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (apolloPlayer.isEmpty()) return;
        if (modSettingModule != null) {
            modSettingModule.getOptions().set(apolloPlayer.get(), ModTeamView.ENABLED, true);
            modSettingModule.getOptions().set(apolloPlayer.get(), ModTeamView.APOLLO_TEAMS, true);
            modSettingModule.getOptions().set(apolloPlayer.get(), ModMarkers.ENABLED, true);
        }
    }

    @Override
    public void onMemberRemoved(TrackingTeam team, Player player) {
        Apollo.getPlayerManager().getPlayer(player.getUniqueId())
                .ifPresent(teamModule::resetTeamMembers);
    }

    @Override
    public void refresh(TrackingTeam team) {
        List<TeamMember> snapshot = buildTeamMembers(team);
        for (Player member : team.getMembers()) {
            if (!member.isOnline()) continue;
            Apollo.getPlayerManager().getPlayer(member.getUniqueId())
                    .ifPresent(apolloPlayer -> teamModule.updateTeamMembers(apolloPlayer, snapshot));
        }
    }

    @Override
    public void dissolve(TrackingTeam team) {
        for (Player member : team.getMembers()) {
            Apollo.getPlayerManager().getPlayer(member.getUniqueId())
                    .ifPresent(teamModule::resetTeamMembers);
        }
    }

    private List<TeamMember> buildTeamMembers(TrackingTeam team) {
        List<TeamMember> result = new ArrayList<>();
        for (Player member : team.getMembers()) {
            if (!member.isOnline()) continue;
            result.add(createTeamMember(member));
        }
        return result;
    }

    private TeamMember createTeamMember(Player player) {
        Location location = player.getLocation();
        return TeamMember.builder()
                .playerUuid(player.getUniqueId())
                .displayName(Component.text()
                        .content(player.getName())
                        .color(NamedTextColor.WHITE)
                        .build())
                .markerColor(Color.WHITE)
                .location(ApolloLocation.builder()
                        .world(location.getWorld().getName())
                        .x(location.getX())
                        .y(location.getY())
                        .z(location.getZ())
                        .build())
                .build();
    }
}
