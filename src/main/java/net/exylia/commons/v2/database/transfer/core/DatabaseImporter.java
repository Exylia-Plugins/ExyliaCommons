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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class DatabaseImporter {

    private static final Gson GSON = new Gson();

    /**
     * Upper bound on rows per batch. Only ever reached by narrow tables — a batch
     * of wide rows hits {@link #BATCH_BYTES} first and is flushed early.
     */
    private static final int BATCH_SIZE = 500;

    /**
     * Payload budget per batch, in characters of decoded JSON.
     * <p>
     * Row count alone is the wrong unit: a table may declare an unbounded text
     * column — a Base64 Bukkit inventory, say — so 500 rows is a few hundred KB
     * for one table and hundreds of MB for another. The JDBC driver holds every
     * bound parameter of the batch until {@code executeBatch()}, and MySQL with
     * {@code rewriteBatchedStatements} additionally concatenates them into a
     * single contiguous payload, so an oversized batch is several multiples of
     * itself in live memory at the moment it is sent.
     * <p>
     * Whichever limit is reached first ends the batch, which keeps peak memory
     * bounded by payload rather than by whatever the widest table happens to hold.
     */
    private static final int BATCH_BYTES = 8 * 1024 * 1024;

    /**
     * Rough character count of a row, used to decide when the batch has grown
     * large enough to flush. Only strings are measured: every other column type is
     * fixed-width and negligible next to a blob, and the estimate only needs to be
     * good enough to keep the batch off the heap ceiling.
     */
    private static long estimateRowSize(Map<String, Object> row) {
        long size = 0;
        for (Object value : row.values()) {
            size += value instanceof String s ? s.length() : 8;
        }
        return size;
    }

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
        Map<String, Integer> tableCounts = new HashMap<>();

        // Buffered on purpose: JsonReader pulls a character at a time, so an
        // unbuffered FileReader turns a multi-gigabyte import into one syscall per
        // character.
        try (JsonReader jr = new JsonReader(Files.newBufferedReader(filePath))) {
            jr.beginObject();
            while (jr.hasNext()) {
                String key = jr.nextName();
                switch (key) {
                    case "version" -> version = jr.nextString();
                    case "sourceAdapter" -> sourceAdapter = jr.nextString();
                    case "tableCounts" -> {
                        jr.beginObject();
                        while (jr.hasNext()) {
                            tableCounts.put(jr.nextName(), jr.nextInt());
                        }
                        jr.endObject();
                    }
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
                            int tableTotal = tableCounts.getOrDefault(tableName, -1);
                            int saved = importTableStream(jr, tableName, metadata, manager, player, tableTotal);
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
                                          DatabaseManager manager, Player player, int totalExpected) throws IOException {
        Class<Entity> entityClass = (Class<Entity>) metadata.getEntityClass();
        Repository<Entity> repo = (Repository<Entity>) manager.getRepository(entityClass);

        List<Entity> batch = new ArrayList<>(BATCH_SIZE);
        int totalSaved = 0;
        int totalRead = 0;
        long batchBytes = 0;

        if (totalExpected > 0) {
            log(player, "Importing '" + tableName + "' (" + totalExpected + " rows)...", NamedTextColor.GRAY);
        } else {
            log(player, "Importing '" + tableName + "'...", NamedTextColor.GRAY);
        }

        jr.beginArray();
        while (jr.hasNext()) {
            Map<String, Object> row = GSON.fromJson(jr, Map.class);
            totalRead++;

            Entity entity = reconstructEntity(entityClass, metadata, row);
            if (entity != null) {
                batch.add(entity);
                batchBytes += estimateRowSize(row);
            }

            if (batch.size() >= BATCH_SIZE || batchBytes >= BATCH_BYTES) {
                try {
                    repo.bulkLoad(batch);
                    totalSaved += batch.size();
                    log(player, "  " + tableName + ": " + formatProgress(totalSaved, totalExpected), NamedTextColor.DARK_GRAY);
                } catch (Exception e) {
                    logError(player, "Batch failed in '" + tableName + "': " + e.getMessage(), e);
                } finally {
                    batch.clear();
                    batchBytes = 0;
                }
            }
        }
        jr.endArray();

        if (!batch.isEmpty()) {
            try {
                repo.bulkLoad(batch);
                totalSaved += batch.size();
            } catch (Exception e) {
                logError(player, "Final batch failed in '" + tableName + "': " + e.getMessage(), e);
            } finally {
                batch.clear();
                batchBytes = 0;
            }
        }

        log(player, "Table '" + tableName + "': " + totalSaved + "/" + totalRead + " rows done", NamedTextColor.GRAY);
        return totalSaved;
    }

    private static String formatProgress(int current, int total) {
        if (total <= 0) {
            return current + " rows...";
        }
        int remaining = total - current;
        int pct = (int) (current * 100L / total);
        return current + "/" + total + " (" + remaining + " left) " + pct + "%";
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
