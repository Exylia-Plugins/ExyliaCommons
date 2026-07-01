package net.exylia.commons.v2.clientapi.cooldown.core;

import net.exylia.commons.v2.clientapi.cooldown.adapter.CooldownAdapter;
import net.exylia.commons.v2.clientapi.cooldown.adapter.impl.ApolloCooldownAdapter;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownDefinition;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CooldownManager {

    private final List<CooldownAdapter> adapters = new ArrayList<>();

    public void initialize(Plugin plugin) {
        tryRegisterApollo();
    }

    private void tryRegisterApollo() {
        if (!isAnyPluginEnabled("Apollo", "Apollo-Folia", "Apollo-Bukkit")) {
            DebugAPI.logLibWarn("Apollo (Lunar Client) API not found. ExyliaCommons supports it — add apollo-api as a dependency to enable it.");
            return;
        }
        try {
            ApolloCooldownAdapter adapter = new ApolloCooldownAdapter();
            adapters.add(adapter);
            DebugAPI.logLibSuccess("Apollo (Lunar Client) cooldown support enabled.");
        } catch (NoClassDefFoundError | Exception e) {
            DebugAPI.logLibError("Failed to initialize Apollo (Lunar Client) cooldown adapter: " + e.getMessage());
        }
    }

    private boolean isAnyPluginEnabled(String... names) {
        for (String name : names) {
            if (Bukkit.getPluginManager().isPluginEnabled(name)) return true;
        }
        return false;
    }

    public void registerAdapter(CooldownAdapter adapter) {
        if (adapter == null || !adapter.isAvailable()) return;
        adapters.add(adapter);
        DebugAPI.logLibSuccess("Registered cooldown adapter: " + adapter.getClass().getSimpleName());
    }

    public void display(Player player, CooldownDefinition definition) {
        for (CooldownAdapter adapter : adapters) {
            if (!adapter.isAvailable() || !adapter.supportsPlayer(player)) continue;
            try {
                adapter.display(player, definition);
            } catch (Exception e) {
                DebugAPI.logLibError("Error displaying cooldown via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void remove(Player player, String name) {
        for (CooldownAdapter adapter : adapters) {
            if (!adapter.isAvailable() || !adapter.supportsPlayer(player)) continue;
            try {
                adapter.remove(player, name);
            } catch (Exception e) {
                DebugAPI.logLibError("Error removing cooldown via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void removeAll(Player player) {
        for (CooldownAdapter adapter : adapters) {
            if (!adapter.isAvailable()) continue;
            try {
                adapter.removeAll(player);
            } catch (Exception e) {
                DebugAPI.logLibError("Error removing all cooldowns via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        for (CooldownAdapter adapter : adapters) {
            try {
                adapter.cleanupPlayer(uuid);
            } catch (Exception e) {
                DebugAPI.logLibError("Error cleaning up player cooldown state via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        for (CooldownAdapter adapter : adapters) {
            try {
                adapter.shutdown();
            } catch (Exception e) {
                DebugAPI.logLibError("Error shutting down cooldown adapter " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        adapters.clear();
    }

    public List<CooldownAdapter> getAdapters() {
        return Collections.unmodifiableList(adapters);
    }
}
