package net.exylia.commons.v2.cooldown.api;

import net.exylia.commons.v2.cooldown.core.ItemCooldownListener;
import net.exylia.commons.v2.cooldown.core.ItemCooldownManager;
import net.exylia.commons.v2.cooldown.core.ItemCooldownRegistry;
import net.exylia.commons.v2.cooldown.model.ItemCooldown;
import net.exylia.commons.v2.cooldown.model.ItemCooldownDefinition;
import net.exylia.commons.v2.cooldown.model.RegionLimitInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public final class ItemCooldownAPI {

    private static ItemCooldownManager manager;
    private static ItemCooldownRegistry registry;
    private static ItemCooldownListener listener;

    private ItemCooldownAPI() {}

    public static void initialize(ItemCooldownManager itemCooldownManager, ItemCooldownRegistry itemCooldownRegistry, ItemCooldownListener itemCooldownListener) {
        manager = itemCooldownManager;
        registry = itemCooldownRegistry;
        listener = itemCooldownListener;
    }

    public static void register(ItemCooldownDefinition definition) {
        registry.register(definition);
    }

    public static void unregister(String id) {
        registry.unregister(id);
    }

    public static ItemCooldownDefinition getDefinition(String id) {
        return registry.getById(id);
    }

    public static Collection<ItemCooldownDefinition> getDefinitions() {
        return registry.getAll();
    }

    public static void set(Player player, String id, long durationMs) {
        manager.set(player, id, durationMs);
    }

    public static void set(Player player, String id, long durationMs, Material material) {
        manager.set(player, id, durationMs, material);
    }

    public static void set(Collection<? extends Player> players, String id, long durationMs) {
        for (Player player : players) {
            manager.set(player, id, durationMs);
        }
    }

    public static void set(Collection<? extends Player> players, String id, long durationMs, Material material) {
        for (Player player : players) {
            manager.set(player, id, durationMs, material);
        }
    }

    public static boolean isOnCooldown(Player player, String id) {
        return manager.isOnCooldown(player, id);
    }

    public static boolean isOnCooldown(UUID playerId, String id) {
        return manager.isOnCooldown(playerId, id);
    }

    public static long getRemainingMillis(Player player, String id) {
        return manager.getRemainingMillis(player, id);
    }

    public static long getRemainingMillis(UUID playerId, String id) {
        return manager.getRemainingMillis(playerId, id);
    }

    public static String getFormattedRemaining(Player player, String id) {
        return manager.getFormattedRemaining(player, id);
    }

    public static ItemCooldown get(Player player, String id) {
        return manager.get(player, id);
    }

    public static ItemCooldown get(UUID playerId, String id) {
        return manager.get(playerId, id);
    }

    public static Map<String, ItemCooldown> getActive(Player player) {
        return manager.getActive(player);
    }

    public static Map<String, ItemCooldown> getActive(UUID playerId) {
        return manager.getActive(playerId);
    }

    public static List<RegionLimitInfo> getActiveRegionLimits(Player player) {
        Set<String> regionIds = listener.getPlayerRegionIds(player);
        if (regionIds.isEmpty()) return Collections.emptyList();

        UUID playerId = player.getUniqueId();
        List<RegionLimitInfo> limits = new ArrayList<>();

        for (ItemCooldownDefinition def : registry.getAll()) {
            if (!def.hasAnyRegionMaxUses()) continue;
            for (String regionId : regionIds) {
                if (!def.hasRegionMaxUses(regionId)) continue;
                int uses = listener.getRegionUseCount(playerId, regionId, def.getId());
                if (uses > 0) {
                    String name = def.getDisplayName() != null ? def.getDisplayName() : def.getMaterial().name();
                    limits.add(new RegionLimitInfo(name, uses, def.getRegionMaxUses(regionId)));
                    break;
                }
            }
        }

        return limits;
    }

    public static void add(Player player, String id, long durationMs) {
        manager.add(player, id, durationMs);
    }

    public static void decrease(Player player, String id, long durationMs) {
        manager.decrease(player, id, durationMs);
    }

    public static void remove(Player player, String id) {
        manager.remove(player, id);
    }

    public static void remove(Collection<? extends Player> players, String id) {
        for (Player player : players) {
            manager.remove(player, id);
        }
    }

    public static void removeAll(Player player) {
        manager.removeAll(player);
    }

    public static void removeAll(Collection<? extends Player> players) {
        for (Player player : players) {
            manager.removeAll(player);
        }
    }

    public static boolean isInitialized() {
        return manager != null;
    }
}
