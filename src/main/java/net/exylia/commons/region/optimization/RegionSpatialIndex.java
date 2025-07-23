package net.exylia.commons.region.optimization;

import net.exylia.commons.region.model.Region;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Índice espacial optimizado para búsquedas O(1) de regiones
 * Divide el mundo en chunks para búsqueda eficiente manteniendo precisión exacta
 */
public class RegionSpatialIndex {
    private static final int CHUNK_SIZE = 16; // Tamaño estándar de chunk

    // Map<World, Map<ChunkCoord, Set<Region>>>
    private final Map<World, Map<Long, Set<Region>>> chunkIndex;

    // Estadísticas para monitoring
    private volatile int totalRegions = 0;
    private volatile int totalChunksUsed = 0;

    public RegionSpatialIndex() {
        this.chunkIndex = new ConcurrentHashMap<>();
    }

    /**
     * Registra una región en el índice espacial
     * Complejidad: O(k) donde k es el número de chunks que ocupa la región
     */
    public void addRegion(Region region) {
        if (region == null || !region.isValid()) {
            return;
        }

        World world = region.getWorld();
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        // Calcular chunks que ocupa la región
        int minChunkX = min.getBlockX() >> 4; // División por 16 optimizada
        int maxChunkX = max.getBlockX() >> 4;
        int minChunkZ = min.getBlockZ() >> 4;
        int maxChunkZ = max.getBlockZ() >> 4;

        Map<Long, Set<Region>> worldIndex = chunkIndex.computeIfAbsent(world,
                k -> new ConcurrentHashMap<>());

        int chunksAdded = 0;

        // Registrar región en todos los chunks que ocupa
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(chunkX, chunkZ);
                Set<Region> regionSet = worldIndex.computeIfAbsent(chunkKey,
                        k -> ConcurrentHashMap.newKeySet());

                if (regionSet.add(region)) {
                    chunksAdded++;
                }
            }
        }

        totalRegions++;
        totalChunksUsed += chunksAdded;
    }

    /**
     * Remueve una región del índice
     * Complejidad: O(k) donde k es el número de chunks que ocupaba la región
     */
    public void removeRegion(Region region) {
        if (region == null || !region.isValid()) {
            return;
        }

        World world = region.getWorld();
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) return;

        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();

        int minChunkX = min.getBlockX() >> 4;
        int maxChunkX = max.getBlockX() >> 4;
        int minChunkZ = min.getBlockZ() >> 4;
        int maxChunkZ = max.getBlockZ() >> 4;

        int chunksRemoved = 0;

        // Remover de todos los chunks que ocupaba
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(chunkX, chunkZ);
                Set<Region> regionSet = worldIndex.get(chunkKey);

                if (regionSet != null && regionSet.remove(region)) {
                    chunksRemoved++;

                    // Limpiar chunks vacíos para ahorrar memoria
                    if (regionSet.isEmpty()) {
                        worldIndex.remove(chunkKey);
                    }
                }
            }
        }

        // Limpiar mundo si está vacío
        if (worldIndex.isEmpty()) {
            chunkIndex.remove(world);
        }

        totalRegions--;
        totalChunksUsed -= chunksRemoved;
    }

    /**
     * Busca regiones en una ubicación específica
     * Complejidad: O(1) promedio - solo verifica regiones del chunk específico
     * Mantiene precisión EXACTA al bloque
     */
    public List<Region> getRegionsAt(Location location) {
        if (location == null) {
            return Collections.emptyList();
        }

        Map<Long, Set<Region>> worldIndex = chunkIndex.get(location.getWorld());
        if (worldIndex == null) {
            return Collections.emptyList();
        }

        // Calcular chunk de la ubicación
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        long chunkKey = getChunkKey(chunkX, chunkZ);

        Set<Region> candidateRegions = worldIndex.get(chunkKey);
        if (candidateRegions == null || candidateRegions.isEmpty()) {
            return Collections.emptyList();
        }

        // AQUÍ SE MANTIENE LA PRECISIÓN EXACTA:
        // Filtrar regiones candidatas con verificación precisa y ordenar por prioridad
        return candidateRegions.stream()
                .filter(region -> region.contains(location)) // ← VERIFICACIÓN EXACTA AL BLOQUE
                .sorted((r1, r2) -> r2.getPriority().getLevel() - r1.getPriority().getLevel())
                .toList();
    }

    /**
     * Busca regiones en múltiples ubicaciones (optimización para áreas)
     */
    public Set<Region> getRegionsInArea(Location corner1, Location corner2) {
        if (corner1 == null || corner2 == null || !corner1.getWorld().equals(corner2.getWorld())) {
            return Collections.emptySet();
        }

        Map<Long, Set<Region>> worldIndex = chunkIndex.get(corner1.getWorld());
        if (worldIndex == null) {
            return Collections.emptySet();
        }

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        Set<Region> result = new HashSet<>();

        // Recopilar todas las regiones de los chunks del área
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = getChunkKey(chunkX, chunkZ);
                Set<Region> chunkRegions = worldIndex.get(chunkKey);

                if (chunkRegions != null) {
                    result.addAll(chunkRegions);
                }
            }
        }

        return result;
    }

    /**
     * Obtiene todas las regiones de un mundo específico
     */
    public Set<Region> getRegionsInWorld(World world) {
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) {
            return Collections.emptySet();
        }

        Set<Region> result = new HashSet<>();
        for (Set<Region> chunkRegions : worldIndex.values()) {
            result.addAll(chunkRegions);
        }

        return result;
    }

    /**
     * Verifica si hay regiones en un chunk específico
     */
    public boolean hasRegionsInChunk(World world, int chunkX, int chunkZ) {
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) return false;

        long chunkKey = getChunkKey(chunkX, chunkZ);
        Set<Region> regions = worldIndex.get(chunkKey);
        return regions != null && !regions.isEmpty();
    }

    /**
     * Obtiene el número de regiones en un chunk específico
     */
    public int getRegionCountInChunk(World world, int chunkX, int chunkZ) {
        Map<Long, Set<Region>> worldIndex = chunkIndex.get(world);
        if (worldIndex == null) return 0;

        long chunkKey = getChunkKey(chunkX, chunkZ);
        Set<Region> regions = worldIndex.get(chunkKey);
        return regions != null ? regions.size() : 0;
    }

    /**
     * Genera clave única para un chunk usando bit shifting
     * Complejidad: O(1)
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Obtiene estadísticas del índice espacial
     */
    public SpatialIndexStats getStats() {
        int totalWorldsIndexed = chunkIndex.size();
        int totalChunksInUse = 0;
        int maxRegionsPerChunk = 0;
        int totalRegionInstances = 0;

        for (Map<Long, Set<Region>> worldIndex : chunkIndex.values()) {
            totalChunksInUse += worldIndex.size();

            for (Set<Region> regionSet : worldIndex.values()) {
                int chunkRegionCount = regionSet.size();
                totalRegionInstances += chunkRegionCount;
                maxRegionsPerChunk = Math.max(maxRegionsPerChunk, chunkRegionCount);
            }
        }

        return new SpatialIndexStats(
                totalRegions,
                totalWorldsIndexed,
                totalChunksInUse,
                maxRegionsPerChunk,
                totalRegionInstances
        );
    }

    /**
     * Valida la integridad del índice
     */
    public boolean validateIndex() {
        try {
            for (Map<Long, Set<Region>> worldIndex : chunkIndex.values()) {
                for (Map.Entry<Long, Set<Region>> chunkEntry : worldIndex.entrySet()) {
                    if (chunkEntry.getValue().isEmpty()) {
                        return false; // No debería haber chunks vacíos
                    }

                    for (Region region : chunkEntry.getValue()) {
                        if (region == null || !region.isValid()) {
                            return false; // No debería haber regiones inválidas
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Limpia el índice completamente
     */
    public void clear() {
        chunkIndex.clear();
        totalRegions = 0;
        totalChunksUsed = 0;
    }

    /**
     * Reconstruye el índice desde una colección de regiones
     */
    public void rebuild(Collection<Region> regions) {
        clear();

        for (Region region : regions) {
            if (region != null && region.isValid()) {
                addRegion(region);
            }
        }
    }

    /**
     * Obtiene información detallada para debugging
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        SpatialIndexStats stats = getStats();

        info.append("=== Spatial Index Detailed Info ===\n");
        info.append(String.format("Total regions: %d\n", stats.getTotalRegions()));
        info.append(String.format("Worlds indexed: %d\n", stats.getTotalWorldsIndexed()));
        info.append(String.format("Chunks in use: %d\n", stats.getTotalChunksInUse()));
        info.append(String.format("Max regions per chunk: %d\n", stats.getMaxRegionsPerChunk()));
        info.append(String.format("Total region instances: %d\n", stats.getTotalRegionInstances()));
        info.append(String.format("Average regions per chunk: %.2f\n", stats.getAverageRegionsPerChunk()));
        info.append(String.format("Index valid: %b\n", validateIndex()));

        // Detalles por mundo
        for (Map.Entry<World, Map<Long, Set<Region>>> worldEntry : chunkIndex.entrySet()) {
            World world = worldEntry.getKey();
            Map<Long, Set<Region>> worldIndex = worldEntry.getValue();

            info.append(String.format("\nWorld '%s': %d chunks, %d region instances\n",
                    world.getName(),
                    worldIndex.size(),
                    worldIndex.values().stream().mapToInt(Set::size).sum()));
        }

        return info.toString();
    }

    /**
     * Clase para estadísticas del índice espacial
     */
    public static class SpatialIndexStats {
        private final int totalRegions;
        private final int totalWorldsIndexed;
        private final int totalChunksInUse;
        private final int maxRegionsPerChunk;
        private final int totalRegionInstances;

        public SpatialIndexStats(int totalRegions, int totalWorldsIndexed, int totalChunksInUse,
                                 int maxRegionsPerChunk, int totalRegionInstances) {
            this.totalRegions = totalRegions;
            this.totalWorldsIndexed = totalWorldsIndexed;
            this.totalChunksInUse = totalChunksInUse;
            this.maxRegionsPerChunk = maxRegionsPerChunk;
            this.totalRegionInstances = totalRegionInstances;
        }

        public int getTotalRegions() { return totalRegions; }
        public int getTotalWorldsIndexed() { return totalWorldsIndexed; }
        public int getTotalChunksInUse() { return totalChunksInUse; }
        public int getMaxRegionsPerChunk() { return maxRegionsPerChunk; }
        public int getTotalRegionInstances() { return totalRegionInstances; }

        public double getAverageRegionsPerChunk() {
            return totalChunksInUse > 0 ? (double) totalRegionInstances / totalChunksInUse : 0.0;
        }

        /**
         * Factor de eficiencia: qué tan bien está distribuido el índice
         * Valor entre 0-1, donde 1 es óptimo (sin duplicación)
         */
        public double getEfficiencyFactor() {
            return totalRegions > 0 ? (double) totalRegions / totalRegionInstances : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "SpatialIndexStats{regions=%d, worlds=%d, chunks=%d, max_per_chunk=%d, instances=%d, avg_per_chunk=%.2f, efficiency=%.2f}",
                    totalRegions, totalWorldsIndexed, totalChunksInUse, maxRegionsPerChunk,
                    totalRegionInstances, getAverageRegionsPerChunk(), getEfficiencyFactor()
            );
        }
    }
}