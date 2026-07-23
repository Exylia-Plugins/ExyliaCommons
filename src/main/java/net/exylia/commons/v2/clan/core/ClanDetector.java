package net.exylia.commons.v2.clan.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.clan.provider.*;
import org.bukkit.Bukkit;

import java.util.List;

public class ClanDetector {

    private static final String FCTIONS_UUID = "Factions";
    private static final String HUSK_TOWNS = "HuskTowns";
    private static final String ZEL_TEAMS = "ZelTeams";
    private static final String RUNITH_CLANS = "RunithClans";
    private static final String ULTIMATE_CLANS = "UltimateClans";
    private static final String SIMPLE_CLANS = "SimpleClans";
    private static final String KINGDOMS_X = "Kingdoms";
    private static final String EXYLIA_CLANS = "ExyliaClans";

    public ClanProvider detectBestProvider() {
        List<ClanProvider> registered = ClanProviderRegistry.getInstance().getOrderedProviders();
        for (ClanProvider provider : registered) {
            if (provider.isEnabled()) {
                DebugAPI.logLibInfo("Using registered clan provider: " + provider.getProviderName());
                return provider;
            }
        }

        ClanProvider detected;

        if (isPluginAvailable(FCTIONS_UUID)) {
            detected = tryCreate("FactionsUUID", FactionsUUIDProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(HUSK_TOWNS)) {
            detected = tryCreate("HuskTowns", HuskTownsProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(ZEL_TEAMS)) {
            detected = tryCreate("ZelTeams", ZelTeamsProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(RUNITH_CLANS)) {
            detected = tryCreate("RunithClans", RunithClansProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(ULTIMATE_CLANS)) {
            detected = tryCreate("UltimateClans", UltimateClanProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(KINGDOMS_X)) {
            detected = tryCreate("KingdomsX", KingdomsProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(SIMPLE_CLANS)) {
            detected = tryCreate("SimpleClans", SimpleClansProvider::new);
            if (detected != null) return detected;
        }

        if (isPluginAvailable(EXYLIA_CLANS)) {
            detected = tryCreate("ExyliaClans", ExyliaClansProvider::new);
            if (detected != null) return detected;
        }

        DebugAPI.logLibWarn("No clan plugin detected");
        return new NoClanProvider();
    }

    private ClanProvider tryCreate(String name, java.util.function.Supplier<ClanProvider> factory) {
        try {
            DebugAPI.logLibInfo("Detected " + name);
            ClanProvider provider = factory.get();
            if (provider.isEnabled()) return provider;
        } catch (NoClassDefFoundError e) {
            DebugAPI.logLibWarn("Plugin " + name + " found but API class is not accessible, skipping.");
        } catch (Exception e) {
            DebugAPI.logLibWarn("Failed to initialize " + name + " provider: " + e.getMessage());
        }
        return null;
    }

    public boolean isPluginAvailable(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    public boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean hasAnyClanPlugin() {
        return isPluginAvailable(FCTIONS_UUID)
                || isPluginAvailable(HUSK_TOWNS)
                || isPluginAvailable(ZEL_TEAMS)
                || isPluginAvailable(RUNITH_CLANS)
                || isPluginAvailable(ULTIMATE_CLANS)
                || isPluginAvailable(SIMPLE_CLANS)
                || isPluginAvailable(KINGDOMS_X)
                || isPluginAvailable(EXYLIA_CLANS);
    }
}
