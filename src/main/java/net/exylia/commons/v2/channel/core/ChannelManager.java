package net.exylia.commons.v2.channel.core;

import lombok.Getter;
import net.exylia.commons.v2.channel.adapter.ChannelMessenger;
import net.exylia.commons.v2.channel.adapter.LocalChannelMessenger;
import net.exylia.commons.v2.channel.adapter.RedisChannelMessenger;
import net.exylia.commons.v2.channel.cache.PermissionCache;
import net.exylia.commons.v2.channel.listener.ChannelChatListener;
import net.exylia.commons.v2.channel.listener.PlayerCleanupListener;
import net.exylia.commons.v2.redis.SimpleRedis;
import org.bukkit.plugin.Plugin;

@Getter
public class ChannelManager {

    private static volatile ChannelManager instance;
    private static final Object LOCK = new Object();

    private final Plugin plugin;
    private final ChannelRegistry registry;
    private final WriteModeTracker writeModeTracker;
    private final ChannelMessenger messenger;
    private final PermissionCache permissionCache;
    private final CooldownManager cooldownManager;
    private boolean initialized;

    private ChannelManager(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new ChannelRegistry();
        this.writeModeTracker = new WriteModeTracker();
        this.permissionCache = new PermissionCache();
        this.cooldownManager = new CooldownManager();

        if (SimpleRedis.getInstance() != null && SimpleRedis.getInstance().isConnected()) {
            this.messenger = new RedisChannelMessenger(plugin, registry, permissionCache, cooldownManager, SimpleRedis.getInstance());
        } else {
            this.messenger = new LocalChannelMessenger(registry, permissionCache, cooldownManager);
        }

        this.initialized = false;
    }

    public static ChannelManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ChannelManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static void initialize(Plugin plugin) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ChannelManager(plugin);
                    instance.init();
                }
            }
        }
    }

    private void init() {
        messenger.initialize();
        registerListeners();
        initialized = true;
    }

    private void registerListeners() {
        ChannelChatListener chatListener = new ChannelChatListener(this);
        PlayerCleanupListener cleanupListener = new PlayerCleanupListener(this);

        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(cleanupListener, plugin);
    }

    public void shutdown() {
        messenger.shutdown();
        registry.clear();
        writeModeTracker.clearAll();
        permissionCache.invalidateAll();
        cooldownManager.clearAll();
        initialized = false;
    }
}
