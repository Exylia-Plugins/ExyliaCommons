package net.exylia.commons.v2.database.transfer.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class DatabaseExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private DatabaseExporter() {}

    @SuppressWarnings("unchecked")
    public static TransferResult export(Player player, Path outputPath) {
        long start = System.currentTimeMillis();
        DatabaseManager manager = DatabaseManager.getInstance();

        log(player, "Starting export from adapter: " + manager.getAdapter().getAdapterName(), NamedTextColor.YELLOW);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("version", "1.0");
        manifest.put("exportedAt", Instant.now().toEpochMilli());
        manifest.put("sourceAdapter", manager.getAdapter().getAdapterName());

        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        int totalRows = 0;
        int tablesCount = 0;

        for (Map.Entry<Class<?>, EntityMetadata> entry : manager.getEntityMetadataCache().entrySet()) {
            EntityMetadata metadata = entry.getValue();
            String tableName = metadata.getTableName();

            try {
                Repository<Entity> repo = (Repository<Entity>) manager.getRepository((Class<Entity>) entry.getKey());
                List<Entity> entities = repo.findAll();
                List<Map<String, Object>> rows = new ArrayList<>(entities.size());

                for (Entity entity : entities) {
                    rows.add(serializeEntity(entity, metadata));
                }

                tables.put(tableName, rows);
                totalRows += rows.size();
                tablesCount++;

                log(player, "Table '" + tableName + "': " + rows.size() + " rows", NamedTextColor.GRAY);
            } catch (Exception e) {
                logError(player, "Failed to export table '" + tableName + "': " + e.getMessage(), e);
            }
        }

        manifest.put("tables", tables);

        try {
            Files.createDirectories(outputPath.getParent());
            try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                GSON.toJson(manifest, writer);
            }
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
