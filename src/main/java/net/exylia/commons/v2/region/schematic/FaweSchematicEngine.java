package net.exylia.commons.v2.region.schematic;
import net.exylia.commons.v2.debug.api.DebugAPI;

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
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


final class FaweSchematicEngine {

    private final File folder;
    private final Map<String, Clipboard> loadedSchematics;

    FaweSchematicEngine(File folder) {
        this.folder = folder;
        this.loadedSchematics = new ConcurrentHashMap<>();
    }

    CompletableFuture<Boolean> save(Region region, String schematicName, Consumer<Float> onProgress) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (!isAvailable()) {
            future.complete(false);
            return future;
        }

        Tasks.sync(() -> {
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

                    ForwardExtentCopy copy = new ForwardExtentCopy(editSession, cuboidRegion, clipboard, cuboidRegion.getMinimumPoint());
                    copy.setCopyingEntities(true);
                    copy.setCopyingBiomes(true);
                    Operations.complete(copy);

                    File schematicFile = schematicFile(schematicName);
                    try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(new FileOutputStream(schematicFile))) {
                        writer.write(clipboard);
                    }

                    loadedSchematics.put(schematicName, clipboard);
                    DebugAPI.logLibInfo("FAWE schematic saved: " + schematicName + " (" + region.getVolume() + " blocks)");
                    if (onProgress != null) onProgress.accept(1f);
                    future.complete(true);
                }
            } catch (Exception e) {
                DebugAPI.logLibDebug("Error saving FAWE schematic " + schematicName + ": " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    CompletableFuture<Clipboard> load(String schematicName) {
        Clipboard cached = loadedSchematics.get(schematicName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<Clipboard> future = new CompletableFuture<>();
        Tasks.ioRun(() -> {
            try {
                File file = schematicFile(schematicName);
                if (!file.exists()) {
                    future.complete(null);
                    return;
                }

                Clipboard clipboard = BuiltInClipboardFormat.FAST.load(file);
                loadedSchematics.put(schematicName, clipboard);
                future.complete(clipboard);
            } catch (Exception e) {
                DebugAPI.logLibDebug("Error loading FAWE schematic " + schematicName + ": " + e.getMessage());
                future.complete(null);
            }
        });
        return future;
    }

    CompletableFuture<Boolean> paste(String schematicName, Location location, Consumer<Float> onProgress) {
        return load(schematicName).thenCompose(clipboard -> {
            if (clipboard == null) {
                return CompletableFuture.completedFuture(false);
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            Tasks.sync(() -> {
                try {
                    com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
                    BlockVector3 pasteLocation = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                        editSession.setFastMode(true);
                        ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
                        Operations.complete(clipboardHolder.createPaste(editSession)
                            .to(pasteLocation)
                            .ignoreAirBlocks(false)
                            .copyEntities(true)
                            .copyBiomes(true)
                            .build());
                        editSession.flushQueue();
                        if (onProgress != null) onProgress.accept(1f);
                        future.complete(true);
                    }
                } catch (Exception e) {
                    DebugAPI.logLibDebug("Error pasting FAWE schematic " + schematicName + ": " + e.getMessage());
                    future.complete(false);
                }
            });

            return future;
        });
    }

    boolean delete(String schematicName) {
        File file = schematicFile(schematicName);
        if (!file.exists()) {
            loadedSchematics.remove(schematicName);
            return false;
        }

        try {
            Files.delete(file.toPath());
            loadedSchematics.remove(schematicName);
            DebugAPI.logLibInfo("Schematic deleted: " + schematicName);
            return true;
        } catch (IOException e) {
            DebugAPI.logLibDebug("Error deleting FAWE schematic " + schematicName + ": " + e.getMessage());
            return false;
        }
    }

    boolean exists(String schematicName) {
        return schematicFile(schematicName).exists();
    }

    void unload(String schematicName) {
        loadedSchematics.remove(schematicName);
    }

    void unloadAll() {
        loadedSchematics.clear();
    }

    int loadedCount() {
        return loadedSchematics.size();
    }

    boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null
            || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    private File schematicFile(String name) {
        return new File(folder, name + ".schem");
    }
}
