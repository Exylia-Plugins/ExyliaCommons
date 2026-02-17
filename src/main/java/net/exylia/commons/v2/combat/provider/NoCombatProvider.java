package net.exylia.commons.v2.combat.provider;

import net.exylia.commons.v2.combat.model.CombatData;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NoCombatProvider implements CombatProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "None";
    }

    @Override
    public boolean isInCombat(Player player) {
        return false;
    }

    @Override
    public int getRemainingCombatTime(Player player) {
        return 0;
    }

    @Override
    public long getRemainingCombatTimeMillis(Player player) {
        return 0L;
    }

    @Override
    public Optional<Player> getCurrentOpponent(Player player) {
        return Optional.empty();
    }

    @Override
    public void tag(Player target, Player attacker) {
    }

    @Override
    public void tag(Player target, Player attacker, int seconds) {
    }

    @Override
    public void untag(Player player) {
    }

    @Override
    public boolean hasProtection(Player player) {
        return false;
    }

    @Override
    public boolean hasPvPEnabled(Player player) {
        return true;
    }

    @Override
    public void togglePvP(Player player, boolean enabled) {
    }

    @Override
    public boolean canAttack(Player attacker, Player defender) {
        return true;
    }

    @Override
    public Optional<CombatData> getPlayerData(Player player) {
        return Optional.empty();
    }

    @Override
    public Optional<CombatData> getPlayerData(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<CombatData>> getPlayerDataAsync(Player player) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<CombatData>> getPlayerDataAsync(UUID playerId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public void invalidateCache() {
    }
}
