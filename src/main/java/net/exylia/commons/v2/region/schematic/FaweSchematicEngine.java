package net.exylia.commons.v2.region.schematic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import com.sk89q.worldedit.world.block.BlockState;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


final class FaweSchematicEngine {

    private final File folder;
    private final Cache<String, Clipboard> loadedSchematics;

    FaweSchematicEngine(File folder) {
        this.folder = folder;
        this.loadedSchematics = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
    }

    CompletableFuture<Boolean> save(Region region, String schematicName, Consumer<Float> onProgress) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (!isAvailable()) {
            future.complete(false);
            return future;
        }

        Tasks.ioRun(() -> {
            try {
                Location min = region.getMinimumPoint();
                Location max = region.getMaximumPoint();

                com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(min.getWorld());
                BlockVector3 minPoint = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
                BlockVector3 maxPoint = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());

                CuboidRegion cuboidRegion = new CuboidRegion(world, minPoint, maxPoint);

                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(world)
                        .maxBlocks(-1)
                        .build()) {
                    editSession.disableHistory();

                    BlockArrayClipboard clipboard = new BlockArrayClipboard(cuboidRegion);
                    clipboard.setOrigin(minPoint);

                    ForwardExtentCopy copy = new ForwardExtentCopy(editSession, cuboidRegion, clipboard, cuboidRegion.getMinimumPoint());
                    copy.setCopyingEntities(false);
                    copy.setCopyingBiomes(false);
                    Operations.complete(copy);

                    File schematicFile = schematicFile(schematicName);
                    try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(new FileOutputStream(schematicFile))) {
                        writer.write(clipboard);
                    }

                    loadedSchematics.put(schematicName, clipboard);
                    DebugAPI.logLibDebug("FAWE schematic saved: " + schematicName + " (" + region.getVolume() + " blocks)");
                    if (onProgress != null) onProgress.accept(1f);
                    future.complete(true);
                }
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.REGION, "Error saving FAWE schematic " + schematicName + ": " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    CompletableFuture<Clipboard> load(String schematicName) {
        Clipboard cached = loadedSchematics.getIfPresent(schematicName);
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
                DebugAPI.logLibDebug(DebugCategory.REGION, "Error loading FAWE schematic " + schematicName + ": " + e.getMessage());
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
            Tasks.ioRun(() -> {
                try {
                    com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
                    BlockVector3 pasteLocation = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                    try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                            .world(world)
                            .maxBlocks(-1)
                            .build()) {
                        editSession.disableHistory();
                        ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
                        Operations.complete(clipboardHolder.createPaste(editSession)
                            .to(pasteLocation)
                            .ignoreAirBlocks(false)
                            .copyEntities(false)
                            .copyBiomes(false)
                            .build());
                        editSession.flushQueue();
                        if (onProgress != null) onProgress.accept(1f);
                        future.complete(true);
                    }
                } catch (Exception e) {
                    DebugAPI.logLibError(DebugCategory.REGION, "Error pasting FAWE schematic " + schematicName + ": " + e.getMessage());
                    future.complete(false);
                }
            });

            return future;
        });
    }

    CompletableFuture<Boolean> saveFromCustom(CustomSchematic schematic, String schematicName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (!isAvailable()) {
            DebugAPI.logLibDebug("[Schematic] saveFromCustom '" + schematicName + "': FAWE/WorldEdit not available");
            future.complete(false);
            return future;
        }

        DebugAPI.logLibDebug("[Schematic] saveFromCustom '" + schematicName + "': starting conversion ("
            + schematic.width() + "x" + schematic.height() + "x" + schematic.length() + ")");

        Tasks.ioRun(() -> {
            try {
                CuboidRegion region = new CuboidRegion(
                    BlockVector3.at(0, 0, 0),
                    BlockVector3.at(schematic.width() - 1, schematic.height() - 1, schematic.length() - 1)
                );

                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(BlockVector3.at(schematic.anchorX(), schematic.anchorY(), schematic.anchorZ()));

                BlockData[] paletteData = schematic.getOrComputePaletteData();
                BlockState[] weStates = new BlockState[paletteData.length];
                for (int i = 0; i < paletteData.length; i++) {
                    weStates[i] = BukkitAdapter.adapt(paletteData[i]);
                }

                int width = schematic.width();
                int height = schematic.height();
                int length = schematic.length();
                short[] data = schematic.data();

                for (int lx = 0; lx < width; lx++) {
                    for (int ly = 0; ly < height; ly++) {
                        for (int lz = 0; lz < length; lz++) {
                            int pi = data[SchematicMath.toIndex(lx, ly, lz, width, length)] & 0xFFFF;
                            clipboard.setBlock(BlockVector3.at(lx, ly, lz), weStates[pi]);
                        }
                    }
                }

                File file = schematicFile(schematicName);
                try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(new FileOutputStream(file))) {
                    writer.write(clipboard);
                }

                loadedSchematics.put(schematicName, clipboard);
                DebugAPI.logLibDebug("[Schematic] Migrated '" + schematicName + "' .xschem → .schem");
                future.complete(true);
            } catch (Exception e) {
                DebugAPI.logLibError(DebugCategory.REGION, "Error migrating schematic '" + schematicName + "': " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    boolean delete(String schematicName) {
        File file = schematicFile(schematicName);
        if (!file.exists()) {
            loadedSchematics.invalidate(schematicName);
            return false;
        }

        try {
            Files.delete(file.toPath());
            loadedSchematics.invalidate(schematicName);
            DebugAPI.logLibDebug("Schematic deleted: " + schematicName);
            return true;
        } catch (IOException e) {
            DebugAPI.logLibDebug(DebugCategory.REGION, "Error deleting FAWE schematic " + schematicName + ": " + e.getMessage());
            return false;
        }
    }

    boolean exists(String schematicName) {
        return schematicFile(schematicName).exists();
    }

    void unload(String schematicName) {
        loadedSchematics.invalidate(schematicName);
    }

    void unloadAll() {
        loadedSchematics.invalidateAll();
    }

    int loadedCount() {
        return (int) loadedSchematics.estimatedSize();
    }

    boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null
            || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    private File schematicFile(String name) {
        return new File(folder, name + ".schem");
    }
}
