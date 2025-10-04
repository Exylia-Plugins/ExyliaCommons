package net.exylia.commons.economy.providers;

import net.exylia.commons.economy.EconomyProvider;
import net.exylia.commons.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

public class DummyEconomyProvider implements EconomyProvider {

    @Override
    public String getProviderName() {
        return "None";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public BigDecimal getBalance(UUID playerId) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getBalance(OfflinePlayer player) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean hasBalance(UUID playerId, BigDecimal amount) {
        return false;
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, BigDecimal amount) {
        return false;
    }

    @Override
    public EconomyResponse deposit(UUID playerId, BigDecimal amount) {
        return EconomyResponse.failure("No hay sistema de economía disponible");
    }

    @Override
    public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) {
        return EconomyResponse.failure("No hay sistema de economía disponible");
    }

    @Override
    public EconomyResponse withdraw(UUID playerId, BigDecimal amount) {
        return EconomyResponse.failure("No hay sistema de economía disponible");
    }

    @Override
    public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) {
        return EconomyResponse.failure("No hay sistema de economía disponible");
    }

    @Override
    public EconomyResponse setBalance(UUID playerId, BigDecimal amount) {
        return EconomyResponse.failure("No hay sistema de economía disponible");
    }

    @Override
    public EconomyResponse setBalance(OfflinePlayer player, BigDecimal amount) {
        return EconomyResponse.failure("No hay sistema de economía disponible");
    }

    @Override
    public String getCurrencySymbol() {
        return "$";
    }

    @Override
    public String getCurrencyName() {
        return "coin";
    }

    @Override
    public String getCurrencyPluralName() {
        return "coins";
    }

    @Override
    public String format(BigDecimal amount) {
        return getCurrencySymbol() + amount.toString();
    }

    @Override
    public boolean hasAccount(UUID playerId) {
        return false;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return false;
    }

    @Override
    public boolean createAccount(UUID playerId) {
        return false;
    }

    @Override
    public boolean createAccount(OfflinePlayer player) {
        return false;
    }
}