package net.exylia.commons.item.vanilla;

import net.exylia.commons.item.vanilla.events.RegionLimitEvent;
import net.exylia.commons.item.vanilla.events.RegionLimitEventType;
import net.exylia.commons.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VanillaRegionLimitManager implements Listener {

    private static VanillaRegionLimitManager instance;
    private static JavaPlugin plugin;
    private static boolean initialized = false;

    private final Map<Material, VanillaRegionLimitConfig> regionLimits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Map<Material, Integer>>> playerRegionUsage = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerCurrentRegion = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerPreviousRegions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private VanillaRegionLimitManager() {}

    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;

        plugin = javaPlugin;
        instance = new VanillaRegionLimitManager();

        Bukkit.getPluginManager().registerEvents(instance, plugin);
        instance.startRegionTrackingTask();

        initialized = true;
    }

    public static VanillaRegionLimitManager getInstance() {
        if (!initialized) {
            throw new IllegalStateException("VanillaRegionLimitManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public void registerRegionLimit(Material material, int maxUsesPerRegion) {
        regionLimits.put(material, new VanillaRegionLimitConfig(material, maxUsesPerRegion));
    }

    public void registerRegionLimits(Map<Material, Integer> limits) {
        limits.forEach(this::registerRegionLimit);
    }

    public boolean hasRegionLimit(Material material) {
        return regionLimits.containsKey(material);
    }

    public boolean canPlayerUseInRegion(Player player, Material material) {
        if (!hasRegionLimit(material) || !WorldGuardUtils.isWorldGuardAvailable()) {
            return true;
        }

        String currentRegion = getCurrentRegion(player);
        if (currentRegion == null) {
            return true;
        }

        VanillaRegionLimitConfig config = regionLimits.get(material);
        int currentUsage = getCurrentUsage(player, currentRegion, material);

        return currentUsage < config.getMaxUsesPerRegion();
    }

    public void recordUsage(Player player, Material material) {
        if (!hasRegionLimit(material) || !WorldGuardUtils.isWorldGuardAvailable()) {
            return;
        }

        String currentRegion = getCurrentRegion(player);
        if (currentRegion == null) {
            return;
        }

        VanillaRegionLimitConfig config = regionLimits.get(material);
        int newUsage = playerRegionUsage
                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentRegion, k -> new ConcurrentHashMap<>())
                .merge(material, 1, Integer::sum);

        fireRegionLimitEvent(RegionLimitEventType.ITEM_USED, player.getUniqueId(),
                currentRegion, material, newUsage, config.getMaxUsesPerRegion());
    }

    public int getCurrentUsage(Player player, String regionName, Material material) {
        return playerRegionUsage
                .getOrDefault(player.getUniqueId(), Map.of())
                .getOrDefault(regionName, Map.of())
                .getOrDefault(material, 0);
    }

    public int getRemainingUses(Player player, Material material) {
        if (!hasRegionLimit(material)) {
            return -1;
        }

        String currentRegion = getCurrentRegion(player);
        if (currentRegion == null) {
            return -1;
        }

        VanillaRegionLimitConfig config = regionLimits.get(material);
        int currentUsage = getCurrentUsage(player, currentRegion, material);

        return Math.max(0, config.getMaxUsesPerRegion() - currentUsage);
    }

    public String getCurrentRegion(Player player) {
        if (!WorldGuardUtils.isWorldGuardAvailable()) {
            return null;
        }

        List<String> regions = WorldGuardUtils.getRegionsAtPlayer(player);
        return regions.isEmpty() ? null : WorldGuardUtils.getHighestPriorityRegion(player);
    }

    private void startRegionTrackingTask() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!WorldGuardUtils.isWorldGuardAvailable()) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPlayerRegionChange(player);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void checkPlayerRegionChange(Player player) {
        UUID playerId = player.getUniqueId();
        String previousRegion = playerCurrentRegion.get(playerId);
        String currentRegion = getCurrentRegion(player);

        if (Objects.equals(previousRegion, currentRegion)) {
            return;
        }

        Set<String> previousRegions = playerPreviousRegions.getOrDefault(playerId, new HashSet<>());

        if (currentRegion != null) {
            playerCurrentRegion.put(playerId, currentRegion);
            playerPreviousRegions.put(playerId, new HashSet<>(Set.of(currentRegion)));

            fireRegionLimitEvent(RegionLimitEventType.REGION_ENTER, playerId,
                    currentRegion, null, 0, 0);
        } else {
            playerCurrentRegion.remove(playerId);
            playerPreviousRegions.put(playerId, new HashSet<>());

            if (previousRegion != null) {
                fireRegionLimitEvent(RegionLimitEventType.REGION_EXIT, playerId,
                        previousRegion, null, 0, 0);
            }
        }

        if (previousRegion != null && !previousRegions.contains(currentRegion)) {
            Bukkit.getScheduler().runTask(plugin, () -> resetPlayerRegionUsage(player, previousRegion));
        }
    }

    private void resetPlayerRegionUsage(Player player, String regionName) {
        Map<String, Map<Material, Integer>> playerUsage = playerRegionUsage.get(player.getUniqueId());
        if (playerUsage != null) {
            playerUsage.remove(regionName);
            fireRegionLimitEvent(RegionLimitEventType.USAGE_RESET, player.getUniqueId(),
                    regionName, null, 0, 0);
        }
    }

    private void fireRegionLimitEvent(RegionLimitEventType eventType, UUID playerId,
                                      String regionName, Material material, int currentUsage, int maxUsage) {
        RegionLimitEvent event = new RegionLimitEvent(eventType, playerId, regionName, material, currentUsage, maxUsage);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.getPluginManager().callEvent(event);
        });
    }

    public void removePlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        playerRegionUsage.remove(playerId);
        playerCurrentRegion.remove(playerId);
        playerPreviousRegions.remove(playerId);
    }

    public void clearAllData() {
        playerRegionUsage.clear();
        playerCurrentRegion.clear();
        playerPreviousRegions.clear();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Map<Material, VanillaRegionLimitConfig> getAllRegionLimits() {
        return new ConcurrentHashMap<>(regionLimits);
    }

    public Map<String, RegionLimitInfo> getActiveRegionLimitsForPlayer(Player player) {
        Map<String, RegionLimitInfo> limits = new ConcurrentHashMap<>();

        String currentRegion = getCurrentRegion(player);
        if (currentRegion == null) {
            return limits;
        }

        for (var entry : regionLimits.entrySet()) {
            Material material = entry.getKey();
            int currentUsage = getCurrentUsage(player, currentRegion, material);

            if (currentUsage >= 1) {
                String itemId = "vanilla_" + material.name().toLowerCase();
                String displayName = VanillaItemCooldownManager.getInstance().getEffectiveDisplayName(material);
                int maxUses = entry.getValue().getMaxUsesPerRegion();

                limits.put(itemId, RegionLimitInfo.builder()
                        .itemId(itemId)
                        .displayName(displayName)
                        .current(currentUsage)
                        .max(maxUses)
                        .build());
            }
        }

        return limits;
    }

    public boolean hasActiveRegionLimits(Player player) {
        String currentRegion = getCurrentRegion(player);
        if (currentRegion == null) {
            return false;
        }

        return regionLimits.entrySet().stream()
                .anyMatch(entry -> getCurrentUsage(player, currentRegion, entry.getKey()) >= 1);
    }

    public static class VanillaRegionLimitConfig {
        private final Material material;
        private final int maxUsesPerRegion;

        public VanillaRegionLimitConfig(Material material, int maxUsesPerRegion) {
            this.material = material;
            this.maxUsesPerRegion = maxUsesPerRegion;
        }

        public Material getMaterial() {
            return material;
        }

        public int getMaxUsesPerRegion() {
            return maxUsesPerRegion;
        }
    }
}