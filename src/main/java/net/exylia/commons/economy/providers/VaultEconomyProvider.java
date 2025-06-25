package net.exylia.commons.economy.providers;

import net.exylia.commons.economy.EconomyProvider;
import net.exylia.commons.economy.EconomyResponse;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementación de EconomyProvider usando Vault
 */
public class VaultEconomyProvider implements EconomyProvider {
    private Economy economy;
    private boolean available = false;

    public VaultEconomyProvider() {
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }

        economy = rsp.getProvider();
        available = economy != null;
    }

    @Override
    public String getProviderName() {
        return "Vault";
    }

    @Override
    public boolean isAvailable() {
        return available && economy != null;
    }

    @Override
    public BigDecimal getBalance(UUID playerId) {
        if (!isAvailable()) return BigDecimal.ZERO;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return BigDecimal.valueOf(economy.getBalance(player));
    }

    @Override
    public BigDecimal getBalance(OfflinePlayer player) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(economy.getBalance(player));
    }

    @Override
    public boolean hasBalance(UUID playerId, BigDecimal amount) {
        if (!isAvailable()) return false;
        return getBalance(playerId).compareTo(amount) >= 0;
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return false;
        return getBalance(player).compareTo(amount) >= 0;
    }

    @Override
    public EconomyResponse deposit(UUID playerId, BigDecimal amount) {
        return deposit(Bukkit.getOfflinePlayer(playerId), amount);
    }

    @Override
    public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            return EconomyResponse.failure("Economía no disponible");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return EconomyResponse.invalidAmount();
        }

        net.milkbowl.vault.economy.EconomyResponse response = economy.depositPlayer(player, amount.doubleValue());

        if (response.transactionSuccess()) {
            return EconomyResponse.success(amount, BigDecimal.valueOf(response.balance), "Depósito exitoso");
        } else {
            return EconomyResponse.failure(response.errorMessage);
        }
    }

    @Override
    public EconomyResponse withdraw(UUID playerId, BigDecimal amount) {
        return withdraw(Bukkit.getOfflinePlayer(playerId), amount);
    }

    @Override
    public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            return EconomyResponse.failure("Economía no disponible");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return EconomyResponse.invalidAmount();
        }

        if (!hasBalance(player, amount)) {
            return EconomyResponse.insufficientFunds(amount, getBalance(player));
        }

        net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(player, amount.doubleValue());

        if (response.transactionSuccess()) {
            return EconomyResponse.success(amount, BigDecimal.valueOf(response.balance), "Retiro exitoso");
        } else {
            return EconomyResponse.failure(response.errorMessage);
        }
    }

    @Override
    public EconomyResponse setBalance(UUID playerId, BigDecimal amount) {
        return setBalance(Bukkit.getOfflinePlayer(playerId), amount);
    }

    @Override
    public EconomyResponse setBalance(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            return EconomyResponse.failure("Economía no disponible");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return EconomyResponse.invalidAmount();
        }

        BigDecimal currentBalance = getBalance(player);
        BigDecimal difference = amount.subtract(currentBalance);

        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            return deposit(player, difference);
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            return withdraw(player, difference.abs());
        } else {
            return EconomyResponse.success(BigDecimal.ZERO, amount, "Balance ya establecido");
        }
    }

    @Override
    public String getCurrencySymbol() {
        if (!isAvailable()) return "$";
        return "$"; // Vault no proporciona símbolo directo
    }

    @Override
    public String getCurrencyName() {
        if (!isAvailable()) return "dollar";
        return economy.currencyNameSingular();
    }

    @Override
    public String getCurrencyPluralName() {
        if (!isAvailable()) return "dollars";
        return economy.currencyNamePlural();
    }

    @Override
    public String format(BigDecimal amount) {
        if (!isAvailable()) return amount.toString();
        return economy.format(amount.doubleValue());
    }

    @Override
    public boolean hasAccount(UUID playerId) {
        return hasAccount(Bukkit.getOfflinePlayer(playerId));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return economy.hasAccount(player);
    }

    @Override
    public boolean createAccount(UUID playerId) {
        return createAccount(Bukkit.getOfflinePlayer(playerId));
    }

    @Override
    public boolean createAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return economy.createPlayerAccount(player);
    }
}