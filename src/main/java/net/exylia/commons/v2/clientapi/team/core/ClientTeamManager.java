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

    private final Map<UUID, Set<String>> viewerOwnedTeams = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> viewerKnownTeams = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> viewerEntityTeam = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewerGlowTargets = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIdToUuid = new ConcurrentHashMap<>();

    public ClientTeamManager() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player observer)) return;

        if (event.getPacketType() == PacketType.Play.Server.TEAMS) {
            trackTeamPacket(observer.getUniqueId(), event);
            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

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

    public void createTeam(Player viewer, String name, NamedTextColor color, boolean seeFriendlyInvisibles, Collection<String> members) {
        User user = getUser(viewer);
        if (user == null) return;
        UUID viewerId = viewer.getUniqueId();
        if (isKnown(viewerId, name)) sendRemoveTeam(user, viewerId, name);
        user.sendPacket(new WrapperPlayServerTeams(
                name, WrapperPlayServerTeams.TeamMode.CREATE,
                Optional.of(buildTeamInfo(color, seeFriendlyInvisibles)), members));
        viewerOwnedTeams.computeIfAbsent(viewerId, k -> ConcurrentHashMap.newKeySet()).add(name);
        markKnown(viewerId, name);
        assignEntities(viewerId, name, members);
    }

    public void ensureTeamAndAdd(Player viewer, String name, NamedTextColor color, boolean seeFriendlyInvisibles, String entityName, boolean includeViewer) {
        if (!isKnown(viewer.getUniqueId(), name)) {
            createTeam(viewer, name, color, seeFriendlyInvisibles, includeViewer
                    ? List.of(viewer.getName(), entityName)
                    : Collections.singletonList(entityName));
            return;
        }
        addToTeam(viewer, name, entityName);
        if (includeViewer) addToTeam(viewer, name, viewer.getName());
    }

    public void addToTeam(Player viewer, String name, String entityName) {
        UUID viewerId = viewer.getUniqueId();
        if (!isKnown(viewerId, name)) return;
        if (name.equals(getEntityTeam(viewerId, entityName))) return;
        User user = getUser(viewer);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerTeams(
                name, WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
                Optional.empty(), Collections.singletonList(entityName)));
        assignEntities(viewerId, name, Collections.singletonList(entityName));
    }

    public void removeFromTeam(Player viewer, String name, String entityName) {
        UUID viewerId = viewer.getUniqueId();
        if (!name.equals(getEntityTeam(viewerId, entityName))) return;
        User user = getUser(viewer);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerTeams(
                name, WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
                Optional.empty(), Collections.singletonList(entityName)));
        unassignEntity(viewerId, name, entityName);
    }

    public void removeOwnedEntity(Player viewer, String entityName) {
        UUID viewerId = viewer.getUniqueId();
        String team = getEntityTeam(viewerId, entityName);
        if (team == null) return;
        Set<String> owned = viewerOwnedTeams.get(viewerId);
        if (owned == null || !owned.contains(team)) return;
        removeFromTeam(viewer, team, entityName);
    }

    public void removeTeam(Player viewer, String name) {
        UUID viewerId = viewer.getUniqueId();
        Set<String> owned = viewerOwnedTeams.get(viewerId);
        if (owned != null) owned.remove(name);
        if (!isKnown(viewerId, name)) return;
        User user = getUser(viewer);
        if (user == null) return;
        sendRemoveTeam(user, viewerId, name);
    }

    public void clearViewer(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        Set<String> owned = viewerOwnedTeams.remove(viewerId);
        viewerGlowTargets.remove(viewerId);
        if (owned == null || owned.isEmpty()) return;
        User user = getUser(viewer);
        if (user == null) return;
        for (String name : owned) {
            if (isKnown(viewerId, name)) sendRemoveTeam(user, viewerId, name);
        }
    }

    public void cleanupViewer(UUID viewerUuid) {
        viewerOwnedTeams.remove(viewerUuid);
        viewerKnownTeams.remove(viewerUuid);
        viewerEntityTeam.remove(viewerUuid);
        viewerGlowTargets.remove(viewerUuid);
    }

    public void forgetEntity(String entityName) {
        for (Map<String, String> assignments : viewerEntityTeam.values()) {
            assignments.remove(entityName);
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
        return Collections.unmodifiableSet(viewerOwnedTeams.getOrDefault(viewerUuid, Collections.emptySet()));
    }

    public boolean hasTeam(UUID viewerUuid, String teamName) {
        return isKnown(viewerUuid, teamName);
    }

    public String getEntityTeam(UUID viewerUuid, String entityName) {
        Map<String, String> assignments = viewerEntityTeam.get(viewerUuid);
        return assignments == null ? null : assignments.get(entityName);
    }

    public void shutdown() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        viewerOwnedTeams.clear();
        viewerKnownTeams.clear();
        viewerEntityTeam.clear();
        viewerGlowTargets.clear();
        entityIdToUuid.clear();
    }

    private void trackTeamPacket(UUID viewerId, PacketSendEvent event) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(event);
        String teamName = packet.getTeamName();
        switch (packet.getTeamMode()) {
            case CREATE -> {
                markKnown(viewerId, teamName);
                assignEntities(viewerId, teamName, packet.getPlayers());
            }
            case ADD_ENTITIES -> assignEntities(viewerId, teamName, packet.getPlayers());
            case REMOVE_ENTITIES -> {
                for (String entity : packet.getPlayers()) unassignEntity(viewerId, teamName, entity);
            }
            case REMOVE -> markUnknown(viewerId, teamName);
            default -> {
            }
        }
    }

    private void sendRemoveTeam(User user, UUID viewerId, String name) {
        user.sendPacket(new WrapperPlayServerTeams(
                name, WrapperPlayServerTeams.TeamMode.REMOVE,
                Optional.empty(), Collections.emptyList()));
        markUnknown(viewerId, name);
    }

    private boolean isKnown(UUID viewerId, String teamName) {
        Set<String> teams = viewerKnownTeams.get(viewerId);
        return teams != null && teams.contains(teamName);
    }

    private void markKnown(UUID viewerId, String teamName) {
        viewerKnownTeams.computeIfAbsent(viewerId, k -> ConcurrentHashMap.newKeySet()).add(teamName);
    }

    private void markUnknown(UUID viewerId, String teamName) {
        Set<String> teams = viewerKnownTeams.get(viewerId);
        if (teams != null) teams.remove(teamName);
        Map<String, String> assignments = viewerEntityTeam.get(viewerId);
        if (assignments != null) assignments.values().removeIf(teamName::equals);
    }

    private void assignEntities(UUID viewerId, String teamName, Collection<String> entities) {
        if (entities.isEmpty()) return;
        Map<String, String> assignments = viewerEntityTeam.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
        for (String entity : entities) assignments.put(entity, teamName);
    }

    private void unassignEntity(UUID viewerId, String teamName, String entityName) {
        Map<String, String> assignments = viewerEntityTeam.get(viewerId);
        if (assignments != null) assignments.remove(entityName, teamName);
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
