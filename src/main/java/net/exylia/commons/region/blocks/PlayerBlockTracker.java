package net.exylia.commons.region.blocks;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

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

    /**
     * Registra que un jugador colocó un bloque en una región
     */
    public void addPlayerBlock(String regionKey, Location location, Material material) {
        String worldName = location.getWorld().getName();
        BlockPosition blockPos = new BlockPosition(
                worldName,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                material
        );

        regionPlayerBlocks.computeIfAbsent(regionKey, k -> ConcurrentHashMap.newKeySet()).add(blockPos);

        // Actualizar cache
        String cacheKey = getCacheKey(regionKey, location);
        blockExistsCache.put(cacheKey, true);

        // Limpiar cache si está muy grande
        if (blockExistsCache.size() > CACHE_MAX_SIZE) {
            cleanupCache();
        }
    }

    /**
     * Remueve un bloque del registro de bloques de jugador
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

                // Actualizar cache
                String cacheKey = getCacheKey(regionKey, location);
                blockExistsCache.put(cacheKey, false);
            }
        }
    }

    /**
     * Verifica si un bloque fue colocado por un jugador
     */
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

    /**
     * Obtiene todos los bloques colocados por jugadores en una región
     */
    public Set<BlockPosition> getPlayerBlocks(String regionKey) {
        Set<BlockPosition> blocks = regionPlayerBlocks.get(regionKey);
        return blocks != null ? new HashSet<>(blocks) : new HashSet<>();
    }

    /**
     * Limpia todos los bloques de una región (útil para regeneración)
     */
    public void clearRegionBlocks(String regionKey) {
        Set<BlockPosition> removed = regionPlayerBlocks.remove(regionKey);
        if (removed != null) {
            // Limpiar cache relacionado
            blockExistsCache.entrySet().removeIf(entry -> entry.getKey().startsWith(regionKey + ":"));
            logInternalDebug(debug(), "Limpiados " + removed.size() + " bloques de jugador para región: " + regionKey);
        }
    }

    /**
     * Obtiene estadísticas del sistema
     */
    public BlockTrackerStats getStats() {
        int totalRegions = regionPlayerBlocks.size();
        int totalBlocks = regionPlayerBlocks.values().stream().mapToInt(Set::size).sum();
        int cacheSize = blockExistsCache.size();

        return new BlockTrackerStats(totalRegions, totalBlocks, cacheSize);
    }

    /**
     * Guarda los datos de forma asíncrona
     */
    public CompletableFuture<Void> saveDataAsync() {
        return CompletableFuture.runAsync(this::saveData);
    }

    /**
     * Guarda los datos de bloques
     */
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(regionPlayerBlocks);
            logInternalDebug(debug(), "Datos de bloques de jugador guardados: " + regionPlayerBlocks.size() + " regiones");
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando datos de bloques de jugador: " + e.getMessage());
        }
    }

    /**
     * Carga los datos de bloques
     */
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

    /**
     * Limpia el cache de existencia de bloques
     */
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

    /**
     * Genera clave de cache
     */
    private String getCacheKey(String regionKey, Location location) {
        return regionKey + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Cierra el sistema y guarda datos
     */
    public void shutdown() {
        logInternalDebug(debug(), "Cerrando PlayerBlockTracker...");
        saveData();
        regionPlayerBlocks.clear();
        blockExistsCache.clear();
    }

    /**
     * Clase para representar una posición de bloque
     */
    @Getter
    public static class BlockPosition implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String world;
        private final int x, y, z;
        private final String material;
        private final long timestamp;

        public BlockPosition(String world, int x, int y, int z, Material material) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material.name();
            this.timestamp = System.currentTimeMillis();
        }

        public boolean matches(Location location) {
            return world.equals(location.getWorld().getName()) &&
                    x == location.getBlockX() &&
                    y == location.getBlockY() &&
                    z == location.getBlockZ();
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
            return String.format("BlockPosition{world='%s', pos=(%d,%d,%d), material='%s'}",
                    world, x, y, z, material);
        }
    }

    /**
     * Clase para estadísticas
     */
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