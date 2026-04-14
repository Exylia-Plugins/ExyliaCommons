package net.exylia.commons.v2.clan.provider;

import com.zeltuv.teams.api.ITeamPlugin;
import com.zeltuv.teams.api.ZelTeamsAPI;
import com.zeltuv.teams.api.cache.IMember;
import com.zeltuv.teams.api.cache.IOwner;
import com.zeltuv.teams.api.cache.ITeam;
import com.zeltuv.teams.api.manager.ITeamManager;
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ZelTeamsProvider implements ClanProvider {

    private final ITeamPlugin api;
    private final ITeamManager teamManager;
    private final boolean enabled;

    public ZelTeamsProvider() {
        ITeamPlugin tempApi = null;
        ITeamManager tempTeamManager = null;
        boolean tempEnabled = false;

        try {
            tempApi = ZelTeamsAPI.getInstance();
            if (tempApi != null) {
                tempTeamManager = tempApi.getTeamManager();
                tempEnabled = tempTeamManager != null;
            }
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.api = tempApi;
        this.teamManager = tempTeamManager;
        this.enabled = tempEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "ZelTeams";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            Optional<ITeam> team = teamManager.getOfflinePlayerTeam(playerId);
            if (team.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/ZT] Player " + playerId + " has no team in ZelTeams");
                }
                return Optional.empty();
            }

            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/ZT] Player " + playerId + " -> team: " + team.get().getName());
            }

            return Optional.of(convertToClan(team.get()));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT] Exception resolving team for player " + playerId, e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return Tasks.run(() -> getPlayerClan(playerId)).thenApply(r -> r.getValue().orElse(Optional.empty()));
    }

    @Override
    public Optional<Clan> getPlayerClan(Player player) {
        if (!enabled) return Optional.empty();

        try {
            Optional<ITeam> team = teamManager.getTeam(player);
            if (team.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/ZT] Player " + player.getName() + " has no team in ZelTeams");
                }
                return Optional.empty();
            }

            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/ZT] Player " + player.getName() + " -> team: " + team.get().getName());
            }

            return Optional.of(convertToClan(team.get()));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT] Exception resolving team for player " + player.getName(), e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return Tasks.run(() -> getPlayerClan(player)).thenApply(r -> r.getValue().orElse(Optional.empty()));
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        if (!enabled) return Optional.empty();

        try {
            Optional<ITeam> team = teamManager.getByTag(tag);
            if (team.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/ZT] No team found for tag: " + tag);
                }
                return Optional.empty();
            }

            return Optional.of(convertToClan(team.get()));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT] Exception resolving team by tag '" + tag + "'", e);
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
            UUID teamUUID = UUID.fromString(id);
            ITeam team = teamManager.getCachedTeams().get(teamUUID);
            if (team == null) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(team));
        } catch (IllegalArgumentException e) {
            return getClanByName(id);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT] Exception resolving team by id '" + id + "'", e);
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
            return teamManager.getCachedTeams().values().stream()
                    .filter(Objects::nonNull)
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ZT] Exception retrieving all teams", e);
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
            return teamManager.getOfflinePlayerTeam(playerId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        if (!enabled) return false;

        try {
            return teamManager.hasTeam(player);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void invalidateCache() {
    }

    private Optional<Clan> getClanByName(String name) {
        try {
            return teamManager.getTeamByName(name).map(this::convertToClan);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Clan convertToClan(ITeam team) {
        IOwner owner = team.getOwner();
        UUID ownerUUID = owner != null ? owner.getUuid() : null;

        Set<UUID> allMemberUUIDs = new HashSet<>();
        Set<UUID> leaderSet = new HashSet<>();
        Set<UUID> moderatorSet = new HashSet<>();
        List<UUID> onlineMemberList = new ArrayList<>();

        if (ownerUUID != null) {
            leaderSet.add(ownerUUID);
            allMemberUUIDs.add(ownerUUID);

            Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
            if (ownerPlayer != null && ownerPlayer.isOnline()) {
                onlineMemberList.add(ownerUUID);
            }
        }

        List<IMember> members = team.getAllMembers();
        if (members != null) {
            for (IMember member : members) {
                UUID memberUUID = member.getUuid();
                if (memberUUID == null) continue;

                allMemberUUIDs.add(memberUUID);

                if (!memberUUID.equals(ownerUUID)) {
                    int priority = member.getRole().getPriority();
                    if (priority > 0) {
                        moderatorSet.add(memberUUID);
                    }
                }

                Player memberPlayer = Bukkit.getPlayer(memberUUID);
                if (memberPlayer != null && memberPlayer.isOnline()) {
                    onlineMemberList.add(memberUUID);
                }
            }
        }

        int totalKills = team.getTotalKills();
        int totalDeaths = team.getTotalDeaths();
        double kdRatio = totalDeaths > 0 ? (double) totalKills / totalDeaths : totalKills;

        return Clan.builder()
                .id(team.getTeamUUID().toString())
                .name(team.getName())
                .tag(team.getTag())
                .displayName(team.getDisplayName())
                .leaders(leaderSet)
                .moderators(moderatorSet)
                .allMembers(allMemberUUIDs)
                .onlineMembers(onlineMemberList)
                .level(team.getRank() >= 0 ? team.getRank() : 0)
                .balance(team.getBankBalance())
                .createdAt(0L)
                .verified(!team.isClosed())
                .description("")
                .maxMembers(0)
                .killDeathRatio(kdRatio)
                .providerName("ZelTeams")
                .build();
    }
}
