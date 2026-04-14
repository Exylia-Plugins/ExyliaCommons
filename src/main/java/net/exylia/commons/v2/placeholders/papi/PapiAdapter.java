package net.exylia.commons.v2.placeholders.papi;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;

public class PapiAdapter {
    private static PapiAdapter instance;
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<String, PapiExpander> expanders = new ConcurrentHashMap<>();
    @Getter
    private boolean papiAvailable;

    private PapiAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.papiAvailable = checkPapiAvailable();
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (PapiAdapter.class) {
            if (instance == null) {
                instance = new PapiAdapter(plugin);
                if (instance.papiAvailable) {
                    DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER, "PapiAdapter initialized - PlaceholderAPI detected");
                } else {
                    DebugAPI.logLibInfo(DebugCategory.PLACEHOLDER, "PapiAdapter initialized - PlaceholderAPI not found");
                }
            }
        }
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static PapiAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PapiAdapterV2 not initialized");
        }
        return instance;
    }

    public void registerExpander(String identifier) {
        if (!papiAvailable) {
            DebugAPI.logLibWarn(DebugCategory.PLACEHOLDER, "Cannot register PAPI expander - PlaceholderAPI not available");
            return;
        }

        try {
            DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Registering PAPI expander with identifier: " + identifier);
            PapiExpander expander = new PapiExpander(identifier);
            if (expander.register()) {
                expanders.put(identifier, expander);
                DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER, "PAPI expander registered: " + identifier);
            } else {
                DebugAPI.logLibWarn(DebugCategory.PLACEHOLDER, "Failed to register PAPI expander: " + identifier);
            }
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.PLACEHOLDER, "Error registering PAPI expander: " + e.getMessage(), e);
        }
    }

    public void unregisterExpander(String identifier) {
        PapiExpander expander = expanders.remove(identifier);
        if (expander != null && papiAvailable) {
            try {
                expander.unregister();
                DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER, "PAPI expander unregistered: " + identifier);
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.PLACEHOLDER, "Error unregistering PAPI expander: " + e.getMessage(), e);
            }
        }
    }

    public void unregister() {
        if (papiAvailable) {
            expanders.forEach((id, expander) -> {
                try {
                    expander.unregister();
                } catch (Exception e) {
                    DebugAPI.logLibError(DebugCategory.PLACEHOLDER, "Error unregistering PAPI expander '" + id + "': " + e.getMessage(), e);
                }
            });
        }
        expanders.clear();
    }

    public static void shutdown() {
        synchronized (PapiAdapter.class) {
            if (instance != null) {
                instance.unregister();
                instance = null;
            }
        }
    }

    public String setPlaceholders(Player player, String text) {
        if (!papiAvailable || player == null || text == null) {
            return text;
        }

        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.PLACEHOLDER, "Error processing PAPI placeholders: " + e.getMessage(), e);
            return text;
        }
    }

    public String setPlaceholders(OfflinePlayer player, String text) {
        if (!papiAvailable || player == null || text == null) {
            return text;
        }

        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.PLACEHOLDER, "Error processing PAPI placeholders: " + e.getMessage(), e);
            return text;
        }
    }

    public boolean canResolvePlaceholder(String placeholder) {
        if (!papiAvailable || placeholder == null || placeholder.isEmpty()) {
            return false;
        }

        try {
            String identifier = placeholder.split("_")[0];
            return PlaceholderAPI.getRegisteredIdentifiers().contains(identifier);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkPapiAvailable() {
        try {
            return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (Exception e) {
            return false;
        }
    }
}
