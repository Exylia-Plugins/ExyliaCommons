package net.exylia.commons.v2.database.transfer.core;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.exylia.commons.v2.database.core.DatabaseManager;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;
import net.exylia.commons.v2.database.repository.Repository;
import net.exylia.commons.v2.database.transfer.model.TransferResult;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class DatabaseImporter {

    private static final Gson GSON = new Gson();
    private static final int BATCH_SIZE = 500;

    private DatabaseImporter() {}

    @SuppressWarnings("unchecked")
    public static TransferResult importData(Player player, Path filePath) {
        long start = System.currentTimeMillis();
        DatabaseManager manager = DatabaseManager.getInstance();
        Map<String, EntityMetadata> metadataByTable = buildTableLookup(manager);

        log(player, "Starting import from: " + filePath.getFileName(), NamedTextColor.YELLOW);

        int totalRows = 0;
        int tablesProcessed = 0;
        String version = "?";
        String sourceAdapter = "unknown";

        try (JsonReader jr = new JsonReader(new FileReader(filePath.toFile()))) {
            jr.beginObject();
            while (jr.hasNext()) {
                String key = jr.nextName();
                switch (key) {
                    case "version" -> version = jr.nextString();
                    case "sourceAdapter" -> sourceAdapter = jr.nextString();
                    case "tables" -> {
                        log(player, "File v" + version + " from " + sourceAdapter, NamedTextColor.GRAY);
                        jr.beginObject();
                        while (jr.hasNext()) {
                            String tableName = jr.nextName();
                            EntityMetadata metadata = metadataByTable.get(tableName);
                            if (metadata == null) {
                                log(player, "Skipping unknown table '" + tableName + "'", NamedTextColor.YELLOW);
                                jr.skipValue();
                                continue;
                            }
                            int saved = importTableStream(jr, tableName, metadata, manager, player);
                            totalRows += saved;
                            tablesProcessed++;
                        }
                        jr.endObject();
                    }
                    default -> jr.skipValue();
                }
            }
            jr.endObject();
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.DATABASE, "[Transfer] Import failed", e);
            return TransferResult.builder().success(false).error(e.getMessage()).build();
        }

        long duration = System.currentTimeMillis() - start;
        logSuccess(player, "Import complete! " + totalRows + " rows into " + tablesProcessed + " tables (" + duration + "ms)");

        return TransferResult.builder()
                .success(true)
                .tablesProcessed(tablesProcessed)
                .rowsProcessed(totalRows)
                .durationMs(duration)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static int importTableStream(JsonReader jr, String tableName, EntityMetadata metadata,
                                          DatabaseManager manager, Player player) throws IOException {
        Class<Entity> entityClass = (Class<Entity>) metadata.getEntityClass();
        Repository<Entity> repo = (Repository<Entity>) manager.getRepository(entityClass);

        List<Entity> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;
        int totalRead = 0;

        log(player, "Importing '" + tableName + "'...", NamedTextColor.GRAY);

        jr.beginArray();
        while (jr.hasNext()) {
            Map<String, Object> row = GSON.fromJson(jr, Map.class);
            totalRead++;

            Entity entity = reconstructEntity(entityClass, metadata, row);
            if (entity != null) {
                batch.add(entity);
            }

            if (batch.size() >= BATCH_SIZE) {
                try {
                    repo.saveAll(batch);
                    totalSaved += batch.size();
                    log(player, "  " + tableName + ": " + totalSaved + " rows...", NamedTextColor.DARK_GRAY);
                } catch (Exception e) {
                    logError(player, "Batch failed in '" + tableName + "': " + e.getMessage(), e);
                } finally {
                    batch.clear();
                }
            }
        }
        jr.endArray();

        if (!batch.isEmpty()) {
            try {
                repo.saveAll(batch);
                totalSaved += batch.size();
            } catch (Exception e) {
                logError(player, "Final batch failed in '" + tableName + "': " + e.getMessage(), e);
            } finally {
                batch.clear();
            }
        }

        log(player, "Table '" + tableName + "': " + totalSaved + "/" + totalRead + " rows done", NamedTextColor.GRAY);
        return totalSaved;
    }

    private static Entity reconstructEntity(Class<Entity> entityClass, EntityMetadata metadata, Map<String, Object> row) {
        try {
            Entity entity = entityClass.getDeclaredConstructor().newInstance();
            for (FieldDescriptor fd : metadata.getFields()) {
                Object value = row.get(fd.getColumnName());
                if (value != null) {
                    fd.setValue(entity, value);
                }
            }
            return entity;
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.DATABASE, "[Transfer] Failed to reconstruct " + entityClass.getSimpleName(), e);
            return null;
        }
    }

    private static Map<String, EntityMetadata> buildTableLookup(DatabaseManager manager) {
        Map<String, EntityMetadata> lookup = new HashMap<>();
        for (EntityMetadata metadata : manager.getEntityMetadataCache().values()) {
            lookup.put(metadata.getTableName(), metadata);
        }
        return lookup;
    }

    private static void log(Player player, String message, NamedTextColor playerColor) {
        DebugAPI.logLibInfo(DebugCategory.DATABASE, "[Transfer] " + message);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("[DB Import] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(message, playerColor)));
        }
    }

    private static void logSuccess(Player player, String message) {
        DebugAPI.logLibSuccess(DebugCategory.DATABASE, "[Transfer] " + message);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("[DB Import] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(message, NamedTextColor.GREEN)));
        }
    }

    private static void logError(Player player, String message, Throwable cause) {
        DebugAPI.logLibError(DebugCategory.DATABASE, "[Transfer] " + message, cause);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("[DB Import] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(message, NamedTextColor.RED)));
        }
    }
}
