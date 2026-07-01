package net.exylia.commons.v2.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class PlayerUtils {

    private static JavaPlugin plugin;
    private static Cache<UUID, Set<EnderPearl>> enderPearlCache;
    private static final Map<UUID, Set<UUID>> hiddenFromViewer = new ConcurrentHashMap<>();
    private static boolean packetEventsAvailable;
    private static Method getEnderPearlsMethod;
    @Setter
    private static BiFunction<Player, Player, Component> tabDisplayProvider;

    public static void initialize(JavaPlugin instance) {
        plugin = instance;
        packetEventsAvailable = plugin.getServer().getPluginManager().isPluginEnabled("packetevents");
        enderPearlCache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .build();
        try {
            getEnderPearlsMethod = Player.class.getMethod("getEnderPearls");
        } catch (NoSuchMethodException ignored) {
            getEnderPearlsMethod = null;
        }
        plugin.getServer().getPluginManager().registerEvents(new EnderPearlListener(), plugin);
        if (instance.getServer().getPluginManager().isPluginEnabled("TAB")) {
            PlayerUtils.setTabDisplayProvider((target, viewer) -> {
                me.neznamy.tab.api.TabAPI tabAPI = me.neznamy.tab.api.TabAPI.getInstance();
                me.neznamy.tab.api.TabPlayer tabTarget = tabAPI.getPlayer(target.getUniqueId());
                if (tabTarget == null) return null;
                me.neznamy.tab.api.tablist.TabListFormatManager fm = tabAPI.getTabListFormatManager();
                if (fm == null) return null;
                String prefix = fm.getOriginalReplacedPrefix(tabTarget);
                String name = fm.getOriginalReplacedName(tabTarget);
                String suffix = fm.getOriginalReplacedSuffix(tabTarget);
                return LegacyComponentSerializer.legacySection().deserialize(prefix + name + suffix);
            });
        }
    }

    static void trackPearl(UUID uuid, EnderPearl pearl) {
        enderPearlCache.asMap()
                .computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                .add(pearl);
    }

    static void untrackPearl(UUID uuid, EnderPearl pearl) {
        Set<EnderPearl> pearls = enderPearlCache.getIfPresent(uuid);
        if (pearls != null) pearls.remove(pearl);
    }

    static void clearPlayerSession(UUID uuid) {
        enderPearlCache.invalidate(uuid);
        hiddenFromViewer.remove(uuid);
        if (packetEventsAvailable) {
            hiddenFromViewer.forEach((viewerUuid, hidden) -> {
                if (hidden.remove(uuid)) {
                    Player viewer = plugin.getServer().getPlayer(viewerUuid);
                    if (viewer != null) PlayerTabPacketHelper.sendTabRemovePacket(uuid, viewer);
                }
            });
        } else {
            hiddenFromViewer.values().forEach(set -> set.remove(uuid));
        }
    }

    @SuppressWarnings("unchecked")
    public static void clearPlayerEnderPearls(Player player) {
        int nativeCount = 0;
        if (getEnderPearlsMethod != null) {
            try {
                Collection<EnderPearl> pearls = (Collection<EnderPearl>) getEnderPearlsMethod.invoke(player);
                nativeCount = pearls.size();
                pearls.forEach(EnderPearl::remove);
            } catch (Exception ignored) {}
        }
        int cacheCount = 0;
        Set<EnderPearl> cached = enderPearlCache.getIfPresent(player.getUniqueId());
        if (cached != null) {
            for (EnderPearl pearl : cached) {
                if (!pearl.isDead()) {
                    if (!pearl.getLocation().getChunk().isLoaded()) {
                        pearl.getLocation().getChunk().load();
                    }
                    pearl.remove();
                    cacheCount++;
                }
            }
            enderPearlCache.invalidate(player.getUniqueId());
        }
        DebugAPI.logLibDebug(DebugCategory.GENERAL, "Cleared " + (nativeCount + cacheCount) + " ender pearl(s) for " + player.getName()
                + " (native=" + nativeCount + ", cache=" + cacheCount + ")");
    }

    public static void hidePlayer(Player viewer, Player target) {
        viewer.hidePlayer(plugin, target);
        if (packetEventsAvailable) {
            Component displayName = tabDisplayProvider != null ? tabDisplayProvider.apply(target, viewer) : null;
            PlayerTabPacketHelper.sendTabAddPacket(target, viewer, displayName);
        }
        hiddenFromViewer
                .computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(target.getUniqueId());
    }

    public static void showPlayer(Player viewer, Player target) {
        viewer.showPlayer(plugin, target);
        Set<UUID> hidden = hiddenFromViewer.get(viewer.getUniqueId());
        if (hidden != null) hidden.remove(target.getUniqueId());
    }

    public static void showAllPlayers(Player viewer) {
        Set<UUID> hidden = hiddenFromViewer.remove(viewer.getUniqueId());
        if (hidden == null) return;
        for (UUID targetId : hidden) {
            Player target = plugin.getServer().getPlayer(targetId);
            if (target != null) viewer.showPlayer(plugin, target);
        }
    }

    public static boolean isHiddenFrom(Player viewer, Player target) {
        Set<UUID> hidden = hiddenFromViewer.get(viewer.getUniqueId());
        return hidden != null && hidden.contains(target.getUniqueId());
    }

}
