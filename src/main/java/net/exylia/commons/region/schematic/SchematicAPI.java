package net.exylia.commons.region.schematic;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SchematicAPI {

    public static void initialize(JavaPlugin plugin) {
        SchematicManager.initialize(plugin);
    }

    public static SchematicManager getManager() {
        return SchematicManager.getInstance();
    }

    public static CompletableFuture<Boolean> saveRegion(String id, Block minBlock, Block maxBlock) {
        return getManager().saveSchematic(id, minBlock, maxBlock);
    }

    public static CompletableFuture<Boolean> pasteRegion(String id, Location pasteLocation) {
        return getManager().pasteSchematic(id, pasteLocation);
    }

    public static CompletableFuture<Boolean> regenerate(String id, Block minBlock) {
        return getManager().regenerateRegion(id, minBlock);
    }

    public static CompletableFuture<Boolean> delete(String id) {
        return getManager().deleteSchematic(id);
    }

    public static CompletableFuture<List<Boolean>> regenerateMultiple(List<String> ids, Block baseBlock) {
        return getManager().regenerateMultiple(ids, baseBlock);
    }

    public static boolean exists(String id) {
        return getManager().hasSchematic(id);
    }

    public static boolean isWorking(String id) {
        return getManager().isOperating(id);
    }

    public static RegenerationScheduler.RegenerationStats getStats() {
        return getManager().getStats();
    }

    public static void clearCache() {
        getManager().clearCache();
    }

    public static void shutdown() {
        getManager().shutdown();
    }
}
