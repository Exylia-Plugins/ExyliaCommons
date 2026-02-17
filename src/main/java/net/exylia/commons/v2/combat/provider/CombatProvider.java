package net.exylia.commons.v2.combat.provider;

import net.exylia.commons.v2.combat.model.CombatData;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CombatProvider {

    boolean isEnabled();

    String getProviderName();

    boolean isInCombat(Player player);

    int getRemainingCombatTime(Player player);

    long getRemainingCombatTimeMillis(Player player);

    Optional<Player> getCurrentOpponent(Player player);

    void tag(Player target, Player attacker);

    void tag(Player target, Player attacker, int seconds);

    void untag(Player player);

    boolean hasProtection(Player player);

    boolean hasPvPEnabled(Player player);

    void togglePvP(Player player, boolean enabled);

    boolean canAttack(Player attacker, Player defender);

    Optional<CombatData> getPlayerData(Player player);

    Optional<CombatData> getPlayerData(UUID playerId);

    CompletableFuture<Optional<CombatData>> getPlayerDataAsync(Player player);

    CompletableFuture<Optional<CombatData>> getPlayerDataAsync(UUID playerId);

    void invalidateCache();
}
