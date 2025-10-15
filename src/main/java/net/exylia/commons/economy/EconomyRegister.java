package net.exylia.commons.economy;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.economy.providers.DummyEconomyProvider;
import net.exylia.commons.economy.providers.VaultEconomyProvider;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class EconomyRegister {
    private static EconomyProvider currentProvider;
    private static final Map<String, EconomyProvider> registeredPlugins = new HashMap<>();
    private static boolean initialized = false;

    public static EconomyProvider init(ExyliaPlugin plugin) {
        String pluginName = plugin.getName();
        if (registeredPlugins.containsKey(pluginName)) {
            DebugUtils.logInternalInfo("Economy already initialized for " + pluginName);
            return registeredPlugins.get(pluginName);
        }
        if (!initialized) {
            initializeEconomySystem();
            initialized = true;
        }
        registeredPlugins.put(pluginName, currentProvider);
        DebugUtils.logInternalInfo("Economy initialized for " + pluginName + " using provider: " + currentProvider.getProviderName());
        return currentProvider;
    }
    public static EconomyProvider getCurrentProvider() {
        if (!initialized) {
            initializeEconomySystem();
            initialized = true;
        }
        return currentProvider;
    }
    public static boolean isEconomyAvailable() {
        return getCurrentProvider().isAvailable();
    }
    public static void unregister(ExyliaPlugin plugin) {
        String pluginName = plugin.getName();
        if (registeredPlugins.remove(pluginName) != null) {
            DebugUtils.logInternalInfo("Economy unregistered for " + pluginName);
        }
        if (registeredPlugins.isEmpty()) {
            cleanup();
        }
    }
    public static void reinitialize() {
        DebugUtils.logInternalInfo("Reinitializing economy system...");
        initialized = false;
        initializeEconomySystem();
        initialized = true;

        for (String pluginName : registeredPlugins.keySet()) {
            registeredPlugins.put(pluginName, currentProvider);
        }

        DebugUtils.logInternalInfo("Economy system reinitialized. Provider: " + currentProvider.getProviderName());
    }

    public static EconomyStatus getStatus() {
        return new EconomyStatus(
                currentProvider != null ? currentProvider.getProviderName() : "None",
                isEconomyAvailable(),
                registeredPlugins.size(),
                registeredPlugins.keySet()
        );
    }

    private static void initializeEconomySystem() {
        DebugUtils.logInternalInfo("Initializing economy system...");

        EconomyProvider provider = tryProvider(VaultEconomyProvider::new, "Vault");

        if (provider == null || !provider.isAvailable()) {
            provider = new DummyEconomyProvider();
            DebugUtils.logInternalWarn("No economy plugin detected. Using dummy provider.");
        } else {
            DebugUtils.logInternalSuccess("Economy provider initialized: " + provider.getProviderName());
        }

        currentProvider = provider;
    }

    private static EconomyProvider tryProvider(ProviderFactory factory, String name) {
        try {
            EconomyProvider provider = factory.create();
            if (provider.isAvailable()) {
                DebugUtils.logInternalInfo("Successfully initialized " + name + " economy provider");
                return provider;
            } else {
                DebugUtils.logInternalInfo(name + " economy provider not available");
            }
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Failed to initialize " + name + " provider: " + e.getMessage());
        }
        return null;
    }

    private static void cleanup() {
        DebugUtils.logInternalInfo("Cleaning up economy system...");
        currentProvider = null;
        initialized = false;
    }

    @FunctionalInterface
    private interface ProviderFactory {
        EconomyProvider create() throws Exception;
    }

    public static class EconomyStatus {
        private final String providerName;
        private final boolean available;
        private final int registeredPlugins;
        private final Iterable<String> pluginNames;

        public EconomyStatus(String providerName, boolean available, int registeredPlugins, Iterable<String> pluginNames) {
            this.providerName = providerName;
            this.available = available;
            this.registeredPlugins = registeredPlugins;
            this.pluginNames = pluginNames;
        }

        public String getProviderName() { return providerName; }
        public boolean isAvailable() { return available; }
        public int getRegisteredPlugins() { return registeredPlugins; }
        public Iterable<String> getPluginNames() { return pluginNames; }

        @Override
        public String toString() {
            return String.format("EconomyStatus{provider='%s', available=%s, plugins=%d}",
                    providerName, available, registeredPlugins);
        }
    }
}
