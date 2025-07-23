package net.exylia.commons.region.flags;

import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache optimizado para validaciones de flags con múltiples tipos de cache
 * Usa hashes eficientes y limpieza automática para máximo rendimiento
 */
public class OptimizedFlagCache {
    // Configuración de cache
    private static final int MAX_CACHE_SIZE = 8000; // Aumentado para 500 jugadores
    private static final long CACHE_EXPIRE_MS = 4000; // 4 segundos
    private static final int CLEANUP_THRESHOLD = 100; // Cleanup cada 100 accesos

    // Caches especializados
    private final ConcurrentHashMap<Long, CacheEntry> flagValidationCache;
    private final ConcurrentHashMap<String, TimedCacheEntry> pvpCache;
    private final ConcurrentHashMap<String, TimedCacheEntry> adminPermissionCache;
    private final ConcurrentHashMap<String, TimedCacheEntry> incompatibilityCache;

    // Contadores para maintenance
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

    // ===== FLAG VALIDATION CACHE =====

    /**
     * Obtiene resultado desde cache de validación de flags
     */
    public Boolean getCachedResult(Player player, int blockX, int blockY, int blockZ, RegionFlag flag) {
        long key = generateFlagKey(player, blockX, blockY, blockZ, flag);
        CacheEntry entry = flagValidationCache.get(key);

        if (entry != null && !entry.isExpired()) {
            return entry.result;
        }

        // Limpiar entrada expirada
        if (entry != null) {
            flagValidationCache.remove(key);
        }

        return null;
    }

    /**
     * Guarda resultado en cache de validación
     */
    public void cacheResult(Player player, int blockX, int blockY, int blockZ, RegionFlag flag, boolean result) {
        triggerMaintenanceIfNeeded();

        long key = generateFlagKey(player, blockX, blockY, blockZ, flag);
        flagValidationCache.put(key, new CacheEntry(result, System.currentTimeMillis()));

        // Control de tamaño
        if (flagValidationCache.size() > MAX_CACHE_SIZE) {
            cleanupOldestEntries();
        }
    }

    // ===== PVP CACHE =====

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

    // ===== ADMIN PERMISSION CACHE =====

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

    // ===== INCOMPATIBILITY CACHE =====

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

    // ===== INVALIDATION METHODS =====

    /**
     * Invalida cache para un jugador específico
     */
    public void invalidatePlayer(Player player) {
        long playerHash = (long) player.getUniqueId().hashCode();

        // Invalidar flag validation cache
        flagValidationCache.entrySet().removeIf(entry ->
                (entry.getKey() >>> 32) == playerHash);

        // Invalidar otros caches
        String playerPrefix = player.getUniqueId().toString();
        pvpCache.entrySet().removeIf(entry -> entry.getKey().contains(playerPrefix));
        adminPermissionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
    }

    /**
     * Invalida cache para una flag específica
     */
    public void invalidateFlag(RegionFlag flag) {
        int flagHash = flag.hashCode();

        // Invalidar flag validation cache
        flagValidationCache.entrySet().removeIf(entry ->
                (int)(entry.getKey() & 0x0000FFFF) == (flagHash & 0x0000FFFF));

        // Invalidar incompatibilidad cache
        String flagName = flag.name();
        incompatibilityCache.entrySet().removeIf(entry -> entry.getKey().contains(flagName));
        adminPermissionCache.entrySet().removeIf(entry -> entry.getKey().contains(flagName));
    }

    /**
     * Invalida cache para una región específica
     */
    public void invalidateRegion(Region region) {
        // Para optimización, solo invalidamos por área aproximada
        int regionHash = region.hashCode();

        // Esto podría generar algunos false positives, pero es más eficiente
        // que verificar cada entrada individualmente
        flagValidationCache.entrySet().removeIf(entry ->
                Math.abs((int)((entry.getKey() >>> 16) & 0xFFFF) - (regionHash & 0xFFFF)) < 100);

        String regionId = region.getId();
        incompatibilityCache.entrySet().removeIf(entry -> entry.getKey().startsWith(regionId + ":"));
    }

    /**
     * Invalida cache para una flag específica en una región específica
     */
    public void invalidateRegionFlag(Region region, RegionFlag flag) {
        String regionFlagKey = region.getId() + ":" + flag.name();
        incompatibilityCache.remove(regionFlagKey);

        // Invalidar entradas de validación relacionadas
        int combinedHash = Objects.hash(region.hashCode(), flag.hashCode()) & 0xFFFF;
        flagValidationCache.entrySet().removeIf(entry ->
                ((int)(entry.getKey() & 0x0000FFFF)) == combinedHash);
    }

    // ===== KEY GENERATION =====

    /**
     * Genera clave optimizada para flag validation usando hashes compactos
     */
    private long generateFlagKey(Player player, int blockX, int blockY, int blockZ, RegionFlag flag) {
        // Layout de la clave (64 bits):
        // [32 bits: player hash][16 bits: location hash][16 bits: flag hash]

        long playerHash = (long) player.getUniqueId().hashCode();

        // Agrupar ubicación por chunks de 4x4x4 para mejor cache locality
        int locationHash = Objects.hash(blockX >> 2, blockY >> 2, blockZ >> 2) & 0xFFFF;
        int flagHash = flag.hashCode() & 0xFFFF;

        return (playerHash << 32) |
                ((long)locationHash << 16) |
                (long)flagHash;
    }

    // ===== MAINTENANCE =====

    /**
     * Trigger maintenance si es necesario
     */
    private void triggerMaintenanceIfNeeded() {
        if (accessCounter.incrementAndGet() % CLEANUP_THRESHOLD == 0) {
            performMaintenance();
        }
    }

    /**
     * Realiza mantenimiento del cache
     */
    public void performMaintenance() {
        long currentTime = System.currentTimeMillis();

        // Solo ejecutar maintenance si ha pasado suficiente tiempo
        if (currentTime - lastMaintenanceTime < 1000) {
            return;
        }

        lastMaintenanceTime = currentTime;

        // Limpiar entradas expiradas en todos los caches
        cleanupExpiredEntries(currentTime);

        // Control de tamaño si es necesario
        if (getTotalCacheSize() > MAX_CACHE_SIZE * 1.2) {
            performAggressiveCleanup();
        }
    }

    /**
     * Limpia entradas expiradas
     */
    private void cleanupExpiredEntries(long currentTime) {
        // Flag validation cache
        flagValidationCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > CACHE_EXPIRE_MS);

        // PvP cache
        pvpCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Admin permission cache
        adminPermissionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Incompatibility cache
        incompatibilityCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Limpia entradas más antiguas cuando el cache está lleno
     */
    private void cleanupOldestEntries() {
        int toRemove = MAX_CACHE_SIZE / 5; // Remover 20%

        // Ordenar por timestamp y remover las más antiguas
        flagValidationCache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                .limit(toRemove)
                .forEach(entry -> flagValidationCache.remove(entry.getKey()));
    }

    /**
     * Limpieza agresiva cuando todos los caches están llenos
     */
    private void performAggressiveCleanup() {
        // Reducir cada cache a 70% de su tamaño actual
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

    // ===== STATISTICS =====

    /**
     * Obtiene estadísticas del cache
     */
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

    /**
     * Obtiene el tamaño total de todos los caches
     */
    private int getTotalCacheSize() {
        return flagValidationCache.size() + pvpCache.size() +
                adminPermissionCache.size() + incompatibilityCache.size();
    }

    /**
     * Limpia completamente todos los caches
     */
    public void clearAll() {
        flagValidationCache.clear();
        pvpCache.clear();
        adminPermissionCache.clear();
        incompatibilityCache.clear();
        accessCounter.set(0);
    }

    // ===== CACHE ENTRY CLASSES =====

    /**
     * Entrada de cache para validaciones de flags
     */
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

    /**
     * Entrada de cache con tiempo para otros tipos de cache
     */
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

    /**
     * Estadísticas del cache optimizado
     */
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

        /**
         * Factor de utilización del cache (0.0 - 1.0)
         */
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