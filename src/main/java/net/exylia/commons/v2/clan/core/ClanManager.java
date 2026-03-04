package net.exylia.commons.v2.clan.core;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.clan.cache.ClanCacheManager;
import net.exylia.commons.v2.clan.config.ClanConfig;
import net.exylia.commons.v2.clan.listener.PlayerCacheListener;
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.clan.provider.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClanManager {

    private static ClanManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final JavaPlugin plugin;

    @Getter
    private final ClanCacheManager cacheManager;

    @Getter
    private final ClanDetector detector;

    @Getter
    private final ClanConfig config;

    @Getter
    private ClanProvider activeProvider;

    private ClanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = new ClanConfig();
        this.cacheManager = new ClanCacheManager();
        this.detector = new ClanDetector();
        this.activeProvider = detectProvider();

        registerListeners();

        DebugAPI.logLibInfo("ClanManager initialized with provider: " + activeProvider.getProviderName());
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ClanManager(plugin);
            }
        }
    }

    public static ClanManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ClanManager not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private ClanProvider detectProvider() {
        ClanProvider provider = detector.detectBestProvider();

        if (provider instanceof NoClanProvider) {
            DebugAPI.logLibWarn("No clan plugin detected. Clan functionality will be limited.");
        }

        return provider;
    }

    public Optional<Clan> getPlayerClan(UUID playerId) {
        Optional<Optional<Clan>> cached = cacheManager.getPlayerClanCache().get(playerId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<Clan> clan = activeProvider.getPlayerClan(playerId);
        cacheManager.getPlayerClanCache().put(playerId, clan);
        return clan;
    }

    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        Optional<Optional<Clan>> cached = cacheManager.getPlayerClanCache().get(playerId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        return activeProvider.getPlayerClanAsync(playerId)
                .thenApply(clan -> {
                    cacheManager.getPlayerClanCache().put(playerId, clan);
                    return clan;
                });
    }

    public Optional<Clan> getPlayerClan(Player player) {
        return getPlayerClan(player.getUniqueId());
    }

    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return getPlayerClanAsync(player.getUniqueId());
    }

    public Optional<Clan> getClanByTag(String tag) {
        Optional<Optional<Clan>> cached = cacheManager.getClanDataCache().get(tag);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<Clan> clan = activeProvider.getClanByTag(tag);
        cacheManager.getClanDataCache().put(tag, clan);
        return clan;
    }

    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        Optional<Optional<Clan>> cached = cacheManager.getClanDataCache().get(tag);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        return activeProvider.getClanByTagAsync(tag)
                .thenApply(clan -> {
                    cacheManager.getClanDataCache().put(tag, clan);
                    return clan;
                });
    }

    public Optional<Clan> getClanById(String id) {
        Optional<Optional<Clan>> cached = cacheManager.getClanDataCache().get(id);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<Clan> clan = activeProvider.getClanById(id);
        cacheManager.getClanDataCache().put(id, clan);
        return clan;
    }

    public CompletableFuture<Optional<Clan>> getClanByIdAsync(String id) {
        Optional<Optional<Clan>> cached = cacheManager.getClanDataCache().get(id);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        return activeProvider.getClanByIdAsync(id)
                .thenApply(clan -> {
                    cacheManager.getClanDataCache().put(id, clan);
                    return clan;
                });
    }

    public Collection<Clan> getAllClans() {
        return activeProvider.getAllClans();
    }

    public CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return activeProvider.getAllClansAsync();
    }

    public boolean hasPlayerClan(UUID playerId) {
        return getPlayerClan(playerId).isPresent();
    }

    public boolean hasPlayerClan(Player player) {
        return hasPlayerClan(player.getUniqueId());
    }

    public void reload() {
        DebugAPI.logLibInfo("Reloading ClanManager...");
        cacheManager.invalidateAll();
        activeProvider.invalidateCache();
        activeProvider = detectProvider();
        DebugAPI.logLibInfo("ClanManager reloaded with provider: " + activeProvider.getProviderName());
    }

    public void shutdown() {
        DebugAPI.logLibInfo("Shutting down ClanManager...");
        cacheManager.invalidateAll();

        synchronized (LOCK) {
            instance = null;
        }

        DebugAPI.logLibInfo("ClanManager shutdown complete");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerCacheListener(this), plugin);
    }
}
