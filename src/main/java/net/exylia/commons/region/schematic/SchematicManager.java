package net.exylia.commons.region.schematic;

import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class SchematicManager {
    private static SchematicManager instance;

    private final JavaPlugin plugin;
    private final SchematicIOManager ioManager;
    private final BlockPlacer blockPlacer;
    private final RegenerationScheduler scheduler;

    private SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;

        Path schemsFolder = Paths.get(plugin.getDataFolder().getAbsolutePath(), "schematics");
        SchematicIOManager.initialize(schemsFolder);
        this.ioManager = SchematicIOManager.getInstance();

        this.blockPlacer = new BlockPlacer();
        this.scheduler = new RegenerationScheduler(plugin, blockPlacer, ioManager);

        SchemMigration.migrateOldSchematics(schemsFolder);

        DebugUtils.logInternalInfo("SchematicManager initialized");
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new SchematicManager(plugin);
        }
    }

    public static SchematicManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SchematicManager not initialized");
        }
        return instance;
    }

    public CompletableFuture<Boolean> saveSchematic(String id, Block minBlock, Block maxBlock) {
        if (minBlock.getWorld() != maxBlock.getWorld()) {
            return CompletableFuture.completedFuture(false);
        }

        return scheduler.queueSave(id, minBlock, maxBlock);
    }

    public CompletableFuture<Boolean> pasteSchematic(String id, Location pasteLocation) {
        if (!ioManager.hasSchematic(id)) {
            return CompletableFuture.completedFuture(false);
        }

        return scheduler.queuePaste(id, pasteLocation);
    }

    public CompletableFuture<Boolean> regenerateRegion(String id, Block minBlock) {
        if (!ioManager.hasSchematic(id)) {
            return CompletableFuture.completedFuture(false);
        }

        return scheduler.queueRegenerate(id, minBlock);
    }

    public CompletableFuture<Boolean> regenerateRegionWithAutoSave(String id, Block minBlock, Block maxBlock) {
        if (!ioManager.hasSchematic(id)) {
            DebugUtils.logInternalDebug("No schematic found for " + id + ", auto-generating from current region state...");
            return saveSchematic(id, minBlock, maxBlock)
                .thenCompose(saved -> {
                    if (saved) {
                        return regenerateRegion(id, minBlock);
                    }
                    return CompletableFuture.completedFuture(false);
                });
        }

        return regenerateRegion(id, minBlock);
    }

    public CompletableFuture<Boolean> deleteSchematic(String id) {
        return ioManager.deleteSchematic(id);
    }

    public CompletableFuture<List<Boolean>> regenerateMultiple(List<String> ids, Block baseBlock) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (String id : ids) {
            if (ioManager.hasSchematic(id)) {
                futures.add(regenerateRegion(id, baseBlock));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    public boolean hasSchematic(String id) {
        return ioManager.hasSchematic(id);
    }

    public boolean isOperating(String id) {
        return scheduler.isOperating(id);
    }

    public RegenerationScheduler.RegenerationStats getStats() {
        return scheduler.getStats();
    }

    public void clearCache() {
        ioManager.clearCache();
    }

    public void shutdown() {
        scheduler.shutdown();
        ioManager.shutdown();
        DebugUtils.logInternalInfo("SchematicManager shutdown complete");
    }
}
