package net.exylia.commons.region.blocks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

/**
 * Sistema para rastrear bloques colocados por jugadores en regiones
 */
public class PlayerBlockTracker {
    private static PlayerBlockTracker instance;

    private final JavaPlugin plugin;
    private final Map<String, Set<BlockPosition>> regionPlayerBlocks; // region -> set of player-placed blocks
    private final File dataFile;

    // Cache para optimización
    private final Map<String, Boolean> blockExistsCache;
    private static final int CACHE_MAX_SIZE = 10000;

    private PlayerBlockTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regionPlayerBlocks = new ConcurrentHashMap<>();
        this.blockExistsCache = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "region_player_blocks.dat");

        loadData();

        // Limpiar cache cada 5 minutos
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupCache, 6000L, 6000L);
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

                // Actualizar cache
                String cacheKey = getCacheKey(regionKey, location);
                blockExistsCache.put(cacheKey, false);
            }
        }
    }

    public boolean isPlayerPlacedBlock(String regionKey, Location location) {
        // Verificar cache primero
        String cacheKey = getCacheKey(regionKey, location);
        Boolean cached = blockExistsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Verificar en datos
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        if (blocks == null) {
            blockExistsCache.put(cacheKey, false);
            return false;
        }

        boolean exists = blocks.stream().anyMatch(block -> block.matches(location));
        blockExistsCache.put(cacheKey, exists);
        return exists;
    }

    public Set<BlockPosition> getPlayerBlocks(String regionKey) {
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        return blocks != null ? new HashSet<>(blocks) : new HashSet<>();
    }

    public void clearRegionBlocks(String regionKey) {
        Set<BlockPosition> removed = regionPlayerBlocks.remove(regionKey);
        if (removed != null) {
            // Limpiar cache relacionado
            blockExistsCache.entrySet().removeIf(entry -> entry.getKey().startsWith(regionKey + ":"));
            logInternalDebug(debug(), "Limpiados " + removed.size() + " bloques de jugador para región: " + regionKey);
        }
    }

    public BlockTrackerStats getStats() {
        int totalRegions = regionPlayerBlocks.size();
        int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
        int cacheSize = blockExistsCache.size();

        return new BlockTrackerStats(totalRegions, totalBlocks, cacheSize);
    }

    public CompletableFuture<Void> saveDataAsync() {
        return CompletableFuture.runAsync(this::saveData);
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(regionPlayerBlocks);
            logInternalDebug(debug(), "Datos de bloques de jugador guardados: " + regionPlayerBlocks.size() + " regiones");
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando datos de bloques de jugador: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) {
            logInternalDebug(debug(), "No se encontraron datos previos de bloques de jugador");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Object data = ois.readObject();
            if (data instanceof Map) {
                regionPlayerBlocks.putAll((Map<String, Set<BlockPosition>>) data);
                int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
                logInternalDebug(debug(), "Cargados datos de bloques de jugador: " + regionPlayerBlocks.size() +
                        " regiones, " + totalBlocks + " bloques");
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Error cargando datos de bloques de jugador: " + e.getMessage());
            logInternalDebug(debug(), "Se iniciará con datos limpios");
        }
    }

    private void cleanupCache() {
        if (blockExistsCache.size() > CACHE_MAX_SIZE) {
            // Remover 20% de las entradas más antiguas (aproximación simple)
            int toRemove = blockExistsCache.size() / 5;
            Iterator<String> iterator = blockExistsCache.keySet().iterator();

            for (int i = 0; i < toRemove && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private String getCacheKey(String regionKey, Location location) {
        return regionKey + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public void shutdown() {
        logInternalDebug(debug(), "Cerrando PlayerBlockTracker...");
        saveData();
        regionPlayerBlocks.clear();
        blockExistsCache.clear();
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

    public void addPlayerBlock(String regionKey, Location location, Material material) {
        addPlayerBlock(regionKey, location, material, null, "unknown");
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

        // Actualizar cache
        String cacheKey = getCacheKey(regionKey, location);
        blockExistsCache.put(cacheKey, true);

        // Limpiar cache si está muy grande
        if (blockExistsCache.size() > CACHE_MAX_SIZE) {
            cleanupCache();
        }

        logInternalDebug(debug(), String.format(
                "Bloque registrado: %s colocó %s en región %s en %s",
                playerName, material.name(), regionKey, location
        ));
    }

    public void addPlayerBlock(String regionKey, Location location, Material material, Player player) {
        if (player != null) {
            addPlayerBlock(regionKey, location, material, player.getUniqueId(), player.getName());
        } else {
            addPlayerBlock(regionKey, location, material);
        }
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

    public Set<BlockPosition> getPlayerBlocks(String regionKey, Player player) {
        return getPlayerBlocks(regionKey, player.getUniqueId());
    }

    public int getPlayerBlockCount(String regionKey, UUID playerId) {
        return getPlayerBlocks(regionKey, playerId).size();
    }

    public int removePlayerBlocks(String regionKey, UUID playerId) {
        Set<BlockPosition> regionBlocks = regionPlayerBlocks.get(regionKey);
        if (regionBlocks == null) {
            return 0;
        }

        int removedCount = 0;
        Iterator<BlockPosition> iterator = regionBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPosition block = iterator.next();
            if (block.wasPlacedBy(playerId)) {
                iterator.remove();
                removedCount++;

                // Limpiar cache
                String cacheKey = getCacheKey(regionKey,
                        new Location(Bukkit.getWorld(block.getWorld()), block.getX(), block.getY(), block.getZ()));
                blockExistsCache.put(cacheKey, false);
            }
        }

        logInternalDebug(debug(), String.format(
                "Removidos %d bloques del jugador %s en región %s",
                removedCount, playerId, regionKey
        ));

        return removedCount;
    }

    public DetailedBlockTrackerStats getDetailedStats() {
        int totalRegions = regionPlayerBlocks.size();
        int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
        int cacheSize = blockExistsCache.size();

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

        return new DetailedBlockTrackerStats(
                totalRegions,
                totalBlocks,
                cacheSize,
                uniquePlayers.size(),
                materialCounts
        );
    }

    @Getter
    public static class DetailedBlockTrackerStats extends BlockTrackerStats {
        private final int uniquePlayers;
        private final Map<String, Integer> materialCounts;

        public DetailedBlockTrackerStats(int totalRegions, int totalBlocks, int cacheSize,
                                         int uniquePlayers, Map<String, Integer> materialCounts) {
            super(totalRegions, totalBlocks, cacheSize);
            this.uniquePlayers = uniquePlayers;
            this.materialCounts = new HashMap<>(materialCounts);
        }

        @Override
        public String toString() {
            return String.format("DetailedBlockTrackerStats{regions=%d, blocks=%d, cache=%d, players=%d, materials=%d}",
                    getTotalRegions(), getTotalBlocks(), getCacheSize(), uniquePlayers, materialCounts.size());
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