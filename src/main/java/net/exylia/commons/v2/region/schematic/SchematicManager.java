package net.exylia.commons.v2.region.schematic;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

public class SchematicManager {

    public enum SchematicType {
        AUTO,
        FAWE,
        CHUNK,
        SECTIONS,
        BLOCK
    }

    private static SchematicManager instance;

    @Getter
    private final JavaPlugin plugin;
    private final File schematicsFolder;
    private final FaweSchematicEngine faweEngine;
    private final CustomSchematicEngine customEngine;
    private volatile SchematicType defaultType;

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
        logInternalInfo("[Schematic] save '" + schematicName + "' engine=" + resolvedType + " volume=" + region.getVolume());
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

        SchematicType resolvedType = resolvePasteType(schematicName, type);
        if (resolvedType == SchematicType.FAWE) {
            logInternalInfo("[Schematic] paste '" + schematicName + "' engine=FAWE");
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

            logInternalInfo("[Schematic] paste '" + schematicName + "' engine=" + applyType);
            return customEngine.paste(schematicName, schematic, location, applyType, onProgress, null);
        });
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        return regenerateRegion(region, region.getId(), defaultType);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region, String schematicName) {
        return regenerateRegion(region, schematicName, defaultType);
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region, String schematicName, SchematicType type) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(schematicName, "schematicName");

        Location minPoint = region.getMinimumPoint();
        World world = minPoint.getWorld();

        if (world != null) {
            cleanEntitiesInRegion(region, world);
        }

        SchematicType resolvedType = resolvePasteType(schematicName, type);

        if (resolvedType == SchematicType.FAWE) {
            if (world != null) {
                preTeleportPlayersInRegion(region, world);
            }
            logInternalInfo("[Schematic] regenerate '" + schematicName + "' engine=FAWE");
            return faweEngine.paste(schematicName, minPoint, null);
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

            BiConsumer<Integer, Integer> safetyConsumer = world == null ? null : (chunkX, chunkZ) -> {
                for (Player player : world.getPlayers()) {
                    Location loc = player.getLocation();
                    if ((loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ) {
                        teleportToSafeAir(player, schematic, baseX, baseY, baseZ);
                    }
                }
            };

            logInternalInfo("[Schematic] regenerate '" + schematicName + "' engine=" + applyType);
            return customEngine.paste(schematicName, schematic, minPoint, applyType, null, safetyConsumer);
        });
    }

    private static void cleanEntitiesInRegion(Region region, World world) {
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();
        BoundingBox box = new BoundingBox(
            min.getX(), min.getY(), min.getZ(),
            max.getX() + 1, max.getY() + 1, max.getZ() + 1
        );
        world.getNearbyEntities(box, e -> !(e instanceof Player)).forEach(Entity::remove);
    }

    private void preTeleportPlayersInRegion(Region region, World world) {
        Location min = region.getMinimumPoint();
        Location max = region.getMaximumPoint();
        int minChunkX = min.getBlockX() >> 4;
        int maxChunkX = max.getBlockX() >> 4;
        int minChunkZ = min.getBlockZ() >> 4;
        int maxChunkZ = max.getBlockZ() >> 4;
        int regionMaxY = max.getBlockY();

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (cx >= minChunkX && cx <= maxChunkX && cz >= minChunkZ && cz <= maxChunkZ) {
                teleportAboveRegion(player, regionMaxY);
            }
        }
    }

    private static void teleportToSafeAir(Player player, CustomSchematic schematic, int baseX, int baseY, int baseZ) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();

        int lx = px - baseX;
        int lz = pz - baseZ;

        if (lx >= 0 && lx < schematic.width() && lz >= 0 && lz < schematic.length()) {
            int startLY = Math.max(py - baseY, 0);
            for (int ly = startLY; ly < schematic.height() - 1; ly++) {
                if (isSchematicAir(schematic, lx, ly, lz) && isSchematicAir(schematic, lx, ly + 1, lz)) {
                    player.teleport(new Location(world, loc.getX(), baseY + ly, loc.getZ(), loc.getYaw(), loc.getPitch()));
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

    private static void teleportAboveRegion(Player player, int regionMaxY) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int maxY = world.getMaxHeight() - 1;

        for (int y = regionMaxY + 1; y < maxY; y++) {
            if (world.getBlockAt(x, y, z).getType().isAir() &&
                world.getBlockAt(x, y + 1, z).getType().isAir()) {
                player.teleport(new Location(world, loc.getX(), y, loc.getZ(), loc.getYaw(), loc.getPitch()));
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
        unloadAllSchematics();
        synchronized (SchematicManager.class) {
            if (instance == this) {
                instance = null;
            }
        }
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
                logInternalInfo("[Schematic] FAWE requested but unavailable, falling back to SECTIONS");
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
            logInternalInfo("[Schematic] FAWE requested but unavailable, falling back to SECTIONS");
            return SchematicType.SECTIONS;
        }
        return base;
    }
}
