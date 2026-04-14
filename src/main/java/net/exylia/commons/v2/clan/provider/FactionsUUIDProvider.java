package net.exylia.commons.v2.clan.provider;

import dev.kitteh.factions.FPlayer;
import dev.kitteh.factions.FPlayers;
import dev.kitteh.factions.Faction;
import dev.kitteh.factions.Factions;
import dev.kitteh.factions.permissible.Role;
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FactionsUUIDProvider implements ClanProvider {

    private final boolean enabled;

    public FactionsUUIDProvider() {
        boolean tempEnabled = false;

        try {
            Factions.factions();
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
        return "FactionsUUID";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            FPlayer fPlayer = FPlayers.fPlayers().get(playerId);
            if (fPlayer == null || !fPlayer.hasFaction()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/FU] Player " + playerId + " has no faction");
                }
                return Optional.empty();
            }

            Faction faction = fPlayer.faction();
            if (faction == null || faction.isWilderness()) {
                return Optional.empty();
            }

            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/FU] Player " + playerId + " -> faction: " + faction.tag());
            }

            return Optional.of(convertToClan(faction));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/FU] Exception resolving faction for player " + playerId, e);
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
            Faction faction = Factions.factions().get(tag);
            if (faction == null || faction.isWilderness()) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(faction));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/FU] Exception resolving faction by tag '" + tag + "'", e);
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
            int factionId = Integer.parseInt(id);
            Faction faction = Factions.factions().get(factionId);
            if (faction == null || faction.isWilderness()) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(faction));
        } catch (NumberFormatException e) {
            return getClanByTag(id);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/FU] Exception resolving faction by id '" + id + "'", e);
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
            return Factions.factions().all().stream()
                    .filter(f -> f.isNormal())
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/FU] Exception retrieving all factions", e);
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
            FPlayer fPlayer = FPlayers.fPlayers().get(playerId);
            return fPlayer != null && fPlayer.hasFaction();
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

    private Clan convertToClan(Faction faction) {
        Set<UUID> leaderSet = new HashSet<>();
        Set<UUID> moderatorSet = new HashSet<>();
        Set<UUID> allMemberSet = new HashSet<>();
        List<UUID> onlineMemberList = new ArrayList<>();

        FPlayer admin = faction.admin();
        if (admin != null) {
            leaderSet.add(admin.uniqueId());
        }

        Set<FPlayer> members = faction.members();
        if (members != null) {
            for (FPlayer member : members) {
                UUID uuid = member.uniqueId();
                allMemberSet.add(uuid);

                if (!leaderSet.contains(uuid)) {
                    Role role = member.role();
                    if (role == Role.COLEADER || role == Role.MODERATOR) {
                        moderatorSet.add(uuid);
                    }
                }

                if (member.isOnline()) {
                    onlineMemberList.add(uuid);
                }
            }
        }

        long foundedTimestamp = 0L;
        try {
            Instant founded = faction.founded();
            if (founded != null) {
                foundedTimestamp = founded.toEpochMilli();
            }
        } catch (Exception ignored) {
        }

        String tag = faction.tag();
        String description = faction.description();

        return Clan.builder()
                .id(String.valueOf(faction.id()))
                .name(tag)
                .tag(tag)
                .displayName(tag)
                .leaders(leaderSet)
                .moderators(moderatorSet)
                .allMembers(allMemberSet)
                .onlineMembers(onlineMemberList)
                .level(0)
                .balance(0.0)
                .createdAt(foundedTimestamp)
                .verified(faction.isNormal())
                .description(description != null ? description : "")
                .maxMembers(0)
                .killDeathRatio(0.0)
                .providerName("FactionsUUID")
                .build();
    }
}
