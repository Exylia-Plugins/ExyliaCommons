package net.exylia.commons.region.blocks;

import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

/**
 * Sistema MEJORADO para rastrear bloques colocados por jugadores en regiones
 * Incluye cache inteligente que NO pierde información crítica
 */
public class PlayerBlockTracker {
    private static PlayerBlockTracker instance;

    private final JavaPlugin plugin;
    private final Map<String, Set<BlockPosition>> regionPlayerBlocks;
    private final File dataFile;

    // ===== CACHE INTELIGENTE MEJORADO =====
    private final Map<String, CacheEntry> intelligentCache;
    private static final int MAX_CACHE_SIZE = 100000; // Aumentado significativamente
    private static final long CACHE_EXPIRE_TIME = 300000; // 5 minutos para datos NO críticos
    private static final long CRITICAL_DATA_PROTECT_TIME = 30000; // 30 segundos protección para datos críticos

    // Estadísticas del cache
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong criticalDataProtected = new AtomicLong();

    // Auto-guardado mejorado
    private volatile boolean isDirty = false;
    private long lastAutoSave = System.currentTimeMillis();
    private static final long AUTO_SAVE_INTERVAL = 60000; // 1 minuto

    private PlayerBlockTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regionPlayerBlocks = new ConcurrentHashMap<>();
        this.intelligentCache = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "region_player_blocks.dat");

        loadData();
        startIntelligentCacheManager();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new PlayerBlockTracker(plugin);
        }
    }

    public static PlayerBlockTracker getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerBlockTracker no ha sido inicializado");
        }
        return instance;
    }

    // ===== MÉTODOS PRINCIPALES MEJORADOS =====

    /**
     * MEJORADO: Añade un bloque con cache inteligente
     */
    public void addPlayerBlock(String regionKey, Location location, Material material, UUID playerId, String playerName) {
        String worldName = location.getWorld().getName();
        BlockPosition blockPos = new BlockPosition(
                worldName,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                material,
                playerId,
                playerName
        );

        // Añadir a datos principales
        regionPlayerBlocks.computeIfAbsent(regionKey, k -> ConcurrentHashMap.newKeySet()).add(blockPos);

        // CACHE INTELIGENTE: Marcar como EXISTS y CRÍTICO
        String cacheKey = getCacheKey(regionKey, location);
        intelligentCache.put(cacheKey, new CacheEntry(true, System.currentTimeMillis(), true));

        markDirty();

        // Auto-guardado si es necesario
        checkAutoSave();

        logInternalDebug(String.format(
                "Bloque registrado con cache inteligente: %s colocó %s en región %s en %s",
                playerName, material.name(), regionKey, location
        ));
    }

    /**
     * OPTIMIZADO: Verifica si un bloque fue colocado por un jugador - MÁXIMO RENDIMIENTO
     */
    public boolean isPlayerPlacedBlock(String regionKey, Location location) {
        String cacheKey = getCacheKey(regionKey, location);

        // PASO 1: Cache inteligente - RÁPIDO
        CacheEntry cached = intelligentCache.get(cacheKey);
        if (cached != null) {
            // Si es dato crítico reciente, SIEMPRE confiar en el cache
            if (cached.isCritical && (System.currentTimeMillis() - cached.timestamp) < CRITICAL_DATA_PROTECT_TIME) {
                cacheHits.incrementAndGet();
                return cached.exists;
            }

            // Si no ha expirado, usar valor del cache
            if (!cached.isExpired()) {
                cacheHits.incrementAndGet();
                return cached.exists;
            }
        }

        cacheMisses.incrementAndGet();

        // PASO 2: Verificar en datos reales - OPTIMIZADO
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        boolean exists = false;

        if (blocks != null && !blocks.isEmpty()) {
            // OPTIMIZACIÓN: Usar coordenadas directas para comparación más rápida
            int blockX = location.getBlockX();
            int blockY = location.getBlockY();
            int blockZ = location.getBlockZ();
            String worldName = location.getWorld().getName();

            // Búsqueda optimizada - evitar métodos costosos
            for (BlockPosition block : blocks) {
                if (block.getX() == blockX &&
                        block.getY() == blockY &&
                        block.getZ() == blockZ &&
                        block.getWorld().equals(worldName)) {
                    exists = true;
                    break; // Salir inmediatamente al encontrar
                }
            }
        }

        // PASO 3: Actualizar cache con resultado REAL
        intelligentCache.put(cacheKey, new CacheEntry(exists, System.currentTimeMillis(), exists));

        if (exists) {
            criticalDataProtected.incrementAndGet();
        }

        return exists;
    }

    /**
     * MEJORADO: Remueve un bloque con actualización inteligente del cache
     */
    public void removePlayerBlock(String regionKey, Location location) {
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        if (blocks != null) {
            BlockPosition toRemove = null;
            for (BlockPosition block : blocks) {
                if (block.matches(location)) {
                    toRemove = block;
                    break;
                }
            }

            if (toRemove != null) {
                blocks.remove(toRemove);
                markDirty();

                // CACHE INTELIGENTE: Marcar como NO EXISTS pero CRÍTICO (para evitar falsos positivos)
                String cacheKey = getCacheKey(regionKey, location);
                intelligentCache.put(cacheKey, new CacheEntry(false, System.currentTimeMillis(), true));

                logInternalDebug(String.format(
                        "Bloque removido y cache actualizado: %s en región %s",
                        location, regionKey
                ));
            }
        }
    }

    /**
     * MEJORADO: Limpia bloques de región con actualización masiva de cache
     */
    public void clearRegionBlocks(String regionKey) {
        Set<BlockPosition> removed = regionPlayerBlocks.remove(regionKey);
        if (removed != null) {
            // CACHE INTELIGENTE: Actualizar en lote todas las entradas relacionadas
            long currentTime = System.currentTimeMillis();

            // Marcar todas las ubicaciones de la región como NO EXISTS y CRÍTICAS
            for (BlockPosition block : removed) {
                String cacheKey = getCacheKey(regionKey,
                        new Location(Bukkit.getWorld(block.getWorld()),
                                block.getX(), block.getY(), block.getZ()));
                intelligentCache.put(cacheKey, new CacheEntry(false, currentTime, true));
            }

            markDirty();
            logInternalDebug("Cache actualizado para " + removed.size() + " bloques en región: " + regionKey);
        }
    }

    // ===== GESTIÓN INTELIGENTE DEL CACHE =====

    /**
     * NUEVO: Inicia el administrador inteligente del cache
     */
    private void startIntelligentCacheManager() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            performIntelligentCacheCleanup();
            checkAutoSave();
        }, 20L * 10, 20L * 10); // Cada 10 segundos
    }

    /**
     * NUEVO: Limpieza inteligente que PROTEGE datos críticos
     */
    private void performIntelligentCacheCleanup() {
        if (intelligentCache.size() <= MAX_CACHE_SIZE) {
            return; // No necesita limpieza
        }

        long currentTime = System.currentTimeMillis();
        int removed = 0;
        int criticalProtected = 0;

        Iterator<Map.Entry<String, CacheEntry>> iterator = intelligentCache.entrySet().iterator();

        while (iterator.hasNext() && intelligentCache.size() > (MAX_CACHE_SIZE * 0.8)) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            CacheEntry cacheEntry = entry.getValue();

            // PROTECCIÓN 1: NUNCA eliminar datos críticos recientes
            if (cacheEntry.isCritical && (currentTime - cacheEntry.timestamp) < CRITICAL_DATA_PROTECT_TIME) {
                criticalProtected++;
                continue;
            }

            // PROTECCIÓN 2: NUNCA eliminar datos que confirman existencia de bloques
            if (cacheEntry.exists && (currentTime - cacheEntry.timestamp) < CRITICAL_DATA_PROTECT_TIME) {
                criticalProtected++;
                continue;
            }

            // Eliminar solo datos expirados no críticos o muy antiguos
            if (cacheEntry.isExpired() || (!cacheEntry.isCritical && (currentTime - cacheEntry.timestamp) > CACHE_EXPIRE_TIME)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0 || criticalProtected > 0) {
            logInternalDebug(String.format(
                    "Cache inteligente: Eliminadas %d entradas, protegidas %d críticas. Tamaño: %d",
                    removed, criticalProtected, intelligentCache.size()
            ));
        }
    }

    // ===== AUTO-GUARDADO MEJORADO =====

    /**
     * NUEVO: Verifica si necesita auto-guardado
     */
    private void checkAutoSave() {
        if (isDirty && (System.currentTimeMillis() - lastAutoSave) > AUTO_SAVE_INTERVAL) {
            saveDataAsync().thenRun(() -> {
                isDirty = false;
                lastAutoSave = System.currentTimeMillis();
                logInternalDebug("Auto-guardado completado");
            });
        }
    }

    /**
     * NUEVO: Marca los datos como modificados
     */
    private void markDirty() {
        this.isDirty = true;
    }

    // ===== MÉTODOS AUXILIARES MEJORADOS =====

    public Set<BlockPosition> getPlayerBlocks(String regionKey) {
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        return blocks != null ? new HashSet<>(blocks) : new HashSet<>();
    }

    public UUID getPlayerWhoPlacedBlock(String regionKey, Location location) {
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        if (blocks == null) {
            return null;
        }

        for (BlockPosition block : blocks) {
            if (block.matches(location)) {
                return block.getPlayerId();
            }
        }

        return null;
    }

    public String getPlayerNameWhoPlacedBlock(String regionKey, Location location) {
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        if (blocks == null) {
            return null;
        }

        for (BlockPosition block : blocks) {
            if (block.matches(location)) {
                return block.getPlayerName();
            }
        }

        return null;
    }

    public Set<BlockPosition> getPlayerBlocks(String regionKey, UUID playerId) {
        Set<BlockPosition> regionBlocks = regionPlayerBlocks.get(regionKey);
        if (regionBlocks == null) {
            return new HashSet<>();
        }

        return regionBlocks.stream()
                .filter(block -> block.wasPlacedBy(playerId))
                .collect(Collectors.toSet());
    }

    public int removePlayerBlocks(String regionKey, UUID playerId) {
        Set<BlockPosition> regionBlocks = regionPlayerBlocks.get(regionKey);
        if (regionBlocks == null) {
            return 0;
        }

        int removedCount = 0;
        long currentTime = System.currentTimeMillis();
        Iterator<BlockPosition> iterator = regionBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPosition block = iterator.next();
            if (block.wasPlacedBy(playerId)) {
                iterator.remove();
                removedCount++;

                // Actualizar cache
                String cacheKey = getCacheKey(regionKey,
                        new Location(Bukkit.getWorld(block.getWorld()),
                                block.getX(), block.getY(), block.getZ()));
                intelligentCache.put(cacheKey, new CacheEntry(false, currentTime, true));
            }
        }

        if (removedCount > 0) {
            markDirty();
            logInternalDebug(String.format(
                    "Removidos %d bloques del jugador %s en región %s",
                    removedCount, playerId, regionKey
            ));
        }

        return removedCount;
    }

    // ===== MÉTODOS DE CONVENIENCIA MEJORADOS =====

    public void addPlayerBlock(String regionKey, Location location, Material material) {
        addPlayerBlock(regionKey, location, material, null, "unknown");
    }

    public void addPlayerBlock(String regionKey, Location location, Material material, Player player) {
        if (player != null) {
            addPlayerBlock(regionKey, location, material, player.getUniqueId(), player.getName());
        } else {
            addPlayerBlock(regionKey, location, material);
        }
    }

    public Set<BlockPosition> getPlayerBlocks(String regionKey, Player player) {
        return getPlayerBlocks(regionKey, player.getUniqueId());
    }

    public int getPlayerBlockCount(String regionKey, UUID playerId) {
        return getPlayerBlocks(regionKey, playerId).size();
    }

    // ===== ESTADÍSTICAS MEJORADAS =====

    public BlockTrackerStats getStats() {
        int totalRegions = regionPlayerBlocks.size();
        int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
        int cacheSize = intelligentCache.size();

        return new BlockTrackerStats(totalRegions, totalBlocks, cacheSize);
    }

    public DetailedBlockTrackerStats getDetailedStats() {
        int totalRegions = regionPlayerBlocks.size();
        int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
        int cacheSize = intelligentCache.size();

        // Contar jugadores únicos
        Set<UUID> uniquePlayers = new HashSet<>();
        Map<String, Integer> materialCounts = new HashMap<>();

        for (Set<BlockPosition> regionBlocks : regionPlayerBlocks.values()) {
            for (BlockPosition block : regionBlocks) {
                if (block.getPlayerId() != null) {
                    uniquePlayers.add(block.getPlayerId());
                }
                materialCounts.merge(block.getMaterial(), 1, Integer::sum);
            }
        }

        // Estadísticas del cache inteligente
        long criticalEntries = intelligentCache.values().stream()
                .mapToLong(entry -> entry.isCritical ? 1 : 0)
                .sum();

        return new DetailedBlockTrackerStats(
                totalRegions,
                totalBlocks,
                cacheSize,
                uniquePlayers.size(),
                materialCounts,
                cacheHits.get(),
                cacheMisses.get(),
                criticalDataProtected.get(),
                criticalEntries
        );
    }

    // ===== PERSISTENCIA MEJORADA =====

    public CompletableFuture<Void> saveDataAsync() {
        return CompletableFuture.runAsync(this::saveData);
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(regionPlayerBlocks);
            logInternalDebug("Datos guardados: " + regionPlayerBlocks.size() + " regiones");
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando datos: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) {
            logInternalDebug("No se encontraron datos previos");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Object data = ois.readObject();
            if (data instanceof Map) {
                regionPlayerBlocks.putAll((Map<String, Set<BlockPosition>>) data);
                int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
                logInternalDebug("Datos cargados: " + regionPlayerBlocks.size() +
                        " regiones, " + totalBlocks + " bloques");
            }
        } catch (IOException | ClassNotFoundException e) {
            logInternalWarn("Error cargando datos: " + e.getMessage());
            logInternalDebug("Se iniciará con datos limpios");
        }
    }

    // ===== UTILIDADES =====

    private String getCacheKey(String regionKey, Location location) {
        return regionKey + ":" + location.getBlockX() + "," +
                location.getBlockY() + "," + location.getBlockZ();
    }

    public void shutdown() {
        logInternalDebug("Cerrando PlayerBlockTracker...");

        // Guardado final
        if (isDirty) {
            saveData();
        }

        regionPlayerBlocks.clear();
        intelligentCache.clear();

        logInternalDebug("PlayerBlockTracker cerrado con guardado final");
    }

    // ===== CLASES INTERNAS MEJORADAS =====

    /**
     * NUEVA: Entrada de cache inteligente
     */
    private static class CacheEntry {
        final boolean exists;
        final long timestamp;
        final boolean isCritical; // Datos críticos que no deben perderse

        CacheEntry(boolean exists, long timestamp, boolean isCritical) {
            this.exists = exists;
            this.timestamp = timestamp;
            this.isCritical = isCritical;
        }

        boolean isExpired() {
            long age = System.currentTimeMillis() - timestamp;
            // Los datos críticos tienen tiempo de vida más largo
            long maxAge = isCritical ? CRITICAL_DATA_PROTECT_TIME : CACHE_EXPIRE_TIME;
            return age > maxAge;
        }
    }

    @Getter
    public static class DetailedBlockTrackerStats extends BlockTrackerStats {
        private final int uniquePlayers;
        private final Map<String, Integer> materialCounts;
        private final long cacheHits;
        private final long cacheMisses;
        private final long criticalDataProtected;
        private final long criticalEntries;

        public DetailedBlockTrackerStats(int totalRegions, int totalBlocks, int cacheSize,
                                         int uniquePlayers, Map<String, Integer> materialCounts,
                                         long cacheHits, long cacheMisses, long criticalDataProtected,
                                         long criticalEntries) {
            super(totalRegions, totalBlocks, cacheSize);
            this.uniquePlayers = uniquePlayers;
            this.materialCounts = new HashMap<>(materialCounts);
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.criticalDataProtected = criticalDataProtected;
            this.criticalEntries = criticalEntries;
        }

        public double getCacheHitRatio() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("DetailedBlockTrackerStats{regions=%d, blocks=%d, cache=%d, " +
                            "players=%d, materials=%d, cache_hits=%d, cache_misses=%d, " +
                            "hit_ratio=%.2f%%, critical_protected=%d, critical_entries=%d}",
                    getTotalRegions(), getTotalBlocks(), getCacheSize(), uniquePlayers,
                    materialCounts.size(), cacheHits, cacheMisses, getCacheHitRatio() * 100,
                    criticalDataProtected, criticalEntries);
        }
    }

    // BlockPosition y BlockTrackerStats permanecen igual que en el código original
    @Getter
    public static class BlockPosition implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L;

        private final String world;
        private final int x, y, z;
        private final String material;
        private final long timestamp;
        private final UUID playerId;
        private final String playerName;

        public BlockPosition(String world, int x, int y, int z, Material material) {
            this(world, x, y, z, material, null, "unknown");
        }

        public BlockPosition(String world, int x, int y, int z, Material material, UUID playerId, String playerName) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material.name();
            this.timestamp = System.currentTimeMillis();
            this.playerId = playerId;
            this.playerName = playerName != null ? playerName : "unknown";
        }

        public boolean matches(Location location) {
            return world.equals(location.getWorld().getName()) &&
                    x == location.getBlockX() &&
                    y == location.getBlockY() &&
                    z == location.getBlockZ();
        }

        public boolean wasPlacedBy(UUID playerId) {
            return this.playerId != null && this.playerId.equals(playerId);
        }

        public boolean wasPlacedBy(Player player) {
            return wasPlacedBy(player.getUniqueId());
        }

        public long getAgeInMillis() {
            return System.currentTimeMillis() - timestamp;
        }

        public long getAgeInSeconds() {
            return getAgeInMillis() / 1000;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockPosition other)) return false;
            return Objects.equals(world, other.world) && x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }

        @Override
        public String toString() {
            return String.format("BlockPosition{world='%s', pos=(%d,%d,%d), material='%s', player='%s', age=%ds}",
                    world, x, y, z, material, playerName, getAgeInSeconds());
        }
    }

    @Getter
    public static class BlockTrackerStats {
        private final int totalRegions;
        private final int totalBlocks;
        private final int cacheSize;

        public BlockTrackerStats(int totalRegions, int totalBlocks, int cacheSize) {
            this.totalRegions = totalRegions;
            this.totalBlocks = totalBlocks;
            this.cacheSize = cacheSize;
        }

        @Override
        public String toString() {
            return String.format("BlockTrackerStats{regions=%d, blocks=%d, cache=%d}",
                    totalRegions, totalBlocks, cacheSize);
        }
    }
}