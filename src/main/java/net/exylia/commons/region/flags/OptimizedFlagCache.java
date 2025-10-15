package net.exylia.commons.region.flags;

import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OptimizedFlagCache {
     
    private static final int MAX_CACHE_SIZE = 8000;  
    private static final long CACHE_EXPIRE_MS = 4000;  
    private static final int CLEANUP_THRESHOLD = 100;  

    private final ConcurrentHashMap<Long, CacheEntry> flagValidationCache;
    private final ConcurrentHashMap<String, TimedCacheEntry> pvpCache;
    private final ConcurrentHashMap<String, TimedCacheEntry> adminPermissionCache;
    private final ConcurrentHashMap<String, TimedCacheEntry> incompatibilityCache;

    private final AtomicLong accessCounter;
    private volatile long lastMaintenanceTime;

    public OptimizedFlagCache() {
        this.flagValidationCache = new ConcurrentHashMap<>();
        this.pvpCache = new ConcurrentHashMap<>();
        this.adminPermissionCache = new ConcurrentHashMap<>();
        this.incompatibilityCache = new ConcurrentHashMap<>();
        this.accessCounter = new AtomicLong();
        this.lastMaintenanceTime = System.currentTimeMillis();
    }

    public Boolean getCachedResult(Player player, int blockX, int blockY, int blockZ, RegionFlag flag) {
        long key = generateFlagKey(player, blockX, blockY, blockZ, flag);
        CacheEntry entry = flagValidationCache.get(key);

        if (entry != null && !entry.isExpired()) {
            return entry.result;
        }

        if (entry != null) {
            flagValidationCache.remove(key);
        }

        return null;
    }

    public void cacheResult(Player player, int blockX, int blockY, int blockZ, RegionFlag flag, boolean result) {
        triggerMaintenanceIfNeeded();

        long key = generateFlagKey(player, blockX, blockY, blockZ, flag);
        flagValidationCache.put(key, new CacheEntry(result, System.currentTimeMillis()));

        if (flagValidationCache.size() > MAX_CACHE_SIZE) {
            cleanupOldestEntries();
        }
    }

    public Boolean getCachedPvpResult(String pvpKey) {
        TimedCacheEntry entry = pvpCache.get(pvpKey);
        if (entry != null && !entry.isExpired()) {
            return entry.booleanValue;
        }

        if (entry != null) {
            pvpCache.remove(pvpKey);
        }

        return null;
    }

    public void cachePvpResult(String pvpKey, boolean result) {
        pvpCache.put(pvpKey, new TimedCacheEntry(result, System.currentTimeMillis()));
    }

    public Boolean getCachedAdminPermission(String adminKey) {
        TimedCacheEntry entry = adminPermissionCache.get(adminKey);
        if (entry != null && !entry.isExpired()) {
            return entry.booleanValue;
        }

        if (entry != null) {
            adminPermissionCache.remove(adminKey);
        }

        return null;
    }

    public void cacheAdminPermission(String adminKey, boolean result) {
        adminPermissionCache.put(adminKey, new TimedCacheEntry(result, System.currentTimeMillis()));
    }

    public Boolean getCachedIncompatibility(String incompatibilityKey) {
        TimedCacheEntry entry = incompatibilityCache.get(incompatibilityKey);
        if (entry != null && !entry.isExpired()) {
            return entry.booleanValue;
        }

        if (entry != null) {
            incompatibilityCache.remove(incompatibilityKey);
        }

        return null;
    }

    public void cacheIncompatibility(String incompatibilityKey, boolean result) {
        incompatibilityCache.put(incompatibilityKey, new TimedCacheEntry(result, System.currentTimeMillis()));
    }

    public void invalidatePlayer(Player player) {
        long playerHash = (long) player.getUniqueId().hashCode();

        flagValidationCache.entrySet().removeIf(entry ->
                (entry.getKey() >>> 32) == playerHash);

        String playerPrefix = player.getUniqueId().toString();
        pvpCache.entrySet().removeIf(entry -> entry.getKey().contains(playerPrefix));
        adminPermissionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
    }

    public void invalidateFlag(RegionFlag flag) {
        int flagHash = flag.hashCode();

        flagValidationCache.entrySet().removeIf(entry ->
                (int)(entry.getKey() & 0x0000FFFF) == (flagHash & 0x0000FFFF));

        String flagName = flag.name();
        incompatibilityCache.entrySet().removeIf(entry -> entry.getKey().contains(flagName));
        adminPermissionCache.entrySet().removeIf(entry -> entry.getKey().contains(flagName));
    }

    public void invalidateRegion(Region region) {
         
        int regionHash = region.hashCode();

        flagValidationCache.entrySet().removeIf(entry ->
                Math.abs((int)((entry.getKey() >>> 16) & 0xFFFF) - (regionHash & 0xFFFF)) < 100);

        String regionId = region.getId();
        incompatibilityCache.entrySet().removeIf(entry -> entry.getKey().startsWith(regionId + ":"));
    }

    public void invalidateRegionFlag(Region region, RegionFlag flag) {
        String regionFlagKey = region.getId() + ":" + flag.name();
        incompatibilityCache.remove(regionFlagKey);

        int combinedHash = Objects.hash(region.hashCode(), flag.hashCode()) & 0xFFFF;
        flagValidationCache.entrySet().removeIf(entry ->
                ((int)(entry.getKey() & 0x0000FFFF)) == combinedHash);
    }

    private long generateFlagKey(Player player, int blockX, int blockY, int blockZ, RegionFlag flag) {
         
        long playerHash = (long) player.getUniqueId().hashCode();

        int locationHash = Objects.hash(blockX >> 2, blockY >> 2, blockZ >> 2) & 0xFFFF;
        int flagHash = flag.hashCode() & 0xFFFF;

        return (playerHash << 32) |
                ((long)locationHash << 16) |
                (long)flagHash;
    }

    private void triggerMaintenanceIfNeeded() {
        if (accessCounter.incrementAndGet() % CLEANUP_THRESHOLD == 0) {
            performMaintenance();
        }
    }

    public void performMaintenance() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastMaintenanceTime < 1000) {
            return;
        }

        lastMaintenanceTime = currentTime;

        cleanupExpiredEntries(currentTime);

        if (getTotalCacheSize() > MAX_CACHE_SIZE * 1.2) {
            performAggressiveCleanup();
        }
    }

    private void cleanupExpiredEntries(long currentTime) {
         
        flagValidationCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > CACHE_EXPIRE_MS);

        pvpCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        adminPermissionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        incompatibilityCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void cleanupOldestEntries() {
        int toRemove = MAX_CACHE_SIZE / 5;  

        flagValidationCache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                .limit(toRemove)
                .forEach(entry -> flagValidationCache.remove(entry.getKey()));
    }

    private void performAggressiveCleanup() {
         
        reduceCache(flagValidationCache, 0.7);
        reduceTimedCache(pvpCache, 0.7);
        reduceTimedCache(adminPermissionCache, 0.7);
        reduceTimedCache(incompatibilityCache, 0.7);
    }

    private void reduceCache(ConcurrentHashMap<Long, CacheEntry> cache, double factor) {
        int targetSize = (int)(cache.size() * factor);
        int toRemove = cache.size() - targetSize;

        if (toRemove > 0) {
            cache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                    .limit(toRemove)
                    .forEach(entry -> cache.remove(entry.getKey()));
        }
    }

    private void reduceTimedCache(ConcurrentHashMap<String, TimedCacheEntry> cache, double factor) {
        int targetSize = (int)(cache.size() * factor);
        int toRemove = cache.size() - targetSize;

        if (toRemove > 0) {
            cache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                    .limit(toRemove)
                    .forEach(entry -> cache.remove(entry.getKey()));
        }
    }

    public CacheStats getStats() {
        return new CacheStats(
                flagValidationCache.size(),
                pvpCache.size(),
                adminPermissionCache.size(),
                incompatibilityCache.size(),
                getTotalCacheSize(),
                accessCounter.get()
        );
    }

    private int getTotalCacheSize() {
        return flagValidationCache.size() + pvpCache.size() +
                adminPermissionCache.size() + incompatibilityCache.size();
    }

    public void clearAll() {
        flagValidationCache.clear();
        pvpCache.clear();
        adminPermissionCache.clear();
        incompatibilityCache.clear();
        accessCounter.set(0);
    }

    private static class CacheEntry {
        final boolean result;
        final long timestamp;

        CacheEntry(boolean result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }

    private static class TimedCacheEntry {
        final boolean booleanValue;
        final long timestamp;

        TimedCacheEntry(boolean value, long timestamp) {
            this.booleanValue = value;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }

    public static class CacheStats {
        private final int flagValidationCacheSize;
        private final int pvpCacheSize;
        private final int adminPermissionCacheSize;
        private final int incompatibilityCacheSize;
        private final int totalCacheSize;
        private final long totalAccesses;

        public CacheStats(int flagValidationCacheSize, int pvpCacheSize, int adminPermissionCacheSize,
                          int incompatibilityCacheSize, int totalCacheSize, long totalAccesses) {
            this.flagValidationCacheSize = flagValidationCacheSize;
            this.pvpCacheSize = pvpCacheSize;
            this.adminPermissionCacheSize = adminPermissionCacheSize;
            this.incompatibilityCacheSize = incompatibilityCacheSize;
            this.totalCacheSize = totalCacheSize;
            this.totalAccesses = totalAccesses;
        }

        public int getFlagValidationCacheSize() { return flagValidationCacheSize; }
        public int getPvpCacheSize() { return pvpCacheSize; }
        public int getAdminPermissionCacheSize() { return adminPermissionCacheSize; }
        public int getIncompatibilityCacheSize() { return incompatibilityCacheSize; }
        public int getTotalCacheSize() { return totalCacheSize; }
        public long getTotalAccesses() { return totalAccesses; }

        public double getUtilizationFactor() {
            return (double) totalCacheSize / MAX_CACHE_SIZE;
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheStats{validation=%d, pvp=%d, admin=%d, incompatibility=%d, total=%d, accesses=%d, utilization=%.2f%%}",
                    flagValidationCacheSize, pvpCacheSize, adminPermissionCacheSize,
                    incompatibilityCacheSize, totalCacheSize, totalAccesses,
                    getUtilizationFactor() * 100
            );
        }
    }
}
