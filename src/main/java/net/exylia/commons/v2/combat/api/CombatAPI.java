package net.exylia.commons.v2.combat.api;

import net.exylia.commons.v2.combat.core.CombatManager;
import net.exylia.commons.v2.combat.model.CombatData;
import net.exylia.commons.v2.combat.provider.CombatProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CombatAPI {

    private CombatAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        CombatManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return CombatManager.isInitialized();
    }

    public static boolean isInCombat(Player player) {
        return CombatManager.getInstance().isInCombat(player);
    }

    public static int getRemainingCombatTime(Player player) {
        return CombatManager.getInstance().getRemainingCombatTime(player);
    }

    public static long getRemainingCombatTimeMillis(Player player) {
        return CombatManager.getInstance().getRemainingCombatTimeMillis(player);
    }

    public static Optional<Player> getCurrentOpponent(Player player) {
        return CombatManager.getInstance().getCurrentOpponent(player);
    }

    public static void tag(Player target, Player attacker) {
        CombatManager.getInstance().tag(target, attacker);
    }

    public static void tag(Player target, Player attacker, int seconds) {
        CombatManager.getInstance().tag(target, attacker, seconds);
    }

    public static void untag(Player player) {
        CombatManager.getInstance().untag(player);
    }

    public static boolean hasProtection(Player player) {
        return CombatManager.getInstance().hasProtection(player);
    }

    public static boolean hasPvPEnabled(Player player) {
        return CombatManager.getInstance().hasPvPEnabled(player);
    }

    public static void togglePvP(Player player, boolean enabled) {
        CombatManager.getInstance().togglePvP(player, enabled);
    }

    public static boolean canAttack(Player attacker, Player defender) {
        return CombatManager.getInstance().canAttack(attacker, defender);
    }

    public static Optional<CombatData> getPlayerData(Player player) {
        return CombatManager.getInstance().getPlayerData(player);
    }

    public static Optional<CombatData> getPlayerData(UUID playerId) {
        return CombatManager.getInstance().getPlayerData(playerId);
    }

    public static CompletableFuture<Optional<CombatData>> getPlayerDataAsync(Player player) {
        return CombatManager.getInstance().getPlayerDataAsync(player);
    }

    public static CompletableFuture<Optional<CombatData>> getPlayerDataAsync(UUID playerId) {
        return CombatManager.getInstance().getPlayerDataAsync(playerId);
    }

    public static Optional<Integer> getKills(Player player) {
        return getPlayerData(player).map(CombatData::getKills);
    }

    public static Optional<Integer> getDeaths(Player player) {
        return getPlayerData(player).map(CombatData::getDeaths);
    }

    public static Optional<Double> getKDR(Player player) {
        return getPlayerData(player).map(CombatData::getKdr);
    }

    public static Optional<Integer> getStreak(Player player) {
        return getPlayerData(player).map(CombatData::getStreak);
    }

    public static Optional<Integer> getHighestStreak(Player player) {
        return getPlayerData(player).map(CombatData::getHighestStreak);
    }

    public static Optional<Integer> getPoints(Player player) {
        return getPlayerData(player).map(CombatData::getPoints);
    }

    public static String getActiveProviderName() {
        return CombatManager.getInstance().getActiveProvider().getProviderName();
    }

    public static CombatProvider getActiveProvider() {
        return CombatManager.getInstance().getActiveProvider();
    }

    public static void reload() {
        CombatManager.getInstance().reload();
    }

    public static void shutdown() {
        CombatManager.getInstance().shutdown();
    }

    public static CombatStats getStats() {
        CombatManager manager = CombatManager.getInstance();

        return CombatStats.builder()
                .providerName(manager.getActiveProvider().getProviderName())
                .combatDataCacheSize(manager.getCombatDataCache().size())
                .combatDataCacheHitRate(manager.getCombatDataCache().hitRate())
                .build();
    }

    public static void clearCache() {
        CombatManager.getInstance().getCombatDataCache().invalidateAll();
    }

    public static CombatManager getManager() {
        return CombatManager.getInstance();
    }
}
