package net.exylia.commons.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class ActionBarUtils {

    private static final Map<Player, BukkitTask> activeTasks = new HashMap<>();
    private static Plugin plugin;

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    public static void sendActionBar(Player player, String message, boolean unlimited) {
        if (!unlimited) {
            MessageUtils.sendActionBarAsync(player, message);
            return;
        }

        cancelActionBar(player);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActionBar(player);
                    return;
                }
                MessageUtils.sendActionBarAsync(player, message);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTasks.put(player, task);
    }

    public static void sendActionBar(Player player, Component component, boolean unlimited) {
        if (!unlimited) {
            MessageUtils.sendActionBarAsync(player, component);
            return;
        }

        cancelActionBar(player);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActionBar(player);
                    return;
                }
                MessageUtils.sendActionBarAsync(player, component);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTasks.put(player, task);
    }

    public static void cancelActionBar(Player player) {
        BukkitTask task = activeTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }
}
