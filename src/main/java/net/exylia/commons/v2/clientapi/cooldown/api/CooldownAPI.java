package net.exylia.commons.v2.clientapi.cooldown.api;

import net.exylia.commons.v2.clientapi.cooldown.core.CooldownManager;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class CooldownAPI {

    private static CooldownManager manager;

    private CooldownAPI() {}

    public static void initialize(CooldownManager cooldownManager) {
        manager = cooldownManager;
    }

    public static void display(Player player, CooldownDefinition definition) {
        if (manager == null) return;
        manager.display(player, definition);
    }

    public static void display(Collection<? extends Player> players, CooldownDefinition definition) {
        if (manager == null) return;
        for (Player player : players) {
            manager.display(player, definition);
        }
    }

    public static void remove(Player player, String name) {
        if (manager == null) return;
        manager.remove(player, name);
    }

    public static void remove(Collection<? extends Player> players, String name) {
        if (manager == null) return;
        for (Player player : players) {
            manager.remove(player, name);
        }
    }

    public static void removeAll(Player player) {
        if (manager == null) return;
        manager.removeAll(player);
    }

    public static void removeAll(Collection<? extends Player> players) {
        if (manager == null) return;
        for (Player player : players) {
            manager.removeAll(player);
        }
    }

    public static boolean isInitialized() {
        return manager != null;
    }
}
