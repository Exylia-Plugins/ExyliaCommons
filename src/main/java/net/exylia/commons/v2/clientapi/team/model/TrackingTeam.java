package net.exylia.commons.v2.clientapi.team.model;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrackingTeam {

    @Getter
    private final UUID teamId;
    private final Map<UUID, Player> members = new ConcurrentHashMap<>();

    public TrackingTeam() {
        this.teamId = UUID.randomUUID();
    }

    public void addMember(Player player) {
        members.put(player.getUniqueId(), player);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public Collection<Player> getMembers() {
        return members.values();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public int size() {
        return members.size();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || other.getClass() != getClass()) return false;
        return teamId.equals(((TrackingTeam) other).teamId);
    }

    @Override
    public int hashCode() {
        return teamId.hashCode();
    }
}
