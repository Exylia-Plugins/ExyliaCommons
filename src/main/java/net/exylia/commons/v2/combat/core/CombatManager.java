package net.exylia.commons.v2.combat.core;

import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.combat.cache.CombatDataCache;
import net.exylia.commons.v2.combat.config.CombatConfig;
import net.exylia.commons.v2.combat.listener.PlayerCacheListener;
import net.exylia.commons.v2.combat.model.CombatData;
import net.exylia.commons.v2.combat.provider.CombatProvider;
import net.exylia.commons.v2.combat.provider.NoCombatProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CombatManager {

    private static CombatManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final JavaPlugin plugin;

    @Getter
    private final CombatDataCache combatDataCache;

    @Getter
    private final CombatDetector detector;

    @Getter
    private final CombatConfig config;

    @Getter
    private CombatProvider activeProvider;

    private CombatManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = new CombatConfig();
        this.combatDataCache = new CombatDataCache();
        this.detector = new CombatDetector();
        this.activeProvider = detectProvider();

        registerListeners();

        DebugUtils.logInternalInfo("CombatManager initialized with provider: " + activeProvider.getProviderName());
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new CombatManager(plugin);
            }
        }
    }

    public static CombatManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CombatManager not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private CombatProvider detectProvider() {
        CombatProvider provider = detector.detectBestProvider();

        if (provider instanceof NoCombatProvider) {
            DebugUtils.logInternalWarn("No combat plugin detected. Combat functionality will be limited.");
        }

        return provider;
    }

    public boolean isInCombat(Player player) {
        return activeProvider.isInCombat(player);
    }

    public int getRemainingCombatTime(Player player) {
        return activeProvider.getRemainingCombatTime(player);
    }

    public long getRemainingCombatTimeMillis(Player player) {
        return activeProvider.getRemainingCombatTimeMillis(player);
    }

    public Optional<Player> getCurrentOpponent(Player player) {
        return activeProvider.getCurrentOpponent(player);
    }

    public void tag(Player target, Player attacker) {
        activeProvider.tag(target, attacker);
    }

    public void tag(Player target, Player attacker, int seconds) {
        activeProvider.tag(target, attacker, seconds);
    }

    public void untag(Player player) {
        activeProvider.untag(player);
    }

    public boolean hasProtection(Player player) {
        return activeProvider.hasProtection(player);
    }

    public boolean hasPvPEnabled(Player player) {
        return activeProvider.hasPvPEnabled(player);
    }

    public void togglePvP(Player player, boolean enabled) {
        activeProvider.togglePvP(player, enabled);
    }

    public boolean canAttack(Player attacker, Player defender) {
        return activeProvider.canAttack(attacker, defender);
    }

    public Optional<CombatData> getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player);
    }

    public Optional<CombatData> getPlayerData(UUID playerId) {
        return getPlayerData(playerId, null);
    }

    private Optional<CombatData> getPlayerData(UUID playerId, Player player) {
        Optional<Optional<CombatData>> cached = combatDataCache.get(playerId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<CombatData> data = player != null
                ? activeProvider.getPlayerData(player)
                : activeProvider.getPlayerData(playerId);
        combatDataCache.put(playerId, data);
        return data;
    }

    public CompletableFuture<Optional<CombatData>> getPlayerDataAsync(Player player) {
        return getPlayerDataAsync(player.getUniqueId(), player);
    }

    public CompletableFuture<Optional<CombatData>> getPlayerDataAsync(UUID playerId) {
        return getPlayerDataAsync(playerId, null);
    }

    private CompletableFuture<Optional<CombatData>> getPlayerDataAsync(UUID playerId, Player player) {
        Optional<Optional<CombatData>> cached = combatDataCache.get(playerId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        CompletableFuture<Optional<CombatData>> future = player != null
                ? activeProvider.getPlayerDataAsync(player)
                : activeProvider.getPlayerDataAsync(playerId);

        return future.thenApply(data -> {
            combatDataCache.put(playerId, data);
            return data;
        });
    }

    public void reload() {
        DebugUtils.logInternalInfo("Reloading CombatManager...");
        combatDataCache.invalidateAll();
        activeProvider.invalidateCache();
        activeProvider = detectProvider();
        DebugUtils.logInternalSuccess("CombatManager reloaded with provider: " + activeProvider.getProviderName());
    }

    public void shutdown() {
        DebugUtils.logInternalInfo("Shutting down CombatManager...");
        combatDataCache.invalidateAll();

        synchronized (LOCK) {
            instance = null;
        }

        DebugUtils.logInternalInfo("CombatManager shutdown complete");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerCacheListener(this), plugin);
    }
}
