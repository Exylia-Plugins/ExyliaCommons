package net.exylia.commons.v2.economy.provider;

import net.exylia.commons.v2.economy.model.EconomyResponse;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.UUID;

public class VaultProvider implements EconomyProvider {
    private Economy economy;
    private boolean available;

    public VaultProvider() {
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        economy = rsp.getProvider();
        available = economy != null;
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public boolean isAvailable() {
        return available && economy != null;
    }

    @Override
    public BigDecimal getBalance(OfflinePlayer player) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(economy.getBalance(player));
    }

    @Override
    public BigDecimal getBalance(UUID playerId) {
        return getBalance(Bukkit.getOfflinePlayer(playerId));
    }

    @Override
    public boolean has(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return false;
        return getBalance(player).compareTo(amount) >= 0;
    }

    @Override
    public boolean has(UUID playerId, BigDecimal amount) {
        return has(Bukkit.getOfflinePlayer(playerId), amount);
    }

    @Override
    public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return EconomyResponse.notAvailable();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return EconomyResponse.invalidAmount();

        net.milkbowl.vault.economy.EconomyResponse r = economy.depositPlayer(player, amount.doubleValue());
        return r.transactionSuccess()
                ? EconomyResponse.success(amount, BigDecimal.valueOf(r.balance))
                : EconomyResponse.failure(r.errorMessage);
    }

    @Override
    public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return EconomyResponse.notAvailable();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return EconomyResponse.invalidAmount();
        if (!has(player, amount)) return EconomyResponse.insufficientFunds(amount, getBalance(player));

        net.milkbowl.vault.economy.EconomyResponse r = economy.withdrawPlayer(player, amount.doubleValue());
        return r.transactionSuccess()
                ? EconomyResponse.success(amount, BigDecimal.valueOf(r.balance))
                : EconomyResponse.failure(r.errorMessage);
    }

    @Override
    public EconomyResponse set(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return EconomyResponse.notAvailable();
        if (amount.compareTo(BigDecimal.ZERO) < 0) return EconomyResponse.invalidAmount();

        BigDecimal current = getBalance(player);
        BigDecimal diff = amount.subtract(current);
        if (diff.compareTo(BigDecimal.ZERO) > 0) return deposit(player, diff);
        if (diff.compareTo(BigDecimal.ZERO) < 0) return withdraw(player, diff.abs());
        return EconomyResponse.success(BigDecimal.ZERO, amount);
    }

    @Override
    public String format(BigDecimal amount) {
        if (!isAvailable()) return "$" + amount;
        return economy.format(amount.doubleValue());
    }

    @Override
    public String currencyName(boolean plural) {
        if (!isAvailable()) return plural ? "dollars" : "dollar";
        return plural ? economy.currencyNamePlural() : economy.currencyNameSingular();
    }

    @Override
    public String currencySymbol() {
        return "$";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return economy.hasAccount(player);
    }

    @Override
    public boolean createAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return economy.createPlayerAccount(player);
    }
}
