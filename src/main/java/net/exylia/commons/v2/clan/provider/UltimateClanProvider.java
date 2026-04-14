package net.exylia.commons.v2.clan.provider;

import me.ulrich.clans.Clans;
import me.ulrich.clans.api.ClanAPIManager;
import me.ulrich.clans.data.ClanData;
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UltimateClanProvider implements ClanProvider {

    private final Clans plugin;
    private final ClanAPIManager api;
    private final boolean enabled;

    public UltimateClanProvider() {
        Clans tempPlugin = null;
        ClanAPIManager tempApi = null;
        boolean tempEnabled = false;

        try {
            tempPlugin = (Clans) Bukkit.getPluginManager().getPlugin("UltimateClans");
            if (tempPlugin != null) {
                tempApi = tempPlugin.getClanAPI();
                tempEnabled = tempApi != null;
            }
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.plugin = tempPlugin;
        this.api = tempApi;
        this.enabled = tempEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "UltimateClans";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            Optional<ClanData> playerClanOpt = plugin.getPlayerAPI().getPlayerClan(playerId);

            if (playerClanOpt.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/UC] Player " + playerId + " has no clan in UltimateClans");
                }
                return Optional.empty();
            }

            ClanData clanData = playerClanOpt.get();

            if (DebugAPI.isLibDebugEnabled()) {
                DebugAPI.logLibDebug("[ClanAPI/UC] Player " + playerId + " -> clan: " + clanData.getTag());
            }

            return Optional.of(convertToClan(clanData));
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/UC] Exception resolving clan for player " + playerId, e);
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
            Optional<ClanData> optData = api.getClanDataByTag(tag);

            if (optData.isEmpty()) {
                if (DebugAPI.isLibDebugEnabled()) {
                    DebugAPI.logLibDebug("[ClanAPI/UC] No ClanData found for tag: " + tag);
                }
                return Optional.empty();
            }

            return optData.map(this::convertToClan);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/UC] Exception resolving clan by tag '" + tag + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return Tasks.run(() -> getClanByTag(tag)).thenApply(r -> r.getValue().orElse(Optional.empty()));
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        return getClanByTag(id);
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByIdAsync(String id) {
        return getClanByTagAsync(id);
    }

    @Override
    public Collection<Clan> getAllClans() {
        if (!enabled) return Collections.emptyList();

        try {
            return api.getAllClansData().stream()
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return Tasks.run(this::getAllClans).thenApply(r -> r.getValue().orElse(Collections.emptyList()));
    }

    @Override
    public boolean hasPlayerClan(UUID playerId) {
        return getPlayerClan(playerId).isPresent();
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        return hasPlayerClan(player.getUniqueId());
    }

    @Override
    public void invalidateCache() {
    }

    private Clan convertToClan(ClanData clanData) {
        Set<UUID> allMembers = new HashSet<>(clanData.getMembers());
        allMembers.addAll(clanData.getMods());
        allMembers.add(clanData.getLeader());

        return Clan.builder()
                .id(clanData.getTagNoColor())
                .name(clanData.getTagNoColor())
                .tag(clanData.getTagNoColor())
                .displayName(clanData.getTag())
                .leader(clanData.getLeader())
                .moderators(new HashSet<>(clanData.getMods()))
                .members(new HashSet<>(clanData.getMembers()))
                .onlineMembers(new ArrayList<>(clanData.getOnlineMembers()))
                .allMembers(allMembers)
                .level(clanData.getLevel())
                .balance(clanData.getBank() != null ? clanData.getBank() : 0.0)
                .createdAt(clanData.getCreationDate())
                .verified(clanData.isVerified())
                .description(clanData.getDesc() != null ? clanData.getDesc() : "")
                .maxMembers(0)
                .killDeathRatio(clanData.getKdr())
                .providerName("UltimateClans")
                .build();
    }
}
