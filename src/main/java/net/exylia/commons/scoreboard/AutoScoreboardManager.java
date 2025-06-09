package net.exylia.commons.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static net.exylia.commons.utils.DebugUtils.logError;

/**
 * Manager para aplicar scoreboards automáticamente
 */
public class AutoScoreboardManager implements Listener {

    private final ExyliaScoreboardManager scoreboardManager;
    private final Plugin plugin;
    private final Map<String, Function<Player, Boolean>> conditions;
    private boolean registered = false;

    public AutoScoreboardManager(ExyliaScoreboardManager scoreboardManager, Plugin plugin) {
        this.scoreboardManager = scoreboardManager;
        this.plugin = plugin;
        this.conditions = new HashMap<>();
    }

    /**
     * Registra un template para aplicación automática
     */
    public void registerTemplate(String templateId) {
        registerTemplate(templateId, player -> true);
    }

    /**
     * Registra un template con condición
     */
    public void registerTemplate(String templateId, Function<Player, Boolean> condition) {
        conditions.put(templateId, condition);

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;

            // Aplicar a jugadores ya conectados
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkAndApply(player);
            }
        }
    }

    public void unregisterTemplate(String templateId) {
        conditions.remove(templateId);

        if (conditions.isEmpty() && registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    public void unregisterAll() {
        conditions.clear();
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    private void checkAndApply(Player player) {
        for (Map.Entry<String, Function<Player, Boolean>> entry : conditions.entrySet()) {
            String templateId = entry.getKey();
            Function<Player, Boolean> condition = entry.getValue();

            try {
                if (condition.apply(player)) {
                    if (!scoreboardManager.hasScoreboard(player)) {
                        scoreboardManager.showScoreboard(player, templateId);
                    }
                    return;
                }
            } catch (Exception e) {
                logError("Error verificando condición para template " + templateId + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Esperar un poco para que otros plugins configuren sus datos
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                checkAndApply(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scoreboardManager.hideScoreboard(event.getPlayer());
    }
}