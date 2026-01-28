package net.exylia.commons.v2.region.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

public class SchematicManager {

    private static SchematicManager instance;

    @Getter
    private final JavaPlugin plugin;
    private final File schematicsFolder;
    private final Map<String, Clipboard> loadedSchematics;

    private SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics/regions");
        this.loadedSchematics = new ConcurrentHashMap<>();

        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }

        logInternalInfo("SchematicManager initialized at: " + schematicsFolder.getAbsolutePath());
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new SchematicManager(plugin);
        }
    }

    public static SchematicManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SchematicManager has not been initialized");
        }
        return instance;
    }

    public CompletableFuture<Boolean> saveSchematic(Region region) {
        return saveSchematic(region, region.getId());
    }

    public CompletableFuture<Boolean> saveSchematic(Region region, String schematicName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Tasks.run(() -> {
            try {
                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(min.getWorld());
                BlockVector3 minPoint = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
                BlockVector3 maxPoint = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());

                CuboidRegion cuboidRegion = new CuboidRegion(world, minPoint, maxPoint);

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    BlockArrayClipboard clipboard = new BlockArrayClipboard(cuboidRegion);
                    clipboard.setOrigin(minPoint);

                    ForwardExtentCopy copy = new ForwardExtentCopy(
                        editSession,
                        cuboidRegion,
                        clipboard,
                        cuboidRegion.getMinimumPoint()
                    );

                    copy.setCopyingEntities(true);
                    copy.setCopyingBiomes(true);
                    Operations.complete(copy);

                    File schematicFile = new File(schematicsFolder, schematicName + ".schem");
                    try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(new FileOutputStream(schematicFile))) {
                        writer.write(clipboard);
                    }

                    loadedSchematics.put(schematicName, clipboard);

                    logInternalInfo("Schematic saved: " + schematicName + " (" + region.getVolume() + " blocks)");
                    future.complete(true);
                }

            } catch (Exception e) {
                logInternalDebug("Error saving schematic " + schematicName + ": " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        });

        return future;
    }

    public CompletableFuture<Clipboard> loadSchematic(String schematicName) {
        CompletableFuture<Clipboard> future = new CompletableFuture<>();

        if (loadedSchematics.containsKey(schematicName)) {
            future.complete(loadedSchematics.get(schematicName));
            return future;
        }

        Tasks.run(() -> {
            try {
                File schematicFile = new File(schematicsFolder, schematicName + ".schem");

                if (!schematicFile.exists()) {
                    logInternalDebug("Schematic not found: " + schematicName);
                    future.complete(null);
                    return;
                }

                Clipboard clipboard = BuiltInClipboardFormat.FAST.load(schematicFile);
                loadedSchematics.put(schematicName, clipboard);

                logInternalDebug("Schematic loaded: " + schematicName);
                future.complete(clipboard);

            } catch (Exception e) {
                logInternalDebug("Error loading schematic " + schematicName + ": " + e.getMessage());
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    public CompletableFuture<Boolean> pasteSchematic(String schematicName, Location location) {
        return loadSchematic(schematicName).thenCompose(clipboard -> {
            if (clipboard == null) {
                return CompletableFuture.completedFuture(false);
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();

            Tasks.run(() -> {
                try {
                    com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
                    BlockVector3 pasteLocation = BlockVector3.at(
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                    );

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                        editSession.setFastMode(true);

                        ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
                        Operations.complete(
                            clipboardHolder.createPaste(editSession)
                                .to(pasteLocation)
                                .ignoreAirBlocks(false)
                                .copyEntities(true)
                                .copyBiomes(true)
                                .build()
                        );

                        editSession.flushQueue();
                        logInternalDebug("Schematic pasted: " + schematicName + " at " + location);
                        future.complete(true);
                    }

                } catch (Exception e) {
                    logInternalDebug("Error pasting schematic " + schematicName + ": " + e.getMessage());
                    e.printStackTrace();
                    future.complete(false);
                }
            });

            return future;
        });
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region) {
        return regenerateRegion(region, region.getId());
    }

    public CompletableFuture<Boolean> regenerateRegion(Region region, String schematicName) {
        return loadSchematic(schematicName).thenCompose(clipboard -> {
            if (clipboard == null) {
                logInternalDebug("Cannot regenerate region " + region.getId() + ": schematic not found");
                return CompletableFuture.completedFuture(false);
            }

            Location minPoint = region.getMinimumPoint();
            return pasteSchematic(schematicName, minPoint);
        });
    }

    public boolean deleteSchematic(String schematicName) {
        File schematicFile = new File(schematicsFolder, schematicName + ".schem");

        if (schematicFile.exists()) {
            try {
                Files.delete(schematicFile.toPath());
                loadedSchematics.remove(schematicName);
                logInternalInfo("Schematic deleted: " + schematicName);
                return true;
            } catch (IOException e) {
                logInternalDebug("Error deleting schematic " + schematicName + ": " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    public boolean schematicExists(String schematicName) {
        File schematicFile = new File(schematicsFolder, schematicName + ".schem");
        return schematicFile.exists();
    }

    public void unloadSchematic(String schematicName) {
        loadedSchematics.remove(schematicName);
        logInternalDebug("Schematic unloaded from memory: " + schematicName);
    }

    public void unloadAllSchematics() {
        loadedSchematics.clear();
        logInternalInfo("All schematics unloaded from memory");
    }

    public File getSchematicsFolder() {
        return schematicsFolder;
    }

    public int getLoadedSchematicsCount() {
        return loadedSchematics.size();
    }
}
