package net.exylia.commons.v2.clan.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.ulrich.clans.Clans;
import me.ulrich.clans.api.ClanAPIManager;
import me.ulrich.clans.data.ClanData;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.v2.clan.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UltimateClanProvider implements ClanProvider {

    private final ClanAPIManager api;
    private final boolean enabled;
    private final Cache<UUID, String> playerClanIdCache;

    public UltimateClanProvider() {
        ClanAPIManager tempApi = null;
        boolean tempEnabled = false;

        try {
            Clans plugin = (Clans) Bukkit.getPluginManager().getPlugin("UltimateClans");
            if (plugin != null) {
                tempApi = plugin.getClanAPI();
                tempEnabled = tempApi != null;
            }
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.api = tempApi;
        this.enabled = tempEnabled;
        this.playerClanIdCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(5000)
                .build();
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
            String clanTag = playerClanIdCache.get(playerId, k -> {
                for (ClanData clanData : api.getAllClansData()) {
                    if (clanData.getMembers().contains(playerId) ||
                            clanData.getMods().contains(playerId) ||
                            clanData.getLeader().equals(playerId)) {
                        return clanData.getTag();
                    }
                }
                return null;
            });

            if (clanTag == null) {
                return Optional.empty();
            }

            return getClanByTag(clanTag);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return AsyncExecutor.getInstance().supplyAsync(() -> getPlayerClan(playerId), false);
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
            return optData.map(this::convertToClan);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return AsyncExecutor.getInstance().supplyAsync(() -> getClanByTag(tag), false);
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
        return AsyncExecutor.getInstance().supplyAsync(this::getAllClans, false);
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
        playerClanIdCache.invalidateAll();
    }

    private Clan convertToClan(ClanData clanData) {
        Set<UUID> allMembers = new HashSet<>(clanData.getMembers());
        allMembers.addAll(clanData.getMods());
        allMembers.add(clanData.getLeader());

        return Clan.builder()
                .id(clanData.getTag())
                .name(clanData.getTag())
                .tag(clanData.getTag())
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
