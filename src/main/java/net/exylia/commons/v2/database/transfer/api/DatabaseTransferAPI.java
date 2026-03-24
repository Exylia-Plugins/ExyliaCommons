package net.exylia.commons.v2.database.transfer.api;

import net.exylia.commons.v2.database.core.DatabaseManager;
import net.exylia.commons.v2.database.transfer.core.DatabaseExporter;
import net.exylia.commons.v2.database.transfer.core.DatabaseImporter;
import net.exylia.commons.v2.database.transfer.model.TransferResult;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public final class DatabaseTransferAPI {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private DatabaseTransferAPI() {}

    public static CompletableFuture<TransferResult> export(Player player, String filename) {
        Path outputPath = resolveExportsDir().resolve(filename.endsWith(".json") ? filename : filename + ".json");
        return Tasks.dbValue(() -> DatabaseExporter.export(player, outputPath));
    }

    public static CompletableFuture<TransferResult> export(String filename) {
        Path outputPath = resolveExportsDir().resolve(filename.endsWith(".json") ? filename : filename + ".json");
        return Tasks.dbValue(() -> DatabaseExporter.export(null, outputPath));
    }

    public static CompletableFuture<TransferResult> export(Player player) {
        String filename = "export_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".json";
        return export(player, filename);
    }

    public static CompletableFuture<TransferResult> export() {
        String filename = "export_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".json";
        return export(filename);
    }

    public static CompletableFuture<TransferResult> importData(Player player, String filename) {
        Path filePath = resolveExportsDir().resolve(filename.endsWith(".json") ? filename : filename + ".json");
        return Tasks.dbValue(() -> DatabaseImporter.importData(player, filePath));
    }

    public static CompletableFuture<TransferResult> importData(String filename) {
        Path filePath = resolveExportsDir().resolve(filename.endsWith(".json") ? filename : filename + ".json");
        return Tasks.dbValue(() -> DatabaseImporter.importData(null, filePath));
    }

    private static Path resolveExportsDir() {
        return DatabaseManager.getInstance().getPlugin().getDataFolder().toPath().resolve("database/exports");
    }
}
