package net.exylia.commons.v2.economy.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.economy.model.CurrencyType;
import net.exylia.commons.v2.economy.model.EconomyResponse;
import net.exylia.commons.v2.economy.model.TransferResult;
import net.exylia.commons.v2.economy.provider.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private static volatile EconomyManager instance;

    private JavaPlugin plugin;
    private final Map<CurrencyType, EconomyProvider> providers = new EnumMap<>(CurrencyType.class);
    private EconomyProvider defaultProvider;

    private EconomyManager() {}

    public static EconomyManager getInstance() {
        if (instance == null) {
            synchronized (EconomyManager.class) {
                if (instance == null) {
                    instance = new EconomyManager();
                }
            }
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        registerProviders();
        resolveDefault();
    }

    private void registerProviders() {
        tryRegister(CurrencyType.VAULT, VaultProvider::new);
        tryRegister(CurrencyType.PLAYER_POINTS, PlayerPointsProvider::new);
    }

    private void tryRegister(CurrencyType type, java.util.function.Supplier<EconomyProvider> factory) {
        try {
            EconomyProvider provider = factory.get();
            if (provider.isAvailable()) {
                providers.put(type, provider);
                DebugAPI.logLibInfo(DebugCategory.ECONOMY, "Registered provider: " + provider.getName());
            }
        } catch (Exception | NoClassDefFoundError e) {
            DebugAPI.logLibDebug(DebugCategory.ECONOMY, type.getDisplayName() + " not available: " + e.getMessage());
        }
    }

    private void resolveDefault() {
        for (CurrencyType type : CurrencyType.values()) {
            EconomyProvider provider = providers.get(type);
            if (provider != null && provider.isAvailable()) {
                defaultProvider = provider;
                DebugAPI.logLibInfo(DebugCategory.ECONOMY, "Default provider: " + provider.getName());
                return;
            }
        }
        defaultProvider = new DummyProvider();
        DebugAPI.logLibWarn(DebugCategory.ECONOMY, "No economy provider found, using dummy");
    }

    public EconomyProvider getProvider() {
        return defaultProvider;
    }

    public EconomyProvider getProvider(CurrencyType type) {
        return providers.getOrDefault(type, defaultProvider);
    }

    public boolean isAvailable() {
        return defaultProvider != null && defaultProvider.isAvailable();
    }

    public boolean isAvailable(CurrencyType type) {
        EconomyProvider provider = providers.get(type);
        return provider != null && provider.isAvailable();
    }

    public BigDecimal getBalance(OfflinePlayer player) {
        return defaultProvider.getBalance(player);
    }

    public BigDecimal getBalance(OfflinePlayer player, CurrencyType type) {
        return getProvider(type).getBalance(player);
    }

    public BigDecimal getBalance(UUID playerId) {
        return defaultProvider.getBalance(playerId);
    }

    public BigDecimal getBalance(UUID playerId, CurrencyType type) {
        return getProvider(type).getBalance(playerId);
    }

    public boolean has(OfflinePlayer player, BigDecimal amount) {
        return defaultProvider.has(player, amount);
    }

    public boolean has(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return getProvider(type).has(player, amount);
    }

    public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount) {
        return defaultProvider.deposit(player, amount);
    }

    public EconomyResponse deposit(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return getProvider(type).deposit(player, amount);
    }

    public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount) {
        return defaultProvider.withdraw(player, amount);
    }

    public EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return getProvider(type).withdraw(player, amount);
    }

    public EconomyResponse set(OfflinePlayer player, BigDecimal amount) {
        return defaultProvider.set(player, amount);
    }

    public EconomyResponse set(OfflinePlayer player, BigDecimal amount, CurrencyType type) {
        return getProvider(type).set(player, amount);
    }

    public TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount) {
        return transfer(from, to, amount, defaultProvider);
    }

    public TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount, CurrencyType type) {
        return transfer(from, to, amount, getProvider(type));
    }

    // TODO(human): Implement transfer logic with validation, withdraw, deposit, and rollback on failure
    private TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount, EconomyProvider provider) {
        return TransferResult.failure("Not implemented");
    }

    public String format(BigDecimal amount) {
        return defaultProvider.format(amount);
    }

    public String format(BigDecimal amount, CurrencyType type) {
        return getProvider(type).format(amount);
    }

    public void reload() {
        providers.clear();
        registerProviders();
        resolveDefault();
    }

    public void shutdown() {
        providers.clear();
        defaultProvider = null;
        plugin = null;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Map<CurrencyType, EconomyProvider> getRegisteredProviders() {
        return Map.copyOf(providers);
    }
}
