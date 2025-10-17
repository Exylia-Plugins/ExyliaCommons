package net.exylia.commons.region.blocks;

import lombok.Getter;
import net.exylia.commons.async.Schedulers;
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

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class PlayerBlockTracker {
    private static PlayerBlockTracker instance;

    private final JavaPlugin plugin;
    private final Map<String, Set<BlockPosition>> regionPlayerBlocks;
    private final File dataFile;

    private final Map<String, CacheEntry> intelligentCache;
    private static final int MAX_CACHE_SIZE = 100000;  
    private static final long CACHE_EXPIRE_TIME = 300000;  
    private static final long CRITICAL_DATA_PROTECT_TIME = 30000;  

    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong criticalDataProtected = new AtomicLong();

    private volatile boolean isDirty = false;
    private long lastAutoSave = System.currentTimeMillis();
    private static final long AUTO_SAVE_INTERVAL = 60000;  

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

        regionPlayerBlocks.computeIfAbsent(regionKey, k -> ConcurrentHashMap.newKeySet()).add(blockPos);

        String cacheKey = getCacheKey(regionKey, location);
        intelligentCache.put(cacheKey, new CacheEntry(true, System.currentTimeMillis(), true));

        markDirty();

    }

    public boolean isPlayerPlacedBlock(String regionKey, Location location) {
        String cacheKey = getCacheKey(regionKey, location);

        CacheEntry cached = intelligentCache.get(cacheKey);
        if (cached != null) {
             
            if (cached.isCritical && (System.currentTimeMillis() - cached.timestamp) < CRITICAL_DATA_PROTECT_TIME) {
                cacheHits.incrementAndGet();
                return cached.exists;
            }

            if (!cached.isExpired()) {
                cacheHits.incrementAndGet();
                return cached.exists;
            }
        }

        cacheMisses.incrementAndGet();

        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        boolean exists = false;

        if (blocks != null && !blocks.isEmpty()) {
             
            int blockX = location.getBlockX();
            int blockY = location.getBlockY();
            int blockZ = location.getBlockZ();
            String worldName = location.getWorld().getName();

            for (BlockPosition block : blocks) {
                if (block.getX() == blockX &&
                        block.getY() == blockY &&
                        block.getZ() == blockZ &&
                        block.getWorld().equals(worldName)) {
                    exists = true;
                    break;  
                }
            }
        }

        intelligentCache.put(cacheKey, new CacheEntry(exists, System.currentTimeMillis(), exists));

        if (exists) {
            criticalDataProtected.incrementAndGet();
        }

        return exists;
    }

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

                String cacheKey = getCacheKey(regionKey, location);
                intelligentCache.put(cacheKey, new CacheEntry(false, System.currentTimeMillis(), true));

            }
        }
    }

    public void clearRegionBlocks(String regionKey) {
        Set<BlockPosition> removed = regionPlayerBlocks.remove(regionKey);
        if (removed != null) {
             
            long currentTime = System.currentTimeMillis();

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

    private void startIntelligentCacheManager() {
        Schedulers.asyncTimer(() -> {
            performIntelligentCacheCleanup();
            checkAutoSave();
        }, 20L * 10, 20L * 10);
    }

    private void performIntelligentCacheCleanup() {
        if (intelligentCache.size() <= MAX_CACHE_SIZE) {
            return;  
        }

        long currentTime = System.currentTimeMillis();
        int removed = 0;
        int criticalProtected = 0;

        Iterator<Map.Entry<String, CacheEntry>> iterator = intelligentCache.entrySet().iterator();

        while (iterator.hasNext() && intelligentCache.size() > (MAX_CACHE_SIZE * 0.8)) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            CacheEntry cacheEntry = entry.getValue();

            if (cacheEntry.isCritical && (currentTime - cacheEntry.timestamp) < CRITICAL_DATA_PROTECT_TIME) {
                criticalProtected++;
                continue;
            }

            if (cacheEntry.exists && (currentTime - cacheEntry.timestamp) < CRITICAL_DATA_PROTECT_TIME) {
                criticalProtected++;
                continue;
            }

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

    private void checkAutoSave() {
        if (isDirty && (System.currentTimeMillis() - lastAutoSave) > AUTO_SAVE_INTERVAL) {
            saveDataAsync().thenRun(() -> {
                isDirty = false;
                lastAutoSave = System.currentTimeMillis();
                logInternalDebug("Auto-guardado completado");
            });
        }
    }

    private void markDirty() {
        this.isDirty = true;
    }

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

    public CompletableFuture<Void> saveDataAsync() {
        return CompletableFuture.runAsync(this::saveData);
    }

    private void saveData() {
         
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

    private String getCacheKey(String regionKey, Location location) {
        return regionKey + ":" + location.getBlockX() + "," +
                location.getBlockY() + "," + location.getBlockZ();
    }

    public void shutdown() {
        logInternalDebug("Cerrando PlayerBlockTracker...");

        if (isDirty) {
            saveData();
        }

        regionPlayerBlocks.clear();
        intelligentCache.clear();

        logInternalDebug("PlayerBlockTracker cerrado con guardado final");
    }

    private static class CacheEntry {
        final boolean exists;
        final long timestamp;
        final boolean isCritical;  

        CacheEntry(boolean exists, long timestamp, boolean isCritical) {
            this.exists = exists;
            this.timestamp = timestamp;
            this.isCritical = isCritical;
        }

        boolean isExpired() {
            long age = System.currentTimeMillis() - timestamp;
             
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
