package net.exylia.commons.v2.placeholders.papi;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PapiAdapter {
    private static PapiAdapter instance;
    private final JavaPlugin plugin;
    private PapiExpander expander;
    @Getter
    private boolean papiAvailable;

    private PapiAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.papiAvailable = checkPapiAvailable();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (PapiAdapter.class) {
                if (instance == null) {
                    instance = new PapiAdapter(plugin);
                }
            }
        }
    }

    public static PapiAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PapiAdapterV2 not initialized");
        }
        return instance;
    }

    public void registerExpander(String identifier) {
        if (!papiAvailable) {
            return;
        }

        try {
            expander = new PapiExpander(identifier);
            if (expander.register()) {
                DebugUtils.logInternalInfo("PlaceholderAPI expander registered with identifier: " + identifier);
            } else {
                DebugUtils.logInternalWarn("Failed to register PlaceholderAPI expander");
            }
        } catch (Exception e) {
            DebugUtils.logInternalError("Error registering PlaceholderAPI expander: " + e.getMessage());
        }
    }

    public void unregister() {
        if (expander != null && papiAvailable) {
            try {
                expander.unregister();
                DebugUtils.logInternalInfo("PlaceholderAPI expander unregistered");
            } catch (Exception e) {
                DebugUtils.logInternalError("Error unregistering PlaceholderAPI expander: " + e.getMessage());
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
            DebugUtils.logInternalError("Error processing PlaceholderAPI placeholders: " + e.getMessage());
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
            DebugUtils.logInternalError("Error processing PlaceholderAPI placeholders: " + e.getMessage());
            return text;
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
