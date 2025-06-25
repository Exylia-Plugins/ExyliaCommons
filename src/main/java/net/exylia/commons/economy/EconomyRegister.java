package net.exylia.commons.economy;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.economy.providers.DummyEconomyProvider;
import net.exylia.commons.economy.providers.VaultEconomyProvider;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * Registrador centralizado de economía para plugins Exylia
 */
public class EconomyRegister {
    private static EconomyProvider currentProvider;
    private static final Map<String, EconomyProvider> registeredPlugins = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Inicializa la economía para un plugin
     * @param plugin El plugin que solicita economía
     * @return El proveedor de economía disponible
     */
    public static EconomyProvider init(ExyliaPlugin plugin) {
        String pluginName = plugin.getName();

        // Si ya está registrado, devolver el mismo proveedor
        if (registeredPlugins.containsKey(pluginName)) {
            DebugUtils.logInfo("Economy already initialized for " + pluginName);
            return registeredPlugins.get(pluginName);
        }

        // Inicializar sistema si es la primera vez
        if (!initialized) {
            initializeEconomySystem();
            initialized = true;
        }

        // Registrar el plugin y devolver el proveedor
        registeredPlugins.put(pluginName, currentProvider);

        DebugUtils.logInfo("Economy initialized for " + pluginName + " using provider: " + currentProvider.getProviderName());

        return currentProvider;
    }

    /**
     * Obtiene el proveedor actual sin inicializar
     */
    public static EconomyProvider getCurrentProvider() {
        if (!initialized) {
            initializeEconomySystem();
            initialized = true;
        }
        return currentProvider;
    }

    /**
     * Verifica si hay economía disponible
     */
    public static boolean isEconomyAvailable() {
        return getCurrentProvider().isAvailable();
    }

    /**
     * Desregistra un plugin del sistema de economía
     */
    public static void unregister(ExyliaPlugin plugin) {
        String pluginName = plugin.getName();
        if (registeredPlugins.remove(pluginName) != null) {
            DebugUtils.logInfo("Economy unregistered for " + pluginName);
        }

        // Si no quedan plugins registrados, limpiar sistema
        if (registeredPlugins.isEmpty()) {
            cleanup();
        }
    }

    /**
     * Fuerza la reinicialización del sistema de economía
     */
    public static void reinitialize() {
        DebugUtils.logInfo("Reinitializing economy system...");
        initialized = false;
        initializeEconomySystem();
        initialized = true;

        // Actualizar todos los plugins registrados
        for (String pluginName : registeredPlugins.keySet()) {
            registeredPlugins.put(pluginName, currentProvider);
        }

        DebugUtils.logInfo("Economy system reinitialized. Provider: " + currentProvider.getProviderName());
    }

    /**
     * Obtiene información del estado actual
     */
    public static EconomyStatus getStatus() {
        return new EconomyStatus(
                currentProvider != null ? currentProvider.getProviderName() : "None",
                isEconomyAvailable(),
                registeredPlugins.size(),
                registeredPlugins.keySet()
        );
    }

    private static void initializeEconomySystem() {
        DebugUtils.logInfo("Initializing economy system...");

        // Intentar proveedores en orden de prioridad
        EconomyProvider provider = tryProvider(VaultEconomyProvider::new, "Vault");

        // Si no hay proveedores disponibles, usar dummy
        if (provider == null || !provider.isAvailable()) {
            provider = new DummyEconomyProvider();
            DebugUtils.logWarn("No economy plugin detected. Using dummy provider.");
        } else {
            DebugUtils.logSuccess("Economy provider initialized: " + provider.getProviderName());
        }

        currentProvider = provider;
    }

    private static EconomyProvider tryProvider(ProviderFactory factory, String name) {
        try {
            EconomyProvider provider = factory.create();
            if (provider.isAvailable()) {
                DebugUtils.logInfo("Successfully initialized " + name + " economy provider");
                return provider;
            } else {
                DebugUtils.logInfo(name + " economy provider not available");
            }
        } catch (Exception e) {
            DebugUtils.logWarn("Failed to initialize " + name + " provider: " + e.getMessage());
        }
        return null;
    }

    private static void cleanup() {
        DebugUtils.logInfo("Cleaning up economy system...");
        currentProvider = null;
        initialized = false;
    }

    @FunctionalInterface
    private interface ProviderFactory {
        EconomyProvider create() throws Exception;
    }

    /**
     * Información del estado del sistema de economía
     */
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