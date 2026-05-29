package net.exylia.commons.v2.clientapi.waypoint.adapter.impl;

import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.waypoint.WaypointBuilder;
import net.digitalingot.feather.serverapi.api.waypoint.WaypointDuration;
import net.digitalingot.feather.serverapi.api.waypoint.WaypointService;
import net.exylia.commons.v2.clientapi.waypoint.adapter.WaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.model.WaypointColor;
import net.exylia.commons.v2.clientapi.waypoint.model.WaypointDefinition;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.UUID;

public class FeatherWaypointAdapter implements WaypointAdapter {

    private final WaypointService waypointService;

    public FeatherWaypointAdapter() {
        this.waypointService = FeatherAPI.getWaypointService();
    }

    @Override
    public boolean isAvailable() {
        return waypointService != null;
    }

    @Override
    public boolean needsResendOnWorldChange() {
        return true;
    }

    @Override
    public boolean supportsPlayer(Player player) {
        return FeatherAPI.getPlayerService().getPlayer(player.getUniqueId()) != null;
    }

    @Override
    public String show(Player player, WaypointDefinition definition) {
        FeatherPlayer featherPlayer = FeatherAPI.getPlayerService().getPlayer(player.getUniqueId());
        if (featherPlayer == null) {
            DebugAPI.logLibDebug("[Feather] show() SKIP player=" + player.getName() + " name=" + definition.getName() + " (not a Feather player)");
            return null;
        }

        featherPlayer.enableMods(Collections.singletonList(new FeatherMod("waypoints")));

        WaypointBuilder builder = waypointService.createWaypointBuilder(
                definition.getX(), definition.getY(), definition.getZ()
        );

        builder.withName(definition.getName())
               .withColor(toFeatherColor(definition.getColor()))
               .withDuration(toFeatherDuration(definition.getDuration()));

        UUID resolvedWorldId = definition.getWorldId();
        if (resolvedWorldId == null && definition.getWorldName() != null) {
            World world = Bukkit.getWorld(definition.getWorldName());
            if (world != null) resolvedWorldId = world.getUID();
        }
        if (resolvedWorldId != null) {
            builder.withWorldId(resolvedWorldId);
        }

        UUID waypointId = waypointService.createWaypoint(featherPlayer, builder);
        DebugAPI.logLibDebug("[Feather] show() player=" + player.getName() + " name=" + definition.getName()
                + " worldId=" + resolvedWorldId + " worldName=" + definition.getWorldName()
                + " pos=" + definition.getX() + "," + definition.getY() + "," + definition.getZ()
                + " featherWaypointId=" + waypointId);
        return waypointId != null ? waypointId.toString() : null;
    }

    @Override
    public void remove(Player player, String handle) {
        DebugAPI.logLibDebug("[Feather] remove() player=" + player.getName() + " handle=" + handle);
        FeatherPlayer featherPlayer = FeatherAPI.getPlayerService().getPlayer(player.getUniqueId());
        if (featherPlayer == null) return;
        try {
            waypointService.destroyWaypoint(featherPlayer, UUID.fromString(handle));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void removeAll(Player player) {
        DebugAPI.logLibDebug("[Feather] removeAll() player=" + player.getName());
        FeatherPlayer featherPlayer = FeatherAPI.getPlayerService().getPlayer(player.getUniqueId());
        if (featherPlayer == null) return;
        waypointService.destroyAllWaypoints(featherPlayer);
    }

    private net.digitalingot.feather.serverapi.api.waypoint.WaypointColor toFeatherColor(WaypointColor color) {
        if (color.isChroma()) return net.digitalingot.feather.serverapi.api.waypoint.WaypointColor.chroma();
        return net.digitalingot.feather.serverapi.api.waypoint.WaypointColor.fromRgba(
                color.getR() / 255.0f, color.getG() / 255.0f, color.getB() / 255.0f, color.getAlpha() / 255.0f
        );
    }

    private WaypointDuration toFeatherDuration(net.exylia.commons.v2.clientapi.waypoint.model.WaypointDuration duration) {
        if (duration.isPermanent()) return WaypointDuration.none();
        return WaypointDuration.of(duration.getSeconds());
    }
}
