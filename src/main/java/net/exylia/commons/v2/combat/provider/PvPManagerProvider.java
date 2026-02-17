package net.exylia.commons.v2.combat.provider;

import me.chancesd.pvpmanager.PvPManager;
import me.chancesd.pvpmanager.manager.PlayerManager;
import me.chancesd.pvpmanager.player.CombatPlayer;
import me.chancesd.pvpmanager.player.UntagReason;
import net.exylia.commons.v2.combat.model.CombatData;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PvPManagerProvider implements CombatProvider {

    private final PlayerManager playerManager;
    private final boolean enabled;

    public PvPManagerProvider() {
        PlayerManager tempManager = null;
        boolean tempEnabled = false;

        try {
            PvPManager plugin = (PvPManager) Bukkit.getPluginManager().getPlugin("PvPManager");
            if (plugin != null) {
                tempManager = plugin.getPlayerManager();
                tempEnabled = tempManager != null;
            }
        } catch (Exception e) {
            tempEnabled = false;
        }

        this.playerManager = tempManager;
        this.enabled = tempEnabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "PvPManager";
    }

    @Override
    public boolean isInCombat(Player player) {
        if (!enabled) return false;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            return cp != null && cp.isInCombat();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getRemainingCombatTime(Player player) {
        if (!enabled) return 0;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp == null || !cp.isInCombat()) return 0;
            return (int) (getRemainingCombatTimeMillis(player) / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public long getRemainingCombatTimeMillis(Player player) {
        if (!enabled) return 0L;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp == null || !cp.isInCombat()) return 0L;
            return cp.getTagTimeLeft();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Optional<Player> getCurrentOpponent(Player player) {
        if (!enabled) return Optional.empty();
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp == null || !cp.isInCombat()) return Optional.empty();
            CombatPlayer enemy = cp.getEnemy();
            return enemy != null ? Optional.of(enemy.getPlayer()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void tag(Player target, Player attacker) {
        if (!enabled) return;
        try {
            CombatPlayer cp = CombatPlayer.get(target);
            if (cp != null) {
                cp.tag(true, CombatPlayer.get(attacker));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void tag(Player target, Player attacker, int seconds) {
        if (!enabled) return;
        try {
            CombatPlayer cp = CombatPlayer.get(target);
            if (cp != null) {
                cp.tag(true, CombatPlayer.get(attacker), seconds * 1000L);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void untag(Player player) {
        if (!enabled) return;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp != null) {
                cp.untag(UntagReason.PLUGIN_API);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean hasProtection(Player player) {
        if (!enabled) return false;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp == null) return false;
            return cp.isNewbie() || cp.hasRespawnProtection();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasPvPEnabled(Player player) {
        if (!enabled) return true;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp == null) return true;
            return cp.hasPvPEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void togglePvP(Player player, boolean enabled) {
        if (!this.enabled) return;
        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp != null) {
                cp.setPvP(enabled);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean canAttack(Player attacker, Player defender) {
        if (!enabled) return true;
        try {
            return playerManager.canAttack(attacker, defender);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public Optional<CombatData> getPlayerData(Player player) {
        if (!enabled) return Optional.empty();

        try {
            CombatPlayer cp = CombatPlayer.get(player);
            if (cp == null) return Optional.empty();

            return Optional.of(CombatData.builder()
                    .playerId(player.getUniqueId())
                    .playerName(player.getName())
                    .kills(0)
                    .deaths(0)
                    .kdr(0.0)
                    .streak(0)
                    .highestStreak(0)
                    .combatLogs(0)
                    .points(0)
                    .providerName("PvPManager")
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
