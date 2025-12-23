package net.exylia.commons.v2.yaml.util;

import net.exylia.commons.v2.yaml.exception.YamlStorageException;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupManager {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path backupDir;
    private final int maxBackups;

    public BackupManager(Path baseDir, int maxBackups) {
        this.backupDir = baseDir.resolve(".backups");
        this.maxBackups = maxBackups;
    }

    public void backup(Path file, String tableName) throws IOException {
        if (!Files.exists(file)) {
            return;
        }

        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            Path tableBackupDir = backupDir.resolve(tableName);
            if (!Files.exists(tableBackupDir)) {
                Files.createDirectories(tableBackupDir);
            }

            String fileName = file.getFileName().toString();
            String entityId = fileName.replace(".yml", "").replace(".yaml", "");
            String timestamp = generateBackupTimestamp();
            String backupFileName = entityId + "_" + timestamp + ".yml";

            Path backupFile = tableBackupDir.resolve(backupFileName);
            Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING);

            DebugAPI.logLibDebug(DebugCategory.GENERAL, "Backup created: " + backupFile);

            cleanOldBackups(tableName, entityId);

        } catch (IOException e) {
            throw new YamlStorageException("Failed to backup file: " + file, e);
        }
    }

    private void cleanOldBackups(String tableName, String entityId) {
        try {
            Path tableBackupDir = backupDir.resolve(tableName);
            if (!Files.exists(tableBackupDir)) {
                return;
            }

            List<Path> backups;
            try (Stream<Path> files = Files.list(tableBackupDir)) {
                backups = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(entityId + "_"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .collect(Collectors.toList());
            }

            if (backups.size() > maxBackups) {
                backups.stream()
                    .skip(maxBackups)
                    .forEach(backup -> {
                        try {
                            Files.deleteIfExists(backup);
                            DebugAPI.logLibDebug(DebugCategory.GENERAL, "Old backup deleted: " + backup);
                        } catch (IOException e) {
                            DebugAPI.logLibWarn(DebugCategory.GENERAL, "Failed to delete old backup: " + backup);
                        }
                    });
            }

        } catch (IOException e) {
            DebugAPI.logLibWarn(DebugCategory.GENERAL, "Failed to clean old backups for " + tableName + "/" + entityId);
        }
    }

    private String generateBackupTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
