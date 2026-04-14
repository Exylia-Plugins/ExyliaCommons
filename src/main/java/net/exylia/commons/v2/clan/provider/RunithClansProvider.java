package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.runith.clan.api.ClanAPI;
import net.runith.clan.api.model.ClanMember;
import net.runith.clan.api.model.ClanMembers;
import net.runith.clan.api.model.MemberRole;
import net.runith.clan.api.storage.ClansStorage;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RunithClansProvider implements ClanProvider {

    private final boolean enabled;

    public RunithClansProvider() {
        boolean tempEnabled = false;

        try {
            ClanAPI api = ClanAPI.getInstance();
            tempEnabled = api != null && api.clansStorage() != null;
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.enabled = tempEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "RunithClans";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            ClansStorage storage = ClanAPI.getInstance().clansStorage();
            ClanMember member = storage.getMember(playerId);
            if (member == null) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/RC] Player " + playerId + " has no clan in RunithClans");
                }
                return Optional.empty();
            }

            net.runith.clan.api.model.Clan clan = member.getClan();
            if (clan == null) {
                return Optional.empty();
            }

            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/RC] Player " + playerId + " -> clan: " + clan.getName());
            }

            return Optional.of(convertToClan(clan));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/RC] Exception resolving clan for player " + playerId, e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return Tasks.run(() -> getPlayerClan(playerId)).thenApply(r -> r.getValue().orElse(Optional.empty()));
    }

    @Override
    public Optional<Clan> getPlayerClan(Player player) {
        return getPlayerClan(player.getUniqueId());
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return getPlayerClanAsync(player.getUniqueId());
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        if (!enabled) return Optional.empty();

        try {
            ClansStorage storage = ClanAPI.getInstance().clansStorage();
            net.runith.clan.api.model.Clan clan = storage.getClan(tag);
            if (clan == null) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(clan));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/RC] Exception resolving clan by tag '" + tag + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return Tasks.run(() -> getClanByTag(tag)).thenApply(r -> r.getValue().orElse(Optional.empty()));
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        if (!enabled) return Optional.empty();

        try {
            UUID clanUUID = UUID.fromString(id);
            ClansStorage storage = ClanAPI.getInstance().clansStorage();
            net.runith.clan.api.model.Clan clan = storage.getClan(clanUUID);
            if (clan == null) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(clan));
        } catch (IllegalArgumentException e) {
            return getClanByTag(id);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/RC] Exception resolving clan by id '" + id + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByIdAsync(String id) {
        return Tasks.run(() -> getClanById(id)).thenApply(r -> r.getValue().orElse(Optional.empty()));
    }

    @Override
    public Collection<Clan> getAllClans() {
        if (!enabled) return Collections.emptyList();

        try {
            return ClanAPI.getInstance().clansStorage().getOnlineClans().stream()
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/RC] Exception retrieving all clans", e);
            return Collections.emptyList();
        }
    }

    @Override
    public CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return Tasks.run(this::getAllClans).thenApply(r -> r.getValue().orElse(Collections.emptyList()));
    }

    @Override
    public boolean hasPlayerClan(UUID playerId) {
        if (!enabled) return false;

        try {
            ClansStorage storage = ClanAPI.getInstance().clansStorage();
            ClanMember member = storage.getMember(playerId);
            return member != null && member.getClan() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        return hasPlayerClan(player.getUniqueId());
    }

    @Override
    public void invalidateCache() {
    }

    private Clan convertToClan(net.runith.clan.api.model.Clan clan) {
        Set<UUID> leaderSet = new HashSet<>();
        Set<UUID> moderatorSet = new HashSet<>();
        Set<UUID> allMemberSet = new HashSet<>();
        List<UUID> onlineMemberList = new ArrayList<>();

        ClanMembers clanMembers = clan.getClanMembers();
        if (clanMembers != null && clanMembers.members() != null) {
            for (ClanMember member : clanMembers.members()) {
                UUID uuid = member.getUuid();
                allMemberSet.add(uuid);

                MemberRole role = member.getRole();
                if (role == MemberRole.LEADER || role == MemberRole.ADMINISTRATOR) {
                    leaderSet.add(uuid);
                } else if (role == MemberRole.CO_LEADER || role == MemberRole.MOD) {
                    moderatorSet.add(uuid);
                }

                if (member.isOnline()) {
                    onlineMemberList.add(uuid);
                }
            }
        }

        int totalKills = clan.getKills();
        int totalDeaths = clan.getDeaths();
        double kdRatio = totalDeaths > 0 ? (double) totalKills / totalDeaths : totalKills;

        return Clan.builder()
                .id(clan.getUuid().toString())
                .name(clan.getName())
                .tag(clan.getTag())
                .displayName(clan.getTag())
                .leaders(leaderSet)
                .moderators(moderatorSet)
                .allMembers(allMemberSet)
                .onlineMembers(onlineMemberList)
                .level(0)
                .balance((double) clan.getBalance())
                .createdAt(0L)
                .verified(true)
                .description("")
                .maxMembers(0)
                .killDeathRatio(kdRatio)
                .providerName("RunithClans")
                .build();
    }
}
