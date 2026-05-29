package net.exylia.commons.v2.clientapi.waypoint.adapter.impl;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import com.lunarclient.apollo.mods.impl.ModMarkers;
import com.lunarclient.apollo.mods.impl.ModWaypoints;
import com.lunarclient.apollo.module.modsetting.ModSettingModule;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import net.exylia.commons.v2.clientapi.waypoint.adapter.WaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.model.WaypointDefinition;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ApolloWaypointAdapter implements WaypointAdapter {

    private final WaypointModule waypointModule;
    private final ModSettingModule modSettingModule;

    public ApolloWaypointAdapter() {
        this.waypointModule = Apollo.getModuleManager().getModule(WaypointModule.class);
        this.modSettingModule = Apollo.getModuleManager().getModule(ModSettingModule.class);
    }

    @Override
    public boolean isAvailable() {
        return waypointModule != null;
    }

    @Override
    public boolean supportsPlayer(Player player) {
        return Apollo.getPlayerManager().getPlayer(player.getUniqueId()).isPresent();
    }

    @Override
    public String show(Player player, WaypointDefinition definition) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (apolloPlayerOpt.isEmpty()) {
            DebugAPI.logLibDebug("[Apollo] show() SKIP player=" + player.getName() + " name=" + definition.getName() + " (not a Lunar player)");
            return null;
        }

        ApolloPlayer apolloPlayer = apolloPlayerOpt.get();

        if (modSettingModule != null) {
            modSettingModule.getOptions().set(apolloPlayer, ModWaypoints.ENABLED, true);
        }

        waypointModule.displayWaypoint(apolloPlayer, Waypoint.builder()
                .name(definition.getName())
                .location(ApolloBlockLocation.builder()
                        .world(definition.getWorldName() != null ? definition.getWorldName() : "world")
                        .x(definition.getX())
                        .y(definition.getY())
                        .z(definition.getZ())
                        .build())
                .color(definition.getColor().toAwtColor())
                .preventRemoval(definition.isPreventRemoval())
                .hidden(definition.isHidden())
                .build());

        DebugAPI.logLibDebug("[Apollo] show() player=" + player.getName() + " name=" + definition.getName()
                + " world=" + definition.getWorldName() + " pos=" + definition.getX() + "," + definition.getY() + "," + definition.getZ());
        return definition.getName();
    }

    @Override
    public void remove(Player player, String handle) {
        DebugAPI.logLibDebug("[Apollo] remove() player=" + player.getName() + " handle=" + handle);
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(apolloPlayer -> waypointModule.removeWaypoint(apolloPlayer, handle));
    }

    @Override
    public void removeAll(Player player) {
        DebugAPI.logLibDebug("[Apollo] removeAll() player=" + player.getName());
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(waypointModule::resetWaypoints);
    }

    @Override
    public List<String> autoRemovedHandles() {
        return Collections.singletonList("Spawn");
    }
}
