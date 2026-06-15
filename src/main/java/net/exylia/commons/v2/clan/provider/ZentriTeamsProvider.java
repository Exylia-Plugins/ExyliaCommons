package net.exylia.commons.v2.clan.provider;

import dev.zentri.teams.api.ZeCl;
import dev.zentri.teams.api.ZentriTeamsAPI;
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ZentriTeamsProvider extends AbstractClanProvider {

    private final boolean enabled;

    public ZentriTeamsProvider() {
        boolean tempEnabled = false;
        try {
            ZentriTeamsAPI.getAllClans();
            tempEnabled = true;
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
        return "ZentriTeams";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            Optional<ZeCl> clan = ZentriTeamsAPI.getPlayerClan(playerId);
            if (clan.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/ZT2] Player " + playerId + " has no team in ZentriTeams");
                }
                return Optional.empty();
            }

            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/ZT2] Player " + playerId + " -> team: " + clan.get().getTag());
            }

            return Optional.of(convertToClan(clan.get()));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT2] Exception resolving team for player " + playerId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        if (!enabled) return Optional.empty();

        try {
            ZeCl clan = ZentriTeamsAPI.getClanByTag(tag);
            if (clan == null) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/ZT2] No team found for tag: " + tag);
                }
                return Optional.empty();
            }

            return Optional.of(convertToClan(clan));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT2] Exception resolving team by tag '" + tag + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        if (!enabled) return Optional.empty();

        try {
            ZeCl clan = ZentriTeamsAPI.getClanById(id);
            if (clan == null) return Optional.empty();
            return Optional.of(convertToClan(clan));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT2] Exception resolving team by id '" + id + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public Collection<Clan> getAllClans() {
        if (!enabled) return Collections.emptyList();

        try {
            return ZentriTeamsAPI.getAllClans().stream()
                    .filter(Objects::nonNull)
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT2] Exception retrieving all teams", e);
            return Collections.emptyList();
        }
    }

    private Clan convertToClan(ZeCl clan) {
        UUID leader = clan.getLeader();
        Set<UUID> coLeaders = clan.getCoLeaders() != null ? clan.getCoLeaders() : Collections.emptySet();
        Set<UUID> rawMembers = clan.getMembers() != null ? clan.getMembers() : Collections.emptySet();

        Set<UUID> allMembers = clan.getAllMembers() != null ? new HashSet<>(clan.getAllMembers()) : new HashSet<>();
        if (leader != null) allMembers.add(leader);
        allMembers.addAll(coLeaders);
        allMembers.addAll(rawMembers);

        Set<UUID> leaderSet = new HashSet<>();
        if (leader != null) leaderSet.add(leader);

        List<UUID> onlineMembers = new ArrayList<>();
        for (UUID uuid : allMembers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) onlineMembers.add(uuid);
        }

        return Clan.builder()
                .id(clan.getId())
                .name(clan.getTag())
                .tag(clan.getTag())
                .displayName(clan.getName())
                .leaders(leaderSet)
                .moderators(coLeaders)
                .members(rawMembers)
                .allMembers(allMembers)
                .onlineMembers(onlineMembers)
                .level(clan.getLevel())
                .balance(0.0)
                .createdAt(clan.getCreatedAt())
                .verified(true)
                .description(clan.getDescription() != null ? clan.getDescription() : "")
                .maxMembers(clan.getMaxMembers())
                .killDeathRatio(clan.getKDR())
                .providerName("ZentriTeams")
                .build();
    }
}
