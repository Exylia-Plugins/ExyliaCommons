package net.exylia.commons.v2.database.transfer.core;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class DatabaseExporter {

    /**
     * Compact on purpose: at millions of rows, pretty-printing inflates the file by
     * a third for zero benefit — the importer reads this with a streaming parser.
     */
    private static final Gson GSON = new Gson();

    /**
     * Rows fetched per page and held in memory at any moment. The previous
     * implementation materialised every table (plus a map per row, plus the whole
     * manifest) in heap before writing a single byte; a server with millions of
     * rows ran out of memory mid-export, got killed by the host, and the
     * ungraceful kill corrupted the H2 file. Streaming keeps memory constant
     * regardless of database size.
     */
    private static final int PAGE_SIZE = 1000;

    private DatabaseExporter() {}

    @SuppressWarnings("unchecked")
    public static TransferResult export(Player player, Path outputPath) {
        long start = System.currentTimeMillis();
        DatabaseManager manager = DatabaseManager.getInstance();

        log(player, "Starting export from adapter: " + manager.getAdapter().getAdapterName(), NamedTextColor.YELLOW);

        // Table counts up front (cheap COUNT(*) per table) so the manifest can be
        // written before the tables themselves, keeping the format identical to
        // what the importer expects.
        Map<String, Repository<Entity>> reposByTable = new LinkedHashMap<>();
        Map<String, Integer> rowCounts = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, EntityMetadata> entry : manager.getEntityMetadataCache().entrySet()) {
            EntityMetadata metadata = entry.getValue();
            try {
                Repository<Entity> repo = (Repository<Entity>) manager.getRepository((Class<Entity>) entry.getKey());
                reposByTable.put(metadata.getTableName(), repo);
                rowCounts.put(metadata.getTableName(), (int) repo.count());
            } catch (Exception e) {
                logError(player, "Failed to prepare table '" + metadata.getTableName() + "': " + e.getMessage(), e);
            }
        }

        int totalRows = 0;
        int tablesCount = 0;

        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            DebugAPI.logLibError(DebugCategory.DATABASE, "[Transfer] Failed to create export directory", e);
            return TransferResult.builder().success(false).error(e.getMessage()).build();
        }

        try (BufferedWriter bw = Files.newBufferedWriter(outputPath);
             JsonWriter jw = new JsonWriter(bw)) {
            jw.beginObject();
            jw.name("version").value("1.0");
            jw.name("exportedAt").value(Instant.now().toEpochMilli());
            jw.name("sourceAdapter").value(manager.getAdapter().getAdapterName());

            jw.name("tableCounts");
            GSON.toJson(rowCounts, Map.class, jw);

            jw.name("tables");
            jw.beginObject();
            for (Map.Entry<String, Repository<Entity>> table : reposByTable.entrySet()) {
                String tableName = table.getKey();
                EntityMetadata metadata = findMetadata(manager, tableName);
                int written = streamTable(player, table.getValue(), metadata, jw);
                totalRows += written;
                tablesCount++;
                log(player, "Table '" + tableName + "': " + written + " rows", NamedTextColor.GRAY);
            }
            jw.endObject();
            jw.endObject();
        } catch (IOException e) {
            DebugAPI.logLibError(DebugCategory.DATABASE, "[Transfer] Failed to write export file", e);
            return TransferResult.builder().success(false).error(e.getMessage()).build();
        }

        long duration = System.currentTimeMillis() - start;
        String pathStr = outputPath.getFileName().toString();

        logSuccess(player, "Export complete! " + totalRows + " rows, " + tablesCount + " tables → " + pathStr + " (" + duration + "ms)");

        return TransferResult.builder()
                .success(true)
                .tablesProcessed(tablesCount)
                .rowsProcessed(totalRows)
                .durationMs(duration)
                .outputPath(pathStr)
                .build();
    }

    /**
     * Writes one table as a JSON array, paging through the repository so at most
     * {@link #PAGE_SIZE} entities are live at once. Each page is garbage
     * collectable as soon as it has been serialised.
     * <p>
     * On failure the array is still closed so the rest of the file stays valid
     * JSON: a partially exported table beats an unimportable export.
     */
    private static int streamTable(Player player, Repository<Entity> repo, EntityMetadata metadata, JsonWriter jw) throws IOException {
        jw.name(metadata.getTableName());
        jw.beginArray();

        int written = 0;
        int page = 0;
        try {
            while (true) {
                List<Entity> entities = repo.findAllPaged(page, PAGE_SIZE);
                for (Entity entity : entities) {
                    GSON.toJson(serializeEntity(entity, metadata), Map.class, jw);
                }
                written += entities.size();
                if (entities.size() < PAGE_SIZE) break;
                page++;
            }
        } catch (Exception e) {
            logError(player, "Failed to export table '" + metadata.getTableName() + "' after " + written + " rows: " + e.getMessage(), e);
        } finally {
            jw.endArray();
            jw.flush();
        }
        return written;
    }

    private static EntityMetadata findMetadata(DatabaseManager manager, String tableName) {
        for (EntityMetadata metadata : manager.getEntityMetadataCache().values()) {
            if (metadata.getTableName().equals(tableName)) return metadata;
        }
        throw new IllegalStateException("No metadata for table " + tableName);
    }

    private static Map<String, Object> serializeEntity(Entity entity, EntityMetadata metadata) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (FieldDescriptor fd : metadata.getFields()) {
            row.put(fd.getColumnName(), toJsonSafe(fd.getValue(entity)));
        }
        return row;
    }

    private static Object toJsonSafe(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return value.toString();
        if (value instanceof Enum) return ((Enum<?>) value).name();
        return value;
    }

    private static void log(Player player, String message, NamedTextColor playerColor) {
        DebugAPI.logLibInfo(DebugCategory.DATABASE, "[Transfer] " + message);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("[DB Export] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(message, playerColor)));
        }
    }

    private static void logSuccess(Player player, String message) {
        DebugAPI.logLibSuccess(DebugCategory.DATABASE, "[Transfer] " + message);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("[DB Export] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(message, NamedTextColor.GREEN)));
        }
    }

    private static void logError(Player player, String message, Throwable cause) {
        DebugAPI.logLibError(DebugCategory.DATABASE, "[Transfer] " + message, cause);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("[DB Export] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(message, NamedTextColor.RED)));
        }
    }
}
