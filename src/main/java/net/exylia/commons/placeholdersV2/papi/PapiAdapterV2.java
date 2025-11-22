package net.exylia.commons.placeholdersV2.papi;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class PapiAdapterV2 {
    private static PapiAdapterV2 instance;
    private final JavaPlugin plugin;
    private PapiExpanderV2 expander;
    private boolean papiAvailable;

    private PapiAdapterV2(JavaPlugin plugin) {
        this.plugin = plugin;
        this.papiAvailable = checkPapiAvailable();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (PapiAdapterV2.class) {
                if (instance == null) {
                    instance = new PapiAdapterV2(plugin);
                }
            }
        }
    }

    public static PapiAdapterV2 getInstance() {
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
            expander = new PapiExpanderV2(identifier);
            if (expander.register()) {
                DebugUtils.logInfo("PlaceholderAPI expander registered with identifier: " + identifier);
            } else {
                DebugUtils.logWarn("Failed to register PlaceholderAPI expander");
            }
        } catch (Exception e) {
            DebugUtils.logError("Error registering PlaceholderAPI expander: " + e.getMessage());
        }
    }

    public boolean isPapiAvailable() {
        return papiAvailable;
    }

    public void unregister() {
        if (expander != null && papiAvailable) {
            try {
                expander.unregister();
                DebugUtils.logInfo("PlaceholderAPI expander unregistered");
            } catch (Exception e) {
                DebugUtils.logError("Error unregistering PlaceholderAPI expander: " + e.getMessage());
            }
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
