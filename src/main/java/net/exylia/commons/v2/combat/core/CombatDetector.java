package net.exylia.commons.v2.combat.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.combat.provider.*;
import org.bukkit.Bukkit;

public class CombatDetector {

    private static final String DELUXE_COMBAT = "DeluxeCombat";
    private static final String PVP_MANAGER = "PvPManager";

    public CombatProvider detectBestProvider() {
        if (isPluginAvailable(DELUXE_COMBAT)) {
            DebugAPI.logLibInfo("Detected DeluxeCombat");
            DeluxeCombatProvider provider = new DeluxeCombatProvider();
            if (provider.isEnabled()) {
                return provider;
            }
        }

        if (isPluginAvailable(PVP_MANAGER)) {
            DebugAPI.logLibInfo("Detected PvPManager");
            PvPManagerProvider provider = new PvPManagerProvider();
            if (provider.isEnabled()) {
                return provider;
            }
        }

        DebugAPI.logLibWarn("No combat plugin detected");
        return new NoCombatProvider();
    }

    public boolean isPluginAvailable(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    public boolean hasAnyCombatPlugin() {
        return isPluginAvailable(DELUXE_COMBAT)
                || isPluginAvailable(PVP_MANAGER);
    }
}
