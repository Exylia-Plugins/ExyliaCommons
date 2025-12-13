package net.exylia.commons.v2.clan.api;

import net.exylia.commons.v2.clan.core.ClanManager;
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.clan.provider.ClanProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ClanAPI {

    private ClanAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        ClanManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return ClanManager.isInitialized();
    }

    public static Optional<Clan> getPlayerClan(UUID playerId) {
        return ClanManager.getInstance().getPlayerClan(playerId);
    }

    public static CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return ClanManager.getInstance().getPlayerClanAsync(playerId);
    }

    public static Optional<Clan> getPlayerClan(Player player) {
        return ClanManager.getInstance().getPlayerClan(player);
    }

    public static CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return ClanManager.getInstance().getPlayerClanAsync(player);
    }

    public static Optional<String> getPlayerClanName(UUID playerId) {
        return getPlayerClan(playerId).map(Clan::getName);
    }

    public static Optional<String> getPlayerClanName(Player player) {
        return getPlayerClan(player).map(Clan::getName);
    }

    public static CompletableFuture<Optional<String>> getPlayerClanNameAsync(UUID playerId) {
        return getPlayerClanAsync(playerId).thenApply(opt -> opt.map(Clan::getName));
    }

    public static Optional<String> getPlayerClanTag(UUID playerId) {
        return getPlayerClan(playerId).map(Clan::getTag);
    }

    public static Optional<String> getPlayerClanTag(Player player) {
        return getPlayerClan(player).map(Clan::getTag);
    }

    public static CompletableFuture<Optional<String>> getPlayerClanTagAsync(UUID playerId) {
        return getPlayerClanAsync(playerId).thenApply(opt -> opt.map(Clan::getTag));
    }

    public static Optional<Clan> getClanByTag(String tag) {
        return ClanManager.getInstance().getClanByTag(tag);
    }

    public static CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return ClanManager.getInstance().getClanByTagAsync(tag);
    }

    public static Optional<Clan> getClanById(String id) {
        return ClanManager.getInstance().getClanById(id);
    }

    public static CompletableFuture<Optional<Clan>> getClanByIdAsync(String id) {
        return ClanManager.getInstance().getClanByIdAsync(id);
    }

    public static Collection<Clan> getAllClans() {
        return ClanManager.getInstance().getAllClans();
    }

    public static CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return ClanManager.getInstance().getAllClansAsync();
    }

    public static boolean hasPlayerClan(UUID playerId) {
        return ClanManager.getInstance().hasPlayerClan(playerId);
    }

    public static boolean hasPlayerClan(Player player) {
        return ClanManager.getInstance().hasPlayerClan(player);
    }

    public static boolean isSameClan(UUID player1, UUID player2) {
        Optional<Clan> clan1 = getPlayerClan(player1);
        Optional<Clan> clan2 = getPlayerClan(player2);

        if (clan1.isEmpty() || clan2.isEmpty()) {
            return false;
        }

        return clan1.get().getId().equals(clan2.get().getId());
    }

    public static boolean isSameClan(Player player1, Player player2) {
        return isSameClan(player1.getUniqueId(), player2.getUniqueId());
    }

    public static boolean isLeader(UUID playerId) {
        return getPlayerClan(playerId)
                .map(clan -> clan.isLeader(playerId))
                .orElse(false);
    }

    public static boolean isLeader(Player player) {
        return isLeader(player.getUniqueId());
    }

    public static boolean isModerator(UUID playerId) {
        return getPlayerClan(playerId)
                .map(clan -> clan.isModerator(playerId))
                .orElse(false);
    }

    public static boolean isModerator(Player player) {
        return isModerator(player.getUniqueId());
    }

    public static String getActiveProviderName() {
        return ClanManager.getInstance().getActiveProvider().getProviderName();
    }

    public static ClanProvider getActiveProvider() {
        return ClanManager.getInstance().getActiveProvider();
    }

    public static void reload() {
        ClanManager.getInstance().reload();
    }

    public static void shutdown() {
        ClanManager.getInstance().shutdown();
    }

    public static ClanStats getStats() {
        ClanManager manager = ClanManager.getInstance();

        return ClanStats.builder()
                .providerName(manager.getActiveProvider().getProviderName())
                .totalClans(manager.getAllClans().size())
                .playerClanCacheSize(manager.getCacheManager().getPlayerClanCache().size())
                .clanDataCacheSize(manager.getCacheManager().getClanDataCache().size())
                .playerClanCacheHitRate(manager.getCacheManager().getPlayerClanCache().hitRate())
                .clanDataCacheHitRate(manager.getCacheManager().getClanDataCache().hitRate())
                .build();
    }

    public static void clearCache() {
        ClanManager.getInstance().getCacheManager().invalidateAll();
    }

    public static ClanManager getManager() {
        return ClanManager.getInstance();
    }
}
