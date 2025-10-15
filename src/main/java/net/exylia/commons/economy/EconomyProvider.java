package net.exylia.commons.economy;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

public interface EconomyProvider {
    String getProviderName();
    boolean isAvailable();
    BigDecimal getBalance(UUID playerId);
    BigDecimal getBalance(OfflinePlayer player);
    boolean hasBalance(UUID playerId, BigDecimal amount);
    boolean hasBalance(OfflinePlayer player, BigDecimal amount);
    EconomyResponse deposit(UUID playerId, BigDecimal amount);
    EconomyResponse deposit(OfflinePlayer player, BigDecimal amount);
    EconomyResponse withdraw(UUID playerId, BigDecimal amount);
    EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount);
    EconomyResponse setBalance(UUID playerId, BigDecimal amount);
    EconomyResponse setBalance(OfflinePlayer player, BigDecimal amount);
    String getCurrencySymbol();
    String getCurrencyName();
    String getCurrencyPluralName();
    String format(BigDecimal amount);
    boolean hasAccount(UUID playerId);
    boolean hasAccount(OfflinePlayer player);
    boolean createAccount(UUID playerId);
    boolean createAccount(OfflinePlayer player);
}
