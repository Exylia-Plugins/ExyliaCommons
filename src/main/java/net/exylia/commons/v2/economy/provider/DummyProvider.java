package net.exylia.commons.v2.economy.provider;

import net.exylia.commons.v2.economy.model.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

public class DummyProvider implements EconomyProvider {
    @Override public String getName() { return "None"; }
    @Override public boolean isAvailable() { return false; }
    @Override public BigDecimal getBalance(OfflinePlayer player) { return BigDecimal.ZERO; }
    @Override public BigDecimal getBalance(UUID playerId) { return BigDecimal.ZERO; }
    @Override public boolean has(OfflinePlayer player, BigDecimal amount) { return false; }
    @Override public boolean has(UUID playerId, BigDecimal amount) { return false; }
    @Override public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) { return EconomyResponse.notAvailable(); }
    @Override public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) { return EconomyResponse.notAvailable(); }
    @Override public EconomyResponse set(OfflinePlayer player, BigDecimal amount) { return EconomyResponse.notAvailable(); }
    @Override public String format(BigDecimal amount) { return "$" + amount; }
    @Override public String currencyName(boolean plural) { return plural ? "coins" : "coin"; }
    @Override public String currencySymbol() { return "$"; }
    @Override public boolean hasAccount(OfflinePlayer player) { return false; }
    @Override public boolean createAccount(OfflinePlayer player) { return false; }
}
