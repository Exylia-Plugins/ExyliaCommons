package net.exylia.commons.v2.yaml.adapter;

import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.yaml.config.YamlConfig;
import net.exylia.commons.v2.yaml.exception.YamlStorageException;
import net.exylia.commons.v2.yaml.util.BackupManager;
import net.exylia.commons.v2.yaml.util.YamlFileManager;
import net.exylia.commons.v2.yaml.util.YamlSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class YamlStorageAdapter {

    private final Path baseDir;
    private final YamlFileManager fileManager;
    private final BackupManager backupManager;
    private final YamlConfig config;

    public YamlStorageAdapter(YamlConfig config) {
        this.config = config;
        this.baseDir = config.getBaseDir();
        this.fileManager = new YamlFileManager();
        this.backupManager = config.isBackupEnabled()
            ? new BackupManager(baseDir, config.getMaxBackupsPerEntity())
            : null;

        if (config.isAutoCreateDirectories()) {
            try {
                fileManager.createDirectories(baseDir);
            } catch (IOException e) {
                throw new YamlStorageException("Failed to create base directory: " + baseDir, e);
            }
        }
    }

    public <T extends Entity> void save(T entity, EntityMetadata metadata) {
        try {
            Path tableDir = getTableDirectory(metadata);
            Path entityFile = getEntityFile(entity.getId(), metadata);

            if (config.isBackupEnabled() && Files.exists(entityFile) && backupManager != null) {
                backupManager.backup(entityFile, metadata.getTableName());
            }

            Map<String, Object> yamlData = YamlSerializer.entityToYaml(entity, metadata);
            fileManager.writeYaml(entityFile, yamlData);

        } catch (IOException e) {
            throw new YamlStorageException("Failed to save entity: " + entity.getId(), e);
        }
    }

    public <T extends Entity> Optional<T> load(Object id, Class<T> entityClass, EntityMetadata metadata) {
        try {
            Path entityFile = getEntityFile(id, metadata);

            if (!Files.exists(entityFile)) {
                return Optional.empty();
            }

            if (config.isValidateOnLoad() && !fileManager.validateYaml(entityFile)) {
                throw new YamlStorageException("Invalid YAML file: " + entityFile);
            }

            Map<String, Object> yamlData = fileManager.readYaml(entityFile);
            if (yamlData.isEmpty()) {
                return Optional.empty();
            }

            T entity = YamlSerializer.yamlToEntity(yamlData, entityClass, metadata);
            return Optional.of(entity);

        } catch (IOException e) {
            throw new YamlStorageException("Failed to load entity: " + id, e);
        }
    }

    public <T extends Entity> List<T> loadAll(Class<T> entityClass, EntityMetadata metadata) {
        try {
            Path tableDir = getTableDirectory(metadata);

            if (!Files.exists(tableDir)) {
                return Collections.emptyList();
            }

            List<Path> yamlFiles = fileManager.listYamlFiles(tableDir);
            List<T> entities = new ArrayList<>();

            for (Path file : yamlFiles) {
                String fileName = file.getFileName().toString();
                String id = fileName.replace(".yml", "").replace(".yaml", "");

                Optional<T> entity = load(id, entityClass, metadata);
                entity.ifPresent(entities::add);
            }

            return entities;

        } catch (IOException e) {
            throw new YamlStorageException("Failed to load all entities for: " + entityClass.getName(), e);
        }
    }

    public <T extends Entity> void delete(Object id, EntityMetadata metadata) {
        try {
            Path entityFile = getEntityFile(id, metadata);

            if (!Files.exists(entityFile)) {
                return;
            }

            if (config.isBackupEnabled() && backupManager != null) {
                backupManager.backup(entityFile, metadata.getTableName());
            }

            Files.delete(entityFile);

        } catch (IOException e) {
            throw new YamlStorageException("Failed to delete entity: " + id, e);
        }
    }

    /**
     * Deletes every entity file for this table in one pass, instead of
     * loading+deleting entities one by one. Returns the number of files
     * removed.
     */
    public int truncate(EntityMetadata metadata) {
        try {
            Path tableDir = getTableDirectory(metadata);
            if (!Files.exists(tableDir)) {
                return 0;
            }
            List<Path> yamlFiles = fileManager.listYamlFiles(tableDir);
            for (Path file : yamlFiles) {
                if (config.isBackupEnabled() && backupManager != null) {
                    backupManager.backup(file, metadata.getTableName());
                }
                Files.delete(file);
            }
            return yamlFiles.size();
        } catch (IOException e) {
            throw new YamlStorageException("Failed to truncate table: " + metadata.getTableName(), e);
        }
    }

    private Path getTableDirectory(EntityMetadata metadata) throws IOException {
        Path dir = baseDir.resolve(metadata.getTableName());

        if (config.isAutoCreateDirectories() && !Files.exists(dir)) {
            fileManager.createDirectories(dir);
        }

        return dir;
    }

    private Path getEntityFile(Object id, EntityMetadata metadata) throws IOException {
        String sanitizedId = fileManager.sanitizeId(id);
        Path tableDir = getTableDirectory(metadata);
        return tableDir.resolve(sanitizedId + ".yml");
    }
}
