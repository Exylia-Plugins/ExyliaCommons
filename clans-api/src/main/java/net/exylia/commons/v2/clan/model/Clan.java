package net.exylia.commons.v2.clan.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class Clan {

    private final String id;
    private final String name;
    private final String tag;
    private final String displayName;

    @Singular
    private final Set<UUID> leaders;

    @Singular
    private final Set<UUID> moderators;

    @Singular
    private final Set<UUID> members;

    @Singular
    private final List<UUID> onlineMembers;

    @Singular
    private final Set<UUID> allMembers;

    private final int level;
    private final double balance;
    private final long createdAt;
    private final boolean verified;
    private final String description;
    private final int maxMembers;
    private final double killDeathRatio;
    private final String providerName;

    public boolean isMember(UUID playerId) {
        return allMembers.contains(playerId);
    }

    public boolean isLeader(UUID playerId) {
        return leaders.contains(playerId);
    }

    public boolean isModerator(UUID playerId) {
        return moderators.contains(playerId);
    }

    public boolean isOnline(UUID playerId) {
        return onlineMembers.contains(playerId);
    }

    public int getMemberCount() {
        return allMembers.size();
    }

    public int getOnlineCount() {
        return onlineMembers.size();
    }

    public boolean isVerified() {
        return verified;
    }
}
