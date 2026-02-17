package net.exylia.commons.v2.economy.provider;

import net.exylia.commons.v2.economy.model.EconomyResponse;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

public class PlayerPointsProvider implements EconomyProvider {
    private PlayerPointsAPI api;
    private boolean available;

    public PlayerPointsProvider() {
        setup();
    }

    private void setup() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) return;
        api = PlayerPoints.getInstance().getAPI();
        available = api != null;
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public boolean isAvailable() {
        return available && api != null;
    }

    @Override
    public BigDecimal getBalance(OfflinePlayer player) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(api.look(player.getUniqueId()));
    }

    @Override
    public BigDecimal getBalance(UUID playerId) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(api.look(playerId));
    }

    @Override
    public boolean has(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return false;
        return getBalance(player).compareTo(amount) >= 0;
    }

    @Override
    public boolean has(UUID playerId, BigDecimal amount) {
        if (!isAvailable()) return false;
        return getBalance(playerId).compareTo(amount) >= 0;
    }

    @Override
    public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return EconomyResponse.notAvailable();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return EconomyResponse.invalidAmount();

        boolean success = api.give(player.getUniqueId(), amount.intValue());
        return success
                ? EconomyResponse.success(amount, getBalance(player))
                : EconomyResponse.failure("Failed to give points");
    }

    @Override
    public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return EconomyResponse.notAvailable();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return EconomyResponse.invalidAmount();
        if (!has(player, amount)) return EconomyResponse.insufficientFunds(amount, getBalance(player));

        boolean success = api.take(player.getUniqueId(), amount.intValue());
        return success
                ? EconomyResponse.success(amount, getBalance(player))
                : EconomyResponse.failure("Failed to take points");
    }

    @Override
    public EconomyResponse set(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return EconomyResponse.notAvailable();
        if (amount.compareTo(BigDecimal.ZERO) < 0) return EconomyResponse.invalidAmount();

        boolean success = api.set(player.getUniqueId(), amount.intValue());
        return success
                ? EconomyResponse.success(amount, getBalance(player))
                : EconomyResponse.failure("Failed to set points");
    }

    @Override
    public String format(BigDecimal amount) {
        return amount.intValue() + " " + currencyName(amount.intValue() != 1);
    }

    @Override
    public String currencyName(boolean plural) {
        return plural ? "points" : "point";
    }

    @Override
    public String currencySymbol() {
        return "\u2605";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return api.look(player.getUniqueId()) >= 0;
    }

    @Override
    public boolean createAccount(OfflinePlayer player) {
        return isAvailable();
    }
}
