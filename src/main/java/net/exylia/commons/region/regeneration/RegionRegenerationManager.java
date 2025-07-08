package net.exylia.commons.region.regeneration;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import net.exylia.commons.region.blocks.PlayerBlockTracker;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.exylia.commons.ExyliaPlugin.debugEnabled;

/**
 * Manager para regeneración de regiones integrado en el sistema de regiones
 */
public class RegionRegenerationManager {
    private static RegionRegenerationManager instance;

    private final JavaPlugin plugin;
    private final File schemsFolder;
    private final ConcurrentMap<String, Clipboard> clipboardCache;
    private final ConcurrentMap<String, Boolean> regeneratingRegions;

    // Configuración
    private static final int MAX_CACHE_SIZE = 30;
    private static final boolean CACHE_ENABLED = true;

    private RegionRegenerationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schemsFolder = new File(plugin.getDataFolder(), "schematics");
        this.clipboardCache = new ConcurrentHashMap<>();
        this.regeneratingRegions = new ConcurrentHashMap<>();

        // Crear directorio si no existe
        if (!schemsFolder.exists()) {
            boolean created = schemsFolder.mkdirs();
            if (created) {
                plugin.getLogger().info("Directorio de schematics de regiones creado: " + schemsFolder.getPath());
            }
        }
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionRegenerationManager(plugin);
        }
    }

    public static RegionRegenerationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionRegenerationManager no ha sido inicializado");
        }
        return instance;
    }

    /**
     * Guarda el estado actual de una región como schematic
     */
    public CompletableFuture<Boolean> saveRegionSchematic(Region region) {
        if (!region.isValid()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Guardando schematic para región: " + region.getId());

                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                // Convertir a WorldEdit
                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(min.getWorld());
                BlockVector3 minVec = BlockVector3.at(
                        Math.min(min.getBlockX(), max.getBlockX()),
                        Math.min(min.getBlockY(), max.getBlockY()),
                        Math.min(min.getBlockZ(), max.getBlockZ())
                );
                BlockVector3 maxVec = BlockVector3.at(
                        Math.max(min.getBlockX(), max.getBlockX()),
                        Math.max(min.getBlockY(), max.getBlockY()),
                        Math.max(min.getBlockZ(), max.getBlockZ())
                );

                CuboidRegion worldEditRegion = new CuboidRegion(weWorld, minVec, maxVec);

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    editSession.setFastMode(true);

                    Clipboard clipboard = new BlockArrayClipboard(worldEditRegion);
                    ForwardExtentCopy copy = new ForwardExtentCopy(editSession, worldEditRegion, clipboard, worldEditRegion.getMinimumPoint());
                    copy.setCopyingEntities(false);
                    Operations.complete(copy);

                    // Guardar archivo
                    File schematicFile = getSchematicFile(region);
                    try (FileOutputStream fos = new FileOutputStream(schematicFile);
                         ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(fos)) {

                        writer.write(clipboard);

                        // Cachear si está habilitado
                        if (CACHE_ENABLED) {
                            cacheClipboard(region, clipboard);
                        }

                        plugin.getLogger().info("Schematic guardado: " + schematicFile.getName());
                        return true;
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error guardando schematic para región " + region.getId() + ": " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Regenera una región usando su schematic guardado
     */
    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        String regionKey = getRegionKey(region);

        // Verificar si ya se está regenerando
        if (regeneratingRegions.containsKey(regionKey)) {
            return CompletableFuture.completedFuture(false);
        }

        File schematicFile = getSchematicFile(region);
        if (!schematicFile.exists()) {
            return CompletableFuture.completedFuture(false);
        }

        regeneratingRegions.put(regionKey, true);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DebugUtils.logInternalDebug(debugEnabled(),"Regenerando región: " + region.getId());

                // Intentar obtener del cache
                Clipboard clipboard = clipboardCache.get(regionKey);

                if (clipboard == null) {
                    // Cargar desde archivo
                    clipboard = loadSchematicFromFile(schematicFile);
                    if (clipboard == null) {
                        return false;
                    }

                    // Cachear para próximas regeneraciones
                    if (CACHE_ENABLED) {
                        cacheClipboard(region, clipboard);
                    }
                }

                // Ejecutar regeneración
                return executeRegeneration(region, clipboard);

            } catch (Exception e) {
                plugin.getLogger().severe("Error regenerando región " + region.getId() + ": " + e.getMessage());
                return false;
            } finally {
                regeneratingRegions.remove(regionKey);
            }
        });
    }

    /**
     * Limpia solo las entidades de una región
     */
    public CompletableFuture<Integer> cleanRegionEntities(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    int removed = 0;
                    Location min = region.getMinimumPoint();
                    Location max = region.getMaximumPoint();

                    // Calcular área de limpieza
                    double minX = Math.min(min.getX(), max.getX());
                    double maxX = Math.max(min.getX(), max.getX());
                    double minY = Math.min(min.getY(), max.getY());
                    double maxY = Math.max(min.getY(), max.getY());
                    double minZ = Math.min(min.getZ(), max.getZ());
                    double maxZ = Math.max(min.getZ(), max.getZ());

                    // Limpiar entidades en el área
                    for (Entity entity : min.getWorld().getEntities()) {
                        if (entity instanceof Player) continue; // No remover jugadores

                        Location loc = entity.getLocation();
                        if (loc.getX() >= minX && loc.getX() <= maxX &&
                                loc.getY() >= minY && loc.getY() <= maxY &&
                                loc.getZ() >= minZ && loc.getZ() <= maxZ) {

                            // Solo remover ciertos tipos de entidades
                            if (shouldRemoveEntity(entity)) {
                                entity.remove();
                                removed++;
                            }
                        }
                    }

                    DebugUtils.logInternalDebug(debugEnabled(), "Limpiadas " + removed + " entidades en región " + region.getId());
                    return removed;

                }).get();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error limpiando entidades en región " + region.getId() + ": " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * Verifica si una región tiene schematic guardado
     */
    public boolean hasSchematic(Region region) {
        return getSchematicFile(region).exists();
    }

    /**
     * Verifica si una región está siendo regenerada
     */
    public boolean isRegenerating(Region region) {
        return regeneratingRegions.containsKey(getRegionKey(region));
    }

    /**
     * Elimina el schematic de una región
     */
    public CompletableFuture<Boolean> deleteRegionSchematic(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            File schematicFile = getSchematicFile(region);
            String regionKey = getRegionKey(region);

            // Remover del cache
            clipboardCache.remove(regionKey);

            if (schematicFile.exists()) {
                boolean deleted = schematicFile.delete();
                if (deleted) {
                    plugin.getLogger().info("Schematic eliminado para región: " + region.getId());
                }
                return deleted;
            }

            return true;
        });
    }

    /**
     * Obtiene estadísticas del sistema
     */
    public RegenerationStats getStats() {
        return new RegenerationStats(
                clipboardCache.size(),
                regeneratingRegions.size(),
                schemsFolder.listFiles() != null ? schemsFolder.listFiles().length : 0
        );
    }

    /**
     * Limpia el cache de clipboards
     */
    public void clearCache() {
        int size = clipboardCache.size();
        clipboardCache.clear();
        plugin.getLogger().info("Cache de clipboards limpiado: " + size + " elementos");
    }

    // Métodos privados

    private Clipboard loadSchematicFromFile(File schematicFile) {
        try (FileInputStream fis = new FileInputStream(schematicFile);
             ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(fis)) {

            return reader.read();

        } catch (IOException e) {
            plugin.getLogger().severe("Error cargando schematic " + schematicFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void cacheClipboard(Region region, Clipboard clipboard) {
        String regionKey = getRegionKey(region);

        if (clipboardCache.size() >= MAX_CACHE_SIZE) {
            // Remover el más antiguo (FIFO simple)
            String oldestKey = clipboardCache.keySet().iterator().next();
            clipboardCache.remove(oldestKey);
        }

        clipboardCache.put(regionKey, clipboard);
        DebugUtils.logInternalDebug(debugEnabled(),"Clipboard cacheado para región: " + region.getId());
    }

    private boolean executeRegeneration(Region region, Clipboard clipboard) {
        try {
            Location min = region.getMinimumPoint();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(min.getWorld());

            BlockVector3 pastePosition = BlockVector3.at(
                    Math.min(region.getMinimumPoint().getBlockX(), region.getMaximumPoint().getBlockX()),
                    Math.min(region.getMinimumPoint().getBlockY(), region.getMaximumPoint().getBlockY()),
                    Math.min(region.getMinimumPoint().getBlockZ(), region.getMaximumPoint().getBlockZ())
            );

            // Ejecutar en hilo principal
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // 1. Limpiar entidades primero
                    cleanRegionEntities(region).thenRun(() -> {

                        // 2. Limpiar bloques de jugador del registro
                        String regionKey = getRegionKey(region);
                        PlayerBlockTracker.getInstance().clearRegionBlocks(regionKey);

                        // 3. Esperar un tick y regenerar bloques
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                                editSession.setFastMode(true);

                                ClipboardHolder holder = new ClipboardHolder(clipboard);
                                Operation operation = holder
                                        .createPaste(editSession)
                                        .to(pastePosition)
                                        .ignoreAirBlocks(false)
                                        .build();

                                Operations.complete(operation);

                                DebugUtils.logInternalDebug(debugEnabled(), "Región regenerada exitosamente: " + region.getId());
                                future.complete(true);

                            } catch (WorldEditException e) {
                                plugin.getLogger().severe("Error de WorldEdit regenerando región " + region.getId() + ": " + e.getMessage());
                                future.complete(false);
                            }
                        }, 2L);
                    });

                } catch (Exception e) {
                    plugin.getLogger().severe("Error en regeneración de región " + region.getId() + ": " + e.getMessage());
                    future.complete(false);
                }
            });

            return future.get();

        } catch (Exception e) {
            plugin.getLogger().severe("Error inesperado regenerando región " + region.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Limpia solo los bloques de jugador de una región sin regenerar
     */
    public CompletableFuture<Integer> clearRegionPlayerBlocks(Region region) {
        return CompletableFuture.supplyAsync(() -> {
            String regionKey = getRegionKey(region);
            PlayerBlockTracker tracker = PlayerBlockTracker.getInstance();

            // Obtener estadísticas antes
            int blocksBefore = tracker.getPlayerBlocks(regionKey).size();

            // Limpiar bloques
            tracker.clearRegionBlocks(regionKey);

            plugin.getLogger().info("Limpiados " + blocksBefore + " bloques de jugador en región " + region.getId());
            return blocksBefore;
        });
    }

    private boolean shouldRemoveEntity(Entity entity) {
        EntityType type = entity.getType();

        return type != EntityType.PLAYER;
    }

    private String getRegionKey(Region region) {
        return region.getPluginName() + ":" + region.getId();
    }

    private File getSchematicFile(Region region) {
        return new File(schemsFolder, getRegionKey(region).replace(":", "_") + ".schem");
    }

    /**
     * Limpia recursos al cerrar
     */
    public void shutdown() {
        // Esperar regeneraciones activas
        if (!regeneratingRegions.isEmpty()) {
            DebugUtils.logInternalInfo("Esperando " + regeneratingRegions.size() + " regeneraciones...");
            int attempts = 0;
            while (!regeneratingRegions.isEmpty() && attempts < 10) {
                try {
                    Thread.sleep(500);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        clearCache();
        regeneratingRegions.clear();
    }

    /**
     * Clase para estadísticas
     */
    @Getter
    public static class RegenerationStats {
        private final int cachedClipboards;
        private final int activeRegenerations;
        private final int totalSchematics;

        public RegenerationStats(int cachedClipboards, int activeRegenerations, int totalSchematics) {
            this.cachedClipboards = cachedClipboards;
            this.activeRegenerations = activeRegenerations;
            this.totalSchematics = totalSchematics;
        }

    }
}