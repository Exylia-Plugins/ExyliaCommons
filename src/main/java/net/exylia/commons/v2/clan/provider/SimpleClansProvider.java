package net.exylia.commons.v2.clan.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.v2.clan.model.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SimpleClansProvider implements ClanProvider {

    private final SimpleClans plugin;
    private final boolean enabled;
    private final Cache<UUID, String> playerClanCache;

    public SimpleClansProvider() {
        SimpleClans tempPlugin = null;
        boolean tempEnabled = false;

        try {
            tempPlugin = (SimpleClans) Bukkit.getPluginManager().getPlugin("SimpleClans");
            tempEnabled = tempPlugin != null;
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.plugin = tempPlugin;
        this.enabled = tempEnabled;
        this.playerClanCache = Caffeine.newBuilder()
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
        return "SimpleClans";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            ClanPlayer cp = plugin.getClanManager().getClanPlayer(playerId);
            if (cp == null) {
                return Optional.empty();
            }

            net.sacredlabyrinth.phaed.simpleclans.Clan scClan = cp.getClan();
            if (scClan == null) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(scClan));
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
            net.sacredlabyrinth.phaed.simpleclans.Clan scClan = plugin.getClanManager().getClan(tag);
            if (scClan == null) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(scClan));
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
            return plugin.getClanManager().getClans().stream()
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
        playerClanCache.invalidateAll();
    }

    private Clan convertToClan(net.sacredlabyrinth.phaed.simpleclans.Clan scClan) {
        Set<UUID> leaders = scClan.getLeaders().stream()
                .map(ClanPlayer::getUniqueId)
                .collect(Collectors.toSet());

        Set<UUID> allMembers = scClan.getMembers().stream()
                .map(ClanPlayer::getUniqueId)
                .collect(Collectors.toSet());

        List<UUID> onlineMembers = scClan.getOnlineMembers().stream()
                .map(ClanPlayer::getUniqueId)
                .collect(Collectors.toList());

        return Clan.builder()
                .id(scClan.getTag())
                .name(scClan.getName())
                .tag(scClan.getTag())
                .displayName(scClan.getName())
                .leaders(leaders)
                .allMembers(allMembers)
                .onlineMembers(onlineMembers)
                .level(0)
                .balance(0.0)
                .createdAt(0L)
                .verified(scClan.isVerified())
                .description("")
                .maxMembers(0)
                .killDeathRatio(0.0)
                .providerName("SimpleClans")
                .build();
    }
}
