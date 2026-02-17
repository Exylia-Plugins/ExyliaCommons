package net.exylia.commons.v2.economy.api;

import net.exylia.commons.v2.economy.core.EconomyManager;
import net.exylia.commons.v2.economy.model.CurrencyType;
import net.exylia.commons.v2.economy.model.EconomyResponse;
import net.exylia.commons.v2.economy.model.TransferResult;
import net.exylia.commons.v2.economy.provider.EconomyProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public final class EconomyAPI {

    private EconomyAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        EconomyManager.getInstance().initialize(plugin);
    }

    public static boolean isAvailable() {
        return EconomyManager.getInstance().isAvailable();
    }

    public static boolean isAvailable(CurrencyType type) {
        return EconomyManager.getInstance().isAvailable(type);
    }

    public static BigDecimal getBalance(OfflinePlayer player) {
        return EconomyManager.getInstance().getBalance(player);
    }

    public static BigDecimal getBalance(OfflinePlayer player, CurrencyType type) {
        return EconomyManager.getInstance().getBalance(player, type);
    }

    public static BigDecimal getBalance(UUID playerId) {
        return EconomyManager.getInstance().getBalance(playerId);
    }

    public static BigDecimal getBalance(UUID playerId, CurrencyType type) {
        return EconomyManager.getInstance().getBalance(playerId, type);
    }

    public static boolean has(OfflinePlayer player, BigDecimal amount) {
        return EconomyManager.getInstance().has(player, amount);
    }

    public static boolean has(OfflinePlayer player, double amount) {
        return has(player, BigDecimal.valueOf(amount));
    }

    public static boolean has(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return EconomyManager.getInstance().has(player, amount, type);
    }

    public static EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) {
        return EconomyManager.getInstance().deposit(player, amount);
    }

    public static EconomyResponse deposit(OfflinePlayer player, double amount) {
        return deposit(player, BigDecimal.valueOf(amount));
    }

    public static EconomyResponse deposit(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return EconomyManager.getInstance().deposit(player, amount, type);
    }

    public static EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) {
        return EconomyManager.getInstance().withdraw(player, amount);
    }

    public static EconomyResponse withdraw(OfflinePlayer player, double amount) {
        return withdraw(player, BigDecimal.valueOf(amount));
    }

    public static EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return EconomyManager.getInstance().withdraw(player, amount, type);
    }

    public static EconomyResponse set(OfflinePlayer player, BigDecimal amount) {
        return EconomyManager.getInstance().set(player, amount);
    }

    public static EconomyResponse set(OfflinePlayer player, double amount) {
        return set(player, BigDecimal.valueOf(amount));
    }

    public static EconomyResponse set(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return EconomyManager.getInstance().set(player, amount, type);
    }

    public static boolean charge(OfflinePlayer player, BigDecimal amount) {
        return withdraw(player, amount).isSuccess();
    }

    public static boolean charge(OfflinePlayer player, double amount) {
        return charge(player, BigDecimal.valueOf(amount));
    }

    public static boolean pay(OfflinePlayer player, BigDecimal amount) {
        return deposit(player, amount).isSuccess();
    }

    public static boolean pay(OfflinePlayer player, double amount) {
        return pay(player, BigDecimal.valueOf(amount));
    }

    public static TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount) {
        return EconomyManager.getInstance().transfer(from, to, amount);
    }

    public static TransferResult transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        return transfer(from, to, BigDecimal.valueOf(amount));
    }

    public static TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount, CurrencyType type) {
        return EconomyManager.getInstance().transfer(from, to, amount, type);
    }

    public static String format(BigDecimal amount) {
        return EconomyManager.getInstance().format(amount);
    }

    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    public static String format(BigDecimal amount, CurrencyType type) {
        return EconomyManager.getInstance().format(amount, type);
    }

    public static EconomyProvider getProvider() {
        return EconomyManager.getInstance().getProvider();
    }

    public static EconomyProvider getProvider(CurrencyType type) {
        return EconomyManager.getInstance().getProvider(type);
    }

    public static Map<CurrencyType, EconomyProvider> getRegisteredProviders() {
        return EconomyManager.getInstance().getRegisteredProviders();
    }

    public static void reload() {
        EconomyManager.getInstance().reload();
    }

    public static void shutdown() {
        EconomyManager.getInstance().shutdown();
    }
}
