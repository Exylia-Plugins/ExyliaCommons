package net.exylia.commons.v2.clientapi.team.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientTeamManager extends PacketListenerAbstract {

    private final Map<UUID, Set<String>> viewerCreatedTeams = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Set<String>>> viewerTeamMembers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewerGlowTargets = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIdToUuid = new ConcurrentHashMap<>();

    public ClientTeamManager() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;
        if (!(event.getPlayer() instanceof Player observer)) return;

        Set<UUID> glowTargets = viewerGlowTargets.get(observer.getUniqueId());
        if (glowTargets == null || glowTargets.isEmpty()) return;

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        UUID targetUuid = entityIdToUuid.get(packet.getEntityId());
        if (targetUuid == null || !glowTargets.contains(targetUuid)) return;

        List<EntityData<?>> metadata = packet.getEntityMetadata();
        for (int i = 0; i < metadata.size(); i++) {
            EntityData<?> data = metadata.get(i);
            if (data.getIndex() != 0) continue;
            byte flags = (Byte) data.getValue();
            if ((flags & 0x20) == 0) return;
            metadata.set(i, new EntityData<>(0, EntityDataTypes.BYTE, (byte) (flags | 0x40)));
            event.markForReEncode(true);
            return;
        }
    }

    public void createTeam(Player viewer, String name, NamedTextColor color, boolean seeFriendlyInvisibles, List<String> members) {
        User user = getUser(viewer);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerTeams(
                name, WrapperPlayServerTeams.TeamMode.CREATE,
                Optional.of(buildTeamInfo(color, seeFriendlyInvisibles)), members));
        viewerCreatedTeams.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(name);
        Set<String> memberSet = viewerTeamMembers
                .computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
        memberSet.clear();
        memberSet.addAll(members);
    }

    public void ensureTeamAndAdd(Player viewer, String name, NamedTextColor color, boolean seeFriendlyInvisibles, String entityName, boolean includeViewer) {
        User user = getUser(viewer);
        if (user == null) return;
        Set<String> created = viewerCreatedTeams.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        Map<String, Set<String>> teamMembersMap = viewerTeamMembers.computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>());
        if (created.add(name)) {
            List<String> members = includeViewer
                    ? List.of(viewer.getName(), entityName)
                    : Collections.singletonList(entityName);
            user.sendPacket(new WrapperPlayServerTeams(
                    name, WrapperPlayServerTeams.TeamMode.CREATE,
                    Optional.of(buildTeamInfo(color, seeFriendlyInvisibles)), members));
            Set<String> memberSet = teamMembersMap.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
            memberSet.addAll(members);
        } else {
            Set<String> memberSet = teamMembersMap.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
            if (memberSet.add(entityName)) {
                user.sendPacket(new WrapperPlayServerTeams(
                        name, WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
                        Optional.empty(), Collections.singletonList(entityName)));
            }
        }
    }

    public void removeFromTeam(Player viewer, String name, String entityName) {
        Map<String, Set<String>> teamMembersMap = viewerTeamMembers.get(viewer.getUniqueId());
        if (teamMembersMap == null) return;
        Set<String> memberSet = teamMembersMap.get(name);
        if (memberSet == null || !memberSet.remove(entityName)) return;
        User user = getUser(viewer);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerTeams(
                name, WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
                Optional.empty(), Collections.singletonList(entityName)));
    }

    public void clearViewer(Player viewer) {
        Set<String> teams = viewerCreatedTeams.remove(viewer.getUniqueId());
        viewerTeamMembers.remove(viewer.getUniqueId());
        viewerGlowTargets.remove(viewer.getUniqueId());
        if (teams == null) return;
        User user = getUser(viewer);
        if (user == null) return;
        for (String name : teams) {
            user.sendPacket(new WrapperPlayServerTeams(
                    name, WrapperPlayServerTeams.TeamMode.REMOVE,
                    Optional.empty(), Collections.emptyList()));
        }
    }

    public void registerEntity(int entityId, UUID uuid) {
        entityIdToUuid.put(entityId, uuid);
    }

    public void unregisterEntity(int entityId) {
        entityIdToUuid.remove(entityId);
    }

    public void addGlowTarget(UUID viewerUuid, UUID targetUuid) {
        viewerGlowTargets.computeIfAbsent(viewerUuid, k -> ConcurrentHashMap.newKeySet()).add(targetUuid);
    }

    public void removeGlowTarget(UUID viewerUuid, UUID targetUuid) {
        Set<UUID> targets = viewerGlowTargets.get(viewerUuid);
        if (targets != null) targets.remove(targetUuid);
    }

    public Set<String> getViewerTeams(UUID viewerUuid) {
        return Collections.unmodifiableSet(viewerCreatedTeams.getOrDefault(viewerUuid, Collections.emptySet()));
    }

    public void shutdown() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        viewerCreatedTeams.clear();
        viewerTeamMembers.clear();
        viewerGlowTargets.clear();
        entityIdToUuid.clear();
    }

    private WrapperPlayServerTeams.ScoreBoardTeamInfo buildTeamInfo(NamedTextColor color, boolean seeFriendlyInvisibles) {
        byte flags = seeFriendlyInvisibles ? (byte) 0x02 : (byte) 0x00;
        return new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(), Component.empty(), Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                color,
                WrapperPlayServerTeams.OptionData.fromValue(flags));
    }

    private User getUser(Player player) {
        return PacketEvents.getAPI().getPlayerManager().getUser(player);
    }
}
