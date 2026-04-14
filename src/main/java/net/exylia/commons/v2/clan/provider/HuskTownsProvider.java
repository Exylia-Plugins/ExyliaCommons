package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.william278.husktowns.api.BukkitHuskTownsAPI;
import net.william278.husktowns.town.Member;
import net.william278.husktowns.town.Role;
import net.william278.husktowns.town.Town;
import net.william278.husktowns.user.OnlineUser;
import net.william278.husktowns.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HuskTownsProvider implements ClanProvider {

    private final BukkitHuskTownsAPI api;
    private final boolean enabled;

    public HuskTownsProvider() {
        BukkitHuskTownsAPI tempApi = null;
        boolean tempEnabled = false;

        try {
            tempApi = BukkitHuskTownsAPI.getInstance();
            tempEnabled = tempApi != null && tempApi.isLoaded();
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.api = tempApi;
        this.enabled = tempEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "HuskTowns";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                return getPlayerClan(player);
            }

            User user = User.of(playerId, "");
            Optional<Member> memberOpt = api.getUserTown(user);
            if (memberOpt.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/HT] Player " + playerId + " has no town in HuskTowns");
                }
                return Optional.empty();
            }

            Town town = memberOpt.get().town();
            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/HT] Player " + playerId + " -> town: " + town.getName());
            }

            return Optional.of(convertToClan(town));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/HT] Exception resolving town for player " + playerId, e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        if (!enabled) return CompletableFuture.completedFuture(Optional.empty());

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            return CompletableFuture.completedFuture(getPlayerClan(player));
        }

        try {
            return api.getUser(playerId)
                    .thenApply(userOpt -> {
                        if (userOpt.isEmpty()) {
                            return Optional.<Clan>empty();
                        }
                        Optional<Member> memberOpt = api.getUserTown(userOpt.get());
                        return memberOpt.map(member -> convertToClan(member.town()));
                    })
                    .exceptionally(throwable -> {
                        DebugAPI.logLibError("[ClanAPI/HT] Exception resolving town async for player " + playerId, throwable);
                        return Optional.empty();
                    });
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/HT] Exception in async town lookup for player " + playerId, e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Override
    public Optional<Clan> getPlayerClan(Player player) {
        if (!enabled) return Optional.empty();

        try {
            OnlineUser onlineUser = api.getOnlineUser(player);
            Optional<Member> memberOpt = api.getUserTown(onlineUser);
            if (memberOpt.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/HT] Player " + player.getName() + " has no town in HuskTowns");
                }
                return Optional.empty();
            }

            Town town = memberOpt.get().town();
            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/HT] Player " + player.getName() + " -> town: " + town.getName());
            }

            return Optional.of(convertToClan(town));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/HT] Exception resolving town for player " + player.getName(), e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return CompletableFuture.completedFuture(getPlayerClan(player));
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        return getClanById(tag);
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return getClanByIdAsync(tag);
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        if (!enabled) return Optional.empty();

        try {
            try {
                int townId = Integer.parseInt(id);
                Optional<Town> townOpt = api.getTown(townId);
                return townOpt.map(this::convertToClan);
            } catch (NumberFormatException e) {
                return api.getTown(id).map(this::convertToClan);
            }
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/HT] Exception resolving town by id '" + id + "'", e);
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
            return api.getTowns().stream()
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/HT] Exception retrieving all towns", e);
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
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                return hasPlayerClan(player);
            }

            User user = User.of(playerId, "");
            return api.getUserTown(user).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        if (!enabled) return false;

        try {
            OnlineUser onlineUser = api.getOnlineUser(player);
            return api.getUserTown(onlineUser).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void invalidateCache() {
    }

    private Clan convertToClan(Town town) {
        Map<UUID, Integer> membersMap = town.getMembers();
        UUID mayor = null;
        try {
            mayor = town.getMayor();
        } catch (Exception ignored) {
        }

        Set<UUID> leaderSet = new HashSet<>();
        Set<UUID> moderatorSet = new HashSet<>();
        Set<UUID> allMemberSet = new HashSet<>(membersMap.keySet());
        List<UUID> onlineMemberList = new ArrayList<>();

        if (mayor != null) {
            leaderSet.add(mayor);
        }

        int maxWeight = membersMap.values().stream().max(Integer::compare).orElse(0);
        int minWeight = membersMap.values().stream().min(Integer::compare).orElse(0);

        for (Map.Entry<UUID, Integer> entry : membersMap.entrySet()) {
            UUID uuid = entry.getKey();
            int weight = entry.getValue();

            if (!uuid.equals(mayor) && weight < maxWeight && weight > minWeight) {
                moderatorSet.add(uuid);
            }

            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlineMemberList.add(uuid);
            }
        }

        long createdTimestamp = 0L;
        try {
            createdTimestamp = town.getFoundedTime().toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }

        BigDecimal money = town.getMoney();
        double balance = money != null ? money.doubleValue() : 0.0;

        return Clan.builder()
                .id(String.valueOf(town.getId()))
                .name(town.getName())
                .tag(town.getName())
                .displayName(town.getName())
                .leaders(leaderSet)
                .moderators(moderatorSet)
                .allMembers(allMemberSet)
                .onlineMembers(onlineMemberList)
                .level(town.getLevel())
                .balance(balance)
                .createdAt(createdTimestamp)
                .verified(true)
                .description(town.getBio().orElse(""))
                .maxMembers(0)
                .killDeathRatio(0.0)
                .providerName("HuskTowns")
                .build();
    }
}
