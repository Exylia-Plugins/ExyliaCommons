package net.exylia.commons.v2.utils;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

class PlayerTabPacketHelper {

    static void sendTabAddPacket(Player player, Player viewer, @Nullable Component displayName) {
        List<TextureProperty> textures = new ArrayList<>();
        for (ProfileProperty prop : player.getPlayerProfile().getProperties()) {
            textures.add(new TextureProperty(prop.getName(), prop.getValue(), prop.getSignature()));
        }

        UserProfile profile = new UserProfile(player.getUniqueId(), player.getName(), textures);

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile,
                true,
                player.getPing(),
                GameMode.getById(player.getGameMode().ordinal()),
                displayName,
                null
        );

        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED
        );

        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(actions, info);

        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(viewer.getUniqueId());
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }

    static void sendTabRemovePacket(UUID playerUuid, Player viewer) {
        WrapperPlayServerPlayerInfoRemove packet = new WrapperPlayServerPlayerInfoRemove(playerUuid);
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(viewer.getUniqueId());
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }
}
