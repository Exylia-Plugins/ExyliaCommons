package net.exylia.commons.v2.economy.provider;

import net.exylia.commons.v2.economy.model.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

public interface EconomyProvider {
    String getName();
    boolean isAvailable();

    BigDecimal getBalance(OfflinePlayer player);
    BigDecimal getBalance(UUID playerId);

    boolean has(OfflinePlayer player, BigDecimal amount);
    boolean has(UUID playerId, BigDecimal amount);

    EconomyResponse deposit(OfflinePlayer player, BigDecimal amount);
    EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount);
    EconomyResponse set(OfflinePlayer player, BigDecimal amount);

    String format(BigDecimal amount);
    String currencyName(boolean plural);
    String currencySymbol();

    boolean hasAccount(OfflinePlayer player);
    boolean createAccount(OfflinePlayer player);
}
