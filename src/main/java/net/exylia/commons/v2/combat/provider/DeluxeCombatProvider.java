package net.exylia.commons.v2.combat.provider;

import net.exylia.commons.v2.combat.model.CombatData;
import net.exylia.commons.v2.tasks.api.Tasks;
import nl.marido.deluxecombat.api.DeluxeCombatAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeluxeCombatProvider implements CombatProvider {

    private final DeluxeCombatAPI api;
    private final boolean enabled;

    public DeluxeCombatProvider() {
        boolean tempEnabled = false;
        DeluxeCombatAPI tempApi = null;

        try {
            if (Bukkit.getPluginManager().getPlugin("DeluxeCombat") != null) {
                tempApi = new DeluxeCombatAPI();
                tempEnabled = true;
            }
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.api = tempApi;
        this.enabled = tempEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "DeluxeCombat";
    }

    @Override
    public boolean isInCombat(Player player) {
        if (!enabled) return false;
        try {
            return api.isInCombat(player);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getRemainingCombatTime(Player player) {
        if (!enabled) return 0;
        try {
            return api.getRemainingCombatTime(player);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public long getRemainingCombatTimeMillis(Player player) {
        if (!enabled) return 0L;
        try {
            return api.getRemainingCombatTimeMillis(player);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Optional<Player> getCurrentOpponent(Player player) {
        if (!enabled) return Optional.empty();
        try {
            return Optional.ofNullable(api.getCurrentOpponent(player));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void tag(Player target, Player attacker) {
        if (!enabled) return;
        try {
            api.tag(target, attacker, 15);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void tag(Player target, Player attacker, int seconds) {
        if (!enabled) return;
        try {
            api.tag(target, attacker, seconds);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void untag(Player player) {
        if (!enabled) return;
        try {
            api.untag(player);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean hasProtection(Player player) {
        if (!enabled) return false;
        try {
            return api.hasProtection(player);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasPvPEnabled(Player player) {
        if (!enabled) return true;
        try {
            return api.hasPvPEnabled(player);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void togglePvP(Player player, boolean enabled) {
        if (!this.enabled) return;
        try {
            api.togglePvP(player, enabled);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean canAttack(Player attacker, Player defender) {
        if (!enabled) return true;
        try {
            // TODO(human): Implement canAttack logic combining DeluxeCombat checks
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public Optional<CombatData> getPlayerData(Player player) {
        if (!enabled) return Optional.empty();

        try {
            return Optional.of(CombatData.builder()
                    .playerId(player.getUniqueId())
                    .playerName(player.getName())
                    .kills(api.getKills(player))
                    .deaths(api.getDeaths(player))
                    .kdr(api.getKD(player))
                    .streak(api.getStreak(player))
                    .highestStreak(api.getHighestStreak(player))
                    .combatLogs(api.getCombatlogs(player))
                    .points(api.getPoints(player))
                    .providerName("DeluxeCombat")
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CombatData> getPlayerData(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return Optional.empty();
        return getPlayerData(player);
    }

    @Override
    public CompletableFuture<Optional<CombatData>> getPlayerDataAsync(Player player) {
        return Tasks.runValue(() -> getPlayerData(player));
    }

    @Override
    public CompletableFuture<Optional<CombatData>> getPlayerDataAsync(UUID playerId) {
        return Tasks.runValue(() -> getPlayerData(playerId));
    }

    @Override
    public void invalidateCache() {
    }
}
