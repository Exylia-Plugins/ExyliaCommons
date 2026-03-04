package net.exylia.commons.v2.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerUtils {

    private static Cache<UUID, Set<EnderPearl>> enderPearlCache;

    public static void initialize(JavaPlugin plugin) {
        enderPearlCache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .build();
        plugin.getServer().getPluginManager().registerEvents(new EnderPearlListener(), plugin);
    }

    static void trackPearl(UUID uuid, EnderPearl pearl) {
        enderPearlCache.asMap()
                .computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                .add(pearl);
    }

    static void untrackPearl(UUID uuid, EnderPearl pearl) {
        Set<EnderPearl> pearls = enderPearlCache.getIfPresent(uuid);
        if (pearls != null) {
            pearls.remove(pearl);
        }
    }

    static void clearCache(UUID uuid) {
        enderPearlCache.invalidate(uuid);
    }

    public static void clearPlayerEnderPearls(Player player) {
        Set<EnderPearl> pearls = enderPearlCache.getIfPresent(player.getUniqueId());
        if (pearls == null) return;
        for (EnderPearl pearl : pearls) {
            if (pearl.isValid() && !pearl.isDead()) pearl.remove();
        }
        enderPearlCache.invalidate(player.getUniqueId());
    }
}
