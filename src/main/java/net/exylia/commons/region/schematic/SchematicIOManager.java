package net.exylia.commons.region.schematic;

import net.exylia.commons.async.Schedulers;
import net.exylia.commons.cache.CaffeineCache;
import net.exylia.commons.utils.DebugUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SchematicIOManager {
    private static SchematicIOManager instance;

    private final Path schemsFolder;
    private final CaffeineCache<String, SchematicData> cache;

    private SchematicIOManager(Path schemsFolder) {
        this.schemsFolder = schemsFolder;
        this.cache = CaffeineCache.<String, SchematicData>builder()
            .maximumSize(5)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

        try {
            Files.createDirectories(schemsFolder);
        } catch (IOException e) {
            DebugUtils.logInternalError("Failed to create schematics folder: " + e.getMessage());
        }
    }

    public static void initialize(Path schemsFolder) {
        if (instance == null) {
            instance = new SchematicIOManager(schemsFolder);
        }
    }

    public static SchematicIOManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SchematicIOManager not initialized");
        }
        return instance;
    }

    public CompletableFuture<SchematicData> loadSchematic(String id) {
        return Schedulers.supplyAsyncDb(() -> {
            long startTime = System.currentTimeMillis();
            DebugUtils.logInternalDebug("[SchematicIOManager] Starting to load schematic: " + id);

            SchematicData cached = cache.get(id);
            if (cached != null) {
                long duration = System.currentTimeMillis() - startTime;
                DebugUtils.logInternalDebug("[SchematicIOManager] Loaded schematic from cache: " + id + " (" + duration + "ms)");
                return cached;
            }

            try {
                long fileStart = System.currentTimeMillis();
                Path filePath = schemsFolder.resolve(id + ".schem");
                if (!Files.exists(filePath)) {
                    DebugUtils.logInternalError("[SchematicIOManager] Schematic file not found: " + id);
                    return null;
                }

                byte[] data = Files.readAllBytes(filePath);
                long readDuration = System.currentTimeMillis() - fileStart;
                DebugUtils.logInternalDebug("[SchematicIOManager] File read completed in " + readDuration + "ms - Size: " + data.length + " bytes");

                long deserializeStart = System.currentTimeMillis();
                SchematicData schematicData = SchematicFormat.deserialize(data);
                long deserializeDuration = System.currentTimeMillis() - deserializeStart;
                DebugUtils.logInternalDebug("[SchematicIOManager] Deserialization completed in " + deserializeDuration + "ms");

                cache.put(id, schematicData);

                long totalDuration = System.currentTimeMillis() - startTime;
                DebugUtils.logInternalDebug("[SchematicIOManager] Loaded schematic from file: " + id + " (total: " + totalDuration + "ms)");
                return schematicData;

            } catch (IOException e) {
                long duration = System.currentTimeMillis() - startTime;
                DebugUtils.logInternalError("[SchematicIOManager] Error loading schematic " + id + " (after " + duration + "ms): " + e.getMessage() + " - File may be in old format and needs migration");

                File schematicFile = schemsFolder.resolve(id + ".schem").toFile();
                if (schematicFile.exists()) {
                    try {
                        Path backupPath = schemsFolder.resolve(id + ".schem.old_backup");
                        Files.move(schematicFile.toPath(), backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        DebugUtils.logInternalInfo("[SchematicIOManager] Old format schematic backed up: " + id + ".schem.old_backup");
                    } catch (Exception ex) {
                        DebugUtils.logInternalError("[SchematicIOManager] Could not backup old schematic: " + ex.getMessage());
                    }
                }

                return null;
            }
        });
    }

    public CompletableFuture<Boolean> saveSchematic(String id, SchematicData data) {
        return Schedulers.supplyAsyncDb(() -> {
            try {
                byte[] serialized = SchematicFormat.serialize(data);
                Path filePath = schemsFolder.resolve(id + ".schem");
                Files.write(filePath, serialized);

                cache.put(id, data);
                DebugUtils.logInternalDebug("Saved schematic: " + id);
                return true;

            } catch (IOException e) {
                DebugUtils.logInternalError("Error saving schematic " + id + ": " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteSchematic(String id) {
        return Schedulers.supplyAsyncDb(() -> {
            try {
                Path filePath = schemsFolder.resolve(id + ".schem");
                Files.deleteIfExists(filePath);
                cache.invalidate(id);

                DebugUtils.logInternalDebug("Deleted schematic: " + id);
                return true;

            } catch (IOException e) {
                DebugUtils.logInternalError("Error deleting schematic " + id + ": " + e.getMessage());
                return false;
            }
        });
    }

    public boolean hasSchematic(String id) {
        return Files.exists(schemsFolder.resolve(id + ".schem"));
    }

    public long getCacheSize() {
        return cache.size();
    }

    public void invalidateFromCache(String id) {
        cache.invalidate(id);
    }

    public void clearCache() {
        cache.invalidateAll();
        DebugUtils.logInternalDebug("Schematic cache cleared");
    }

    public void shutdown() {
        cache.invalidateAll();
    }
}
