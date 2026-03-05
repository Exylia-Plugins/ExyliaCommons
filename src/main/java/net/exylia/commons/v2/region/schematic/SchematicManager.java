package net.exylia.commons.v2.region.schematic;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.region.blocks.PlayerBlockTracker;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class SchematicManager {

    public enum SchematicType {
        AUTO,
        FAWE,
        CHUNK,
        SECTIONS,
        BLOCK
    }

    private record QueuedOperation(String schematicName, Supplier<CompletableFuture<Boolean>> action, CompletableFuture<Boolean> result) {}

    private static SchematicManager instance;

    @Getter
    private final JavaPlugin plugin;
    private final File schematicsFolder;
    private final FaweSchematicEngine faweEngine;
    private final CustomSchematicEngine customEngine;
    private volatile SchematicType defaultType;

    private final Queue<QueuedOperation> operationQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics/regions");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }

        FaweSchematicEngine fawe;
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            fawe = new FaweSchematicEngine(schematicsFolder);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            fawe = null;
        }
        this.faweEngine = fawe;
        this.customEngine = new CustomSchematicEngine(schematicsFolder);
        this.defaultType = SchematicType.AUTO;
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (SchematicManager.class) {
            if (instance != null && instance.plugin == plugin) {
                return;
            }

            if (instance != null) {
                instance.unloadAllSchematics();
            }

            instance = new SchematicManager(plugin);
        }
    }

    public static SchematicManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SchematicManager has not been initialized");
        }
        return instance;
    }

    public SchematicType getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(SchematicType defaultType) {
        this.defaultType = defaultType == null ? SchematicType.AUTO : defaultType;
    }

    public CompletableFuture<Boolean> saveSchematic(Region region) {
        return saveSchematic(region, region.getId(), defaultType, null);
    }

    public CompletableFuture<Boolean> saveSchematic(Region region, String schematicName) {
        return saveSchematic(region, schematicName, defaultType, null);
    }

    public CompletableFuture<Boolean> saveSchematic(Region region, String schematicName, SchematicType type) {
        return saveSchematic(region, schematicName, type, null);
    }

    public CompletableFuture<Boolean> saveSchematic(Region region, String schematicName, SchematicType type, Consumer<Float> onProgress) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(schematicName, "schematicName");

        SchematicType resolvedType = resolveSaveType(type, region);
        DebugAPI.logLibInfo("[Schematic] save '" + schematicName + "' engine=" + resolvedType + " volume=" + region.getVolume());
        if (resolvedType == SchematicType.FAWE) {
            return faweEngine.save(region, schematicName, onProgress);
        }
        return customEngine.save(region, schematicName, resolvedType, onProgress);
    }

    public boolean isFaweAvailable() {
        return faweEngine != null && faweEngine.isAvailable();
    }

    public CompletableFuture<Clipboard> loadSchematic(String schematicName) {
        if (faweEngine == null) return CompletableFuture.completedFuture(null);
        return faweEngine.load(schematicName);
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, Location location) {
        return pasteSchematic(schematicName, location, defaultType, null);
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, Location location, SchematicType type) {
        return pasteSchematic(schematicName, location, type, null);
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, Location location, SchematicType type, Consumer<Float> onProgress) {
        Objects.requireNonNull(schematicName, "schematicName");
        Objects.requireNonNull(location, "location");

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        int pending = operationQueue.size() + (processing.get() ? 1 : 0);
        if (pending > 0) {
            DebugAPI.logLibInfo("[Schematic] queued '" + schematicName + "' — waiting for " + pending + " schematic(s)");
        }
        operationQueue.add(new QueuedOperation(schematicName, () -> doPaste(schematicName, location, type, onProgress), future));
        drainQueue();
        return future;
    }

    private CompletableFuture<Boolean> doPaste(String schematicName, Location location, SchematicType type, Consumer<Float> onProgress) {
        SchematicType resolvedType = resolvePasteType(schematicName, type);
        if (resolvedType == SchematicType.FAWE) {
            DebugAPI.logLibInfo("[Schematic] paste '" + schematicName + "' engine=FAWE");
            return faweEngine.paste(schematicName, location, onProgress);
        }

        return customEngine.load(schematicName).thenCompose(schematic -> {
            if (schematic == null) {
                return CompletableFuture.completedFuture(false);
            }

            SchematicType applyType = resolvedType == SchematicType.AUTO ? schematic.storedType() : resolvedType;
            if (applyType == SchematicType.AUTO || applyType == SchematicType.FAWE) {
                applyType = SchematicType.SECTIONS;
            }

            DebugAPI.logLibInfo("[Schematic] paste '" + schematicName + "' engine=" + applyType);
            return customEngine.paste(schematicName, schematic, location, applyType, onProgress, null);
        });
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        return regenerateRegion(region, region.getId(), defaultType, true);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region, String schematicName) {
        return regenerateRegion(region, schematicName, defaultType, true);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region, String schematicName, SchematicType type) {
        return regenerateRegion(region, schematicName, type, true);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region, String schematicName, SchematicType type, boolean teleportToAir) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(schematicName, "schematicName");

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        int pending = operationQueue.size() + (processing.get() ? 1 : 0);
        if (pending > 0) {
            DebugAPI.logLibInfo("[Schematic] queued '" + schematicName + "' — waiting for " + pending + " schematic(s)");
        }
        operationQueue.add(new QueuedOperation(schematicName, () -> doRegenerate(region, schematicName, type, teleportToAir), future));
        drainQueue();
        return future;
    }

    private void drainQueue() {
        if (!processing.compareAndSet(false, true)) return;
        pollAndRun();
    }

    private void pollAndRun() {
        QueuedOperation next = operationQueue.poll();
        if (next == null) {
            processing.set(false);
            DebugAPI.logLibInfo("[Schematic] queue empty — all operations complete");
            return;
        }
        int remaining = operationQueue.size();
        String suffix = remaining > 0 ? " (" + remaining + " more in queue)" : "";
        DebugAPI.logLibInfo("[Schematic] starting '" + next.schematicName() + "'" + suffix);
        next.action().get().whenComplete((result, ex) -> {
            if (ex != null) {
                next.result().completeExceptionally(ex);
                DebugAPI.logLibInfo("[Schematic] failed '" + next.schematicName() + "'");
            } else {
                next.result().complete(result);
                DebugAPI.logLibInfo("[Schematic] finished '" + next.schematicName() + "'");
            }
            pollAndRun();
        });
    }

    private CompletableFuture<Boolean> doRegenerate(Region region, String schematicName, SchematicType type, boolean teleportToAir) {
        Location minPoint = region.getMinimumPoint();
        World world = minPoint.getWorld();

        if (PlayerBlockTracker.isInitialized()) {
            PlayerBlockTracker.getInstance().clearRegionBlocks(region.getId());
        }

        CompletableFuture<Void> cleanup = (world != null)
            ? cleanEntitiesInRegion(region, world)
            : CompletableFuture.completedFuture(null);

        SchematicType resolvedType = resolvePasteType(schematicName, type);

        return cleanup.thenCompose(v -> {
            if (resolvedType == SchematicType.FAWE) {
                CompletableFuture<Void> teleport = (teleportToAir && world != null)
                    ? preTeleportPlayersInRegion(region, world)
                    : CompletableFuture.completedFuture(null);
                return teleport.thenCompose(v2 -> {
                    DebugAPI.logLibInfo("[Schematic] regenerate '" + schematicName + "' engine=FAWE");
                    return faweEngine.paste(schematicName, minPoint, null);
                });
            }

            return customEngine.load(schematicName).thenCompose(schematic -> {
                if (schematic == null) {
                    return CompletableFuture.completedFuture(false);
                }

                SchematicType applyType = resolvedType == SchematicType.AUTO ? schematic.storedType() : resolvedType;
                if (applyType == SchematicType.AUTO || applyType == SchematicType.FAWE) {
                    applyType = SchematicType.SECTIONS;
                }

                int baseX = minPoint.getBlockX() - schematic.anchorX();
                int baseY = minPoint.getBlockY() - schematic.anchorY();
                int baseZ = minPoint.getBlockZ() - schematic.anchorZ();

                int topY = baseY + schematic.height() - 1;
                final CustomSchematic finalSchematic = schematic;
                BiConsumer<Integer, Integer> safetyConsumer = (teleportToAir && world != null) ? (chunkX, chunkZ) -> {
                    for (Player player : world.getPlayers()) {
                        if (!player.isOnline() || !player.getWorld().equals(world)) continue;
                        Location loc = player.getLocation();
                        int py = loc.getBlockY();
                        if ((loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ
                                && py >= baseY && py <= topY && isSuffocating(player)) {
                            teleportToSafeAir(player, finalSchematic, baseX, baseY, baseZ);
                        }
                    }
                } : null;

                DebugAPI.logLibInfo("[Schematic] regenerate '" + schematicName + "' engine=" + applyType);
                return customEngine.paste(schematicName, schematic, minPoint, applyType, null, safetyConsumer);
            });
        });
    }

    private static CompletableFuture<Void> cleanEntitiesInRegion(Region region, World world) {
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();
        BoundingBox box = new BoundingBox(
            min.getX(), min.getY(), min.getZ(),
            max.getX() + 1, max.getY() + 1, max.getZ() + 1
        );

        int minCX = min.getBlockX() >> 4;
        int maxCX = max.getBlockX() >> 4;
        int minCZ = min.getBlockZ() >> 4;
        int maxCZ = max.getBlockZ() >> 4;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                final int chunkX = cx;
                final int chunkZ = cz;
                CompletableFuture<Void> chunkFuture = world.getChunkAtAsync(chunkX, chunkZ)
                    .thenAccept(chunk -> {
                        for (Entity entity : chunk.getEntities()) {
                            if (!(entity instanceof Player) && box.contains(entity.getLocation().toVector())) {
                                entity.remove();
                            }
                        }
                    });
                futures.add(chunkFuture);
            }
        }

        if (futures.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> preTeleportPlayersInRegion(Region region, World world) {
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();
        int minCX = min.getBlockX() >> 4;
        int maxCX = max.getBlockX() >> 4;
        int minCZ = min.getBlockZ() >> 4;
        int maxCZ = max.getBlockZ() >> 4;
        int regionMinY = min.getBlockY();
        int regionMaxY = max.getBlockY();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            int py = loc.getBlockY();
            if (cx >= minCX && cx <= maxCX && cz >= minCZ && cz <= maxCZ
                    && py >= regionMinY && py <= regionMaxY && isSuffocating(player)) {
                CompletableFuture<Void> f = new CompletableFuture<>();
                Tasks.at(player, () -> {
                    if (isStillInRegion(player, world, minCX, maxCX, minCZ, maxCZ, regionMinY, regionMaxY)) {
                        teleportAboveRegion(player, regionMaxY);
                    }
                    f.complete(null);
                });
                futures.add(f);
            }
        }

        if (futures.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static void teleportToSafeAir(Player player, CustomSchematic schematic, int baseX, int baseY, int baseZ) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        int px = loc.getBlockX();
        int pz = loc.getBlockZ();

        int lx = px - baseX;
        int lz = pz - baseZ;

        if (lx >= 0 && lx < schematic.width() && lz >= 0 && lz < schematic.length()) {
            int startLY = Math.max(loc.getBlockY() - baseY, 0);
            for (int ly = startLY; ly < schematic.height() - 1; ly++) {
                if (isSchematicAir(schematic, lx, ly, lz) && isSchematicAir(schematic, lx, ly + 1, lz)) {
                    player.teleportAsync(new Location(world, loc.getX(), baseY + ly, loc.getZ(), loc.getYaw(), loc.getPitch()));
                    return;
                }
            }
        }

        teleportAboveRegion(player, baseY + schematic.height() - 1);
    }

    private static boolean isSchematicAir(CustomSchematic schematic, int lx, int ly, int lz) {
        int idx = SchematicMath.toIndex(lx, ly, lz, schematic.width(), schematic.length());
        String block = schematic.palette().get(schematic.data()[idx]);
        return block.equals("minecraft:air") || block.equals("minecraft:cave_air") || block.equals("minecraft:void_air");
    }

    private static boolean isStillInRegion(Player player, World expectedWorld,
                                              int minCX, int maxCX, int minCZ, int maxCZ,
                                              int regionMinY, int regionMaxY) {
        if (!player.isOnline() || !player.getWorld().equals(expectedWorld)) return false;
        Location loc = player.getLocation();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        int py = loc.getBlockY();
        return cx >= minCX && cx <= maxCX && cz >= minCZ && cz <= maxCZ
                && py >= regionMinY && py <= regionMaxY;
    }

    private static boolean isSuffocating(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return world.getBlockAt(x, y, z).getType().isSolid()
                || world.getBlockAt(x, y + 1, z).getType().isSolid();
    }

    private static void teleportAboveRegion(Player player, int regionMaxY) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int maxY = world.getMaxHeight() - 1;

        for (int y = regionMaxY + 1; y < maxY; y++) {
            if (world.getBlockAt(x, y, z).getType().isAir() &&
                world.getBlockAt(x, y + 1, z).getType().isAir()) {
                player.teleportAsync(new Location(world, loc.getX(), y, loc.getZ(), loc.getYaw(), loc.getPitch()));
                return;
            }
        }
    }

    public boolean deleteSchematic(String schematicName) {
        boolean deletedFawe = faweEngine != null && faweEngine.delete(schematicName);
        boolean deletedCustom = customEngine.delete(schematicName);
        return deletedFawe || deletedCustom;
    }

    public boolean schematicExists(String schematicName) {
        return (faweEngine != null && faweEngine.exists(schematicName)) || customEngine.exists(schematicName);
    }

    public void unloadSchematic(String schematicName) {
        if (faweEngine != null) faweEngine.unload(schematicName);
        customEngine.unload(schematicName);
    }

    public void unloadAllSchematics() {
        if (faweEngine != null) faweEngine.unloadAll();
        customEngine.unloadAll();
    }

    public void shutdown() {
        QueuedOperation pending;
        while ((pending = operationQueue.poll()) != null) {
            pending.result().cancel(true);
        }
        unloadAllSchematics();
        synchronized (SchematicManager.class) {
            if (instance == this) {
                instance = null;
            }
        }
    }

    public int getQueueSize() {
        return operationQueue.size() + (processing.get() ? 1 : 0);
    }

    public File getSchematicsFolder() {
        return schematicsFolder;
    }

    public int getLoadedSchematicsCount() {
        return (faweEngine != null ? faweEngine.loadedCount() : 0) + customEngine.loadedCount();
    }

    private SchematicType resolveSaveType(SchematicType requested, Region region) {
        SchematicType base = requested == null ? defaultType : requested;
        if (base != SchematicType.AUTO) {
            if (base == SchematicType.FAWE && !isFaweAvailable()) {
                DebugAPI.logLibInfo("[Schematic] FAWE requested but unavailable, falling back to SECTIONS");
                return SchematicType.SECTIONS;
            }
            return base;
        }

        long volume = region.getVolume();
        if (volume >= 1_500_000L) {
            return SchematicType.CHUNK;
        }
        if (volume >= 200_000L) {
            return SchematicType.SECTIONS;
        }
        return SchematicType.BLOCK;
    }

    private SchematicType resolvePasteType(String schematicName, SchematicType requested) {
        SchematicType base = requested == null ? defaultType : requested;
        if (base == SchematicType.FAWE && !isFaweAvailable()) {
            DebugAPI.logLibInfo("[Schematic] FAWE requested but unavailable, falling back to SECTIONS");
            return SchematicType.SECTIONS;
        }
        return base;
    }
}
