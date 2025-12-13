package net.exylia.commons.v2.clan.core;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.clan.provider.*;
import org.bukkit.Bukkit;

public class ClanDetector {

    private static final String ULTIMATE_CLANS = "UltimateClans";
    private static final String SIMPLE_CLANS = "SimpleClans";
    private static final String KINGDOMS_X = "Kingdoms";

    public ClanProvider detectBestProvider() {
        if (isPluginAvailable(ULTIMATE_CLANS)) {
            DebugUtils.logInternalInfo("Detected UltimateClans");
            UltimateClanProvider provider = new UltimateClanProvider();
            if (provider.isEnabled()) {
                return provider;
            }
        }

        if (isPluginAvailable(KINGDOMS_X)) {
            DebugUtils.logInternalInfo("Detected KingdomsX");
            KingdomsProvider provider = new KingdomsProvider();
            if (provider.isEnabled()) {
                return provider;
            }
        }

        if (isPluginAvailable(SIMPLE_CLANS)) {
            DebugUtils.logInternalInfo("Detected SimpleClans");
            SimpleClansProvider provider = new SimpleClansProvider();
            if (provider.isEnabled()) {
                return provider;
            }
        }

        DebugUtils.logInternalWarn("No clan plugin detected");
        return new NoClanProvider();
    }

    public boolean isPluginAvailable(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    public boolean hasAnyClanPlugin() {
        return isPluginAvailable(ULTIMATE_CLANS)
                || isPluginAvailable(SIMPLE_CLANS)
                || isPluginAvailable(KINGDOMS_X);
    }
}
