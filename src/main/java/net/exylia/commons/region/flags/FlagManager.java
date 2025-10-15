package net.exylia.commons.region.flags;

import lombok.Getter;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.region.model.RegionFlagType;
import net.exylia.commons.region.RegionManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class FlagManager {
    private static FlagManager instance;

    private final JavaPlugin plugin;
    private final Map<UUID, FlagState> playerStates;  
    private final Map<UUID, BukkitRunnable> activeEffects;  

    private final OptimizedFlagCache optimizedCache;

    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong validationCalls = new AtomicLong();

    private FlagManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        this.activeEffects = new ConcurrentHashMap<>();
        this.optimizedCache = new OptimizedFlagCache();

        startOptimizedCleanupTask();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new FlagManager(plugin);
        }
    }

    public static FlagManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FlagManager no ha sido inicializado");
        }
        return instance;
    }

    public boolean canPlayerPerformAction(Player player, RegionFlag flag) {
        return canPlayerPerformActionAt(player, player.getLocation(), flag);
    }

    public boolean canPlayerPerformActionAt(Player player, Location location, RegionFlag flag) {
        validationCalls.incrementAndGet();

        if (location == null || flag == null) {
            throw new IllegalArgumentException("Location y flag no pueden ser null");
        }

        if (player != null) {
            Boolean cached = optimizedCache.getCachedResult(player,
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), flag);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
        }

        cacheMisses.incrementAndGet();

        if (player != null && hasAdminPermission(player, flag)) {
            cacheResult(player, location, flag, true);
            return true;
        }

        List<Region> regions = RegionManager.getInstance().getRegionsAt(location);

        boolean result;
        if (regions.isEmpty()) {
             
            result = flag.isDefaultValue();
        } else {
             
            Region highestPriorityRegion = regions.get(0);  
            result = evaluateFlagInRegionOptimized(player, highestPriorityRegion, flag, location);
        }

        cacheResult(player, location, flag, result);
        return result;
    }

    private boolean evaluateFlagInRegionOptimized(Player player, Region region, RegionFlag flag, Location actionLocation) {
         
        if (player != null && flag == RegionFlag.REGION_MEMBERS_ONLY && region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            if (flag.isAffectedByRegionMembersOnly()) {
                boolean playerInRegion = region.contains(player.getLocation());
                if (!playerInRegion) {
                    return false;  
                }
            }
        }

        if (player != null) {
            UUID playerId = player.getUniqueId();

            if (region.isOwner(playerId)) {
                RegionFlagType flagType = region.getFlagType(flag);
                return flagType != RegionFlagType.DENY;
            }

            if (region.isMember(playerId)) {
                if (flag.affectsBuilding() || flag == RegionFlag.INTERACT) {
                    RegionFlagType flagType = region.getFlagType(flag);
                    return flagType != RegionFlagType.DENY;
                }
            }

            String flagPermission = flag.getRequiredPermission(region.getId());
            if (player.hasPermission(flagPermission)) {
                return true;
            }
        }

        boolean flagValue = region.getFlagValue(flag);

        if (!flag.requiresSpecialPermission()) {
            return flagValue;
        }

        if (hasIncompatibleFlags(region, flag)) {
            return false;
        }

        return flagValue;
    }

    public boolean isPvpAllowed(Player attacker, Player target) {
         
        if (isPlayerInvincible(target)) {
            return false;
        }

        String pvpCacheKey = getPvpCacheKey(attacker, target);
        Boolean cachedPvp = optimizedCache.getCachedPvpResult(pvpCacheKey);
        if (cachedPvp != null) {
            return cachedPvp;
        }

        Location attackerLoc = attacker.getLocation();
        Location targetLoc = target.getLocation();

        List<Region> attackerRegions = RegionManager.getInstance().getRegionsAt(attackerLoc);
        List<Region> targetRegions = RegionManager.getInstance().getRegionsAt(targetLoc);

        if (attackerRegions.isEmpty() && targetRegions.isEmpty()) {
            boolean result = RegionFlag.PVP.isDefaultValue();
            optimizedCache.cachePvpResult(pvpCacheKey, result);
            return result;
        }

        boolean pvpAllowed = true;

        if (!attackerRegions.isEmpty()) {
            Region attackerRegion = attackerRegions.get(0);
            if (!checkPvpInRegion(attackerRegion, attacker, target, attackerLoc, targetLoc)) {
                pvpAllowed = false;
            }
        }

        if (pvpAllowed && !targetRegions.isEmpty()) {
            Region targetRegion = targetRegions.get(0);
            if (!attackerRegions.contains(targetRegion)) {  
                if (!checkPvpInRegion(targetRegion, attacker, target, attackerLoc, targetLoc)) {
                    pvpAllowed = false;
                }
            }
        }

        optimizedCache.cachePvpResult(pvpCacheKey, pvpAllowed);
        return pvpAllowed;
    }

    private boolean checkPvpInRegion(Region region, Player attacker, Player target, Location attackerLoc, Location targetLoc) {
         
        if (region.getFlagValue(RegionFlag.REGION_MEMBERS_ONLY)) {
            boolean attackerInRegion = region.contains(attackerLoc);
            boolean targetInRegion = region.contains(targetLoc);

            if (!attackerInRegion || !targetInRegion) {
                return false;
            }
        }

        return region.getFlagValue(RegionFlag.PVP);
    }

    private String getPvpCacheKey(Player attacker, Player target) {
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (attackerId.compareTo(targetId) < 0) {
            return attackerId + ":" + targetId;
        } else {
            return targetId + ":" + attackerId;
        }
    }

    private boolean hasIncompatibleFlags(Region region, RegionFlag flag) {
         
        String incompatibleKey = region.getId() + ":" + flag.name();
        Boolean cachedIncompatible = optimizedCache.getCachedIncompatibility(incompatibleKey);
        if (cachedIncompatible != null) {
            return cachedIncompatible;
        }

        boolean hasIncompatible = region.getConfiguredFlags().keySet().stream()
                .anyMatch(configuredFlag -> {
                     
                    if (flag.isIncompatibleWith(configuredFlag) && region.getFlagValue(configuredFlag)) {
                        return true;
                    }

                    if (flag == RegionFlag.RE_GIVE_BLOCKS &&
                            configuredFlag == RegionFlag.TEMPORARY_BLOCKS &&
                            !region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
                        return true;  
                    }

                    return false;
                });

        optimizedCache.cacheIncompatibility(incompatibleKey, hasIncompatible);
        return hasIncompatible;
    }

    private boolean hasAdminPermission(Player player, RegionFlag flag) {
        String adminCacheKey = player.getUniqueId() + ":admin:" + flag.name();
        Boolean cachedAdmin = optimizedCache.getCachedAdminPermission(adminCacheKey);
        if (cachedAdmin != null) {
            return cachedAdmin;
        }

        boolean hasAdmin = player.hasPermission("exylia.region.admin") ||
                player.hasPermission("exylia.region.flag.admin") ||
                player.hasPermission("exylia.region.flag." + flag.getKey().replace("-", "_") + ".admin");

        optimizedCache.cacheAdminPermission(adminCacheKey, hasAdmin);
        return hasAdmin;
    }

    private void cacheResult(Player player, Location location, RegionFlag flag, boolean result) {
        if (player != null) {
            optimizedCache.cacheResult(player, location.getBlockX(), location.getBlockY(),
                    location.getBlockZ(), flag, result);
        }
    }

    public void applyRegionEffects(Player player, Region region) {
        FlagState state = getOrCreatePlayerState(player);

        for (Map.Entry<RegionFlag, RegionFlagType> entry : region.getConfiguredFlags().entrySet()) {
            RegionFlag flag = entry.getKey();
            RegionFlagType type = entry.getValue();

            if (type == RegionFlagType.ALLOW || (type == RegionFlagType.DEFAULT && flag.isDefaultValue())) {
                applyFlagEffect(player, region, flag, state);
            }
        }

        state.setActiveRegion(region);
    }

    public void removeRegionEffects(Player player, Region region) {
        FlagState state = playerStates.get(player.getUniqueId());
        if (state == null) return;

        for (RegionFlag flag : region.getConfiguredFlags().keySet()) {
            removeFlagEffect(player, region, flag, state);
        }

        if (region.equals(state.getActiveRegion())) {
            state.setActiveRegion(null);
        }
    }

    private void applyFlagEffect(Player player, Region region, RegionFlag flag, FlagState state) {
        switch (flag) {
            case FLIGHT:
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    state.addAppliedFlag(flag);
                }
                break;

            case INVINCIBLE:
                state.addAppliedFlag(flag);
                break;

            case HEAL:
                startHealEffect(player, region);
                state.addAppliedFlag(flag);
                break;

            case FEED:
                startFeedEffect(player, region);
                state.addAppliedFlag(flag);
                break;

            case FORCE_ADVENTURE:
                if (player.getGameMode() != GameMode.ADVENTURE) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.ADVENTURE);
                    state.addAppliedFlag(flag);
                }
                break;

            case FORCE_SURVIVAL:
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.SURVIVAL);
                    state.addAppliedFlag(flag);
                }
                break;

            case FORCE_CREATIVE:
                if (player.getGameMode() != GameMode.CREATIVE) {
                    state.setPreviousGameMode(player.getGameMode());
                    player.setGameMode(GameMode.CREATIVE);
                    state.addAppliedFlag(flag);
                }
                break;

            case REGION_MEMBERS_ONLY:
                state.addAppliedFlag(flag);
                break;
        }
    }

    private void removeFlagEffect(Player player, Region region, RegionFlag flag, FlagState state) {
        if (!state.hasAppliedFlag(flag)) return;

        switch (flag) {
            case FLIGHT:
                if (!player.hasPermission("exylia.flight") &&
                        player.getGameMode() != GameMode.CREATIVE &&
                        player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
                break;

            case INVINCIBLE:
                break;

            case HEAL:
                stopEffect(player, "heal");
                break;

            case FEED:
                stopEffect(player, "feed");
                break;

            case FORCE_ADVENTURE:
            case FORCE_SURVIVAL:
            case FORCE_CREATIVE:
                GameMode previous = state.getPreviousGameMode();
                if (previous != null) {
                    player.setGameMode(previous);
                }
                break;

            case REGION_MEMBERS_ONLY:
                break;
        }

        state.removeAppliedFlag(flag);
    }

    private void startHealEffect(Player player, Region region) {
        String effectKey = "heal";
        stopEffect(player, effectKey);

        BukkitRunnable healTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !region.contains(player)) {
                    this.cancel();
                    return;
                }

                double health = player.getHealth();
                double maxHealth = player.getMaxHealth();

                if (health < maxHealth) {
                    player.setHealth(Math.min(maxHealth, health + 1.0));
                }
            }
        };

        healTask.runTaskTimer(plugin, 0L, 40L);
        activeEffects.put(getEffectKey(player, effectKey), healTask);
    }

    private void startFeedEffect(Player player, Region region) {
        String effectKey = "feed";
        stopEffect(player, effectKey);

        BukkitRunnable feedTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !region.contains(player)) {
                    this.cancel();
                    return;
                }

                if (player.getFoodLevel() < 20) {
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);
                }
            }
        };

        feedTask.runTaskTimer(plugin, 0L, 100L);
        activeEffects.put(getEffectKey(player, effectKey), feedTask);
    }

    private void stopEffect(Player player, String effectName) {
        UUID effectKey = getEffectKey(player, effectName);
        BukkitRunnable task = activeEffects.remove(effectKey);
        if (task != null) {
            task.cancel();
        }
    }

    private UUID getEffectKey(Player player, String effectName) {
        return UUID.nameUUIDFromBytes((player.getUniqueId().toString() + ":" + effectName).getBytes());
    }

    private FlagState getOrCreatePlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(),
                k -> new FlagState(player.getUniqueId()));
    }

    public void cleanupPlayerState(Player player) {
        FlagState state = playerStates.remove(player.getUniqueId());
        if (state != null) {
            for (RegionFlag flag : state.getAppliedFlags()) {
                removeFlagEffect(player, state.getActiveRegion(), flag, state);
            }
        }

        activeEffects.entrySet().removeIf(entry -> {
            if (entry.getKey().toString().startsWith(player.getUniqueId().toString())) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });

        optimizedCache.invalidatePlayer(player);
    }

    public void invalidateFlagCache(RegionFlag flag) {
        optimizedCache.invalidateFlag(flag);
    }

    public void invalidatePlayerCache(Player player) {
        optimizedCache.invalidatePlayer(player);
    }

    public void invalidateRegionCache(Region region) {
        optimizedCache.invalidateRegion(region);
    }

    public void invalidateRegionFlagCache(Region region, RegionFlag flag) {
        optimizedCache.invalidateRegionFlag(region, flag);
    }

    public boolean isPlayerInvincible(Player player) {
        FlagState state = playerStates.get(player.getUniqueId());
        return state != null && state.hasAppliedFlag(RegionFlag.INVINCIBLE);
    }

    private void startOptimizedCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                optimizedCache.performMaintenance();
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 100L);  
    }

    public FlagManagerStats getStats() {
        return new FlagManagerStats(
                playerStates.size(),
                activeEffects.size(),
                cacheHits.get(),
                cacheMisses.get(),
                validationCalls.get(),
                optimizedCache.getStats()
        );
    }

    public void resetStats() {
        cacheHits.set(0);
        cacheMisses.set(0);
        validationCalls.set(0);
    }

    @Getter
    public static class FlagManagerStats {
        private final int activePlayerStates;
        private final int activeEffects;
        private final long cacheHits;
        private final long cacheMisses;
        private final long validationCalls;
        private final OptimizedFlagCache.CacheStats cacheStats;

        public FlagManagerStats(int activePlayerStates, int activeEffects, long cacheHits,
                                long cacheMisses, long validationCalls, OptimizedFlagCache.CacheStats cacheStats) {
            this.activePlayerStates = activePlayerStates;
            this.activeEffects = activeEffects;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.validationCalls = validationCalls;
            this.cacheStats = cacheStats;
        }

        public double getCacheHitRatio() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "FlagManagerStats{states=%d, effects=%d, cache_hits=%d, cache_misses=%d, hit_ratio=%.2f%%, validations=%d}",
                    activePlayerStates, activeEffects, cacheHits, cacheMisses,
                    getCacheHitRatio() * 100, validationCalls
            );
        }
    }

    private boolean validateFlagConfiguration(Region region, RegionFlag flag, boolean newValue) {
         
        if (flag == RegionFlag.RE_GIVE_BLOCKS && newValue) {
             
            if (!region.getFlagValue(RegionFlag.TEMPORARY_BLOCKS)) {
                return false;
            }
        }

        if (flag == RegionFlag.TEMPORARY_BLOCKS && !newValue) {
             
            if (region.getFlagValue(RegionFlag.RE_GIVE_BLOCKS)) {
                region.setFlag(RegionFlag.RE_GIVE_BLOCKS, RegionFlagType.DEFAULT);
                logInternalDebug(String.format(
                        "RE_GIVE_BLOCKS desactivado automáticamente en región %s al desactivar TEMPORARY_BLOCKS",
                        region.getId()
                ));
            }
        }

        return true;
    }
}
