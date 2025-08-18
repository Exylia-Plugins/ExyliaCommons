package net.exylia.commons.database.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.exceptions.DatabaseException;
import net.exylia.commons.database.repository.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class DatabaseExportImportManager {

    private final ExyliaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Gson gson;
    @Getter
    private final Path exportDirectory;

    public DatabaseExportImportManager(ExyliaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
        this.exportDirectory = Paths.get(plugin.getDataFolder().getAbsolutePath(), "exports");

        try {
            Files.createDirectories(exportDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create export directory", e);
        }
    }

    public CompletableFuture<ExportResult> exportDatabase(ExportOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String fileName = options.getFileName() != null ? options.getFileName() :
                        "database_export_" + timestamp + (options.isCompressed() ? ".gz" : ".json");

                Path outputFile = exportDirectory.resolve(fileName);
                DatabaseExport export = new DatabaseExport();
                export.setMetadata(createExportMetadata(options));
                export.setData(new HashMap<>());

                Set<Class<?>> entitiesToExport = options.getEntityClasses() != null ?
                        options.getEntityClasses() : databaseManager.getRegisteredEntities();

                int totalEntities = 0;
                int totalTables = 0;

                for (Class<?> entityClass : entitiesToExport) {
                    try {
                        Repository<?> repository = databaseManager.getRepository(entityClass);
                        List<?> entities = repository.findAll();

                        if (!entities.isEmpty() || options.isIncludeEmptyTables()) {
                            String tableName = getTableName(entityClass);
                            EntityExport entityExport = new EntityExport();
                            entityExport.setEntityClass(entityClass.getName());
                            entityExport.setTableName(tableName);
                            entityExport.setCount(entities.size());
                            entityExport.setEntities(convertEntitiesToMaps(entities));

                            export.getData().put(tableName, entityExport);
                            totalEntities += entities.size();
                            totalTables++;

                            logInternalInfo("Exported " + entities.size() + " entities from " + tableName);
                        }
                    } catch (Exception e) {
                        if (!options.isContinueOnError()) {
                            throw new DatabaseException("Export", entityClass.getSimpleName(),
                                    getAdapterType(), "Failed to export entity", e);
                        }
                        logInternalWarn("Failed to export entity " + entityClass.getSimpleName() + ": " + e.getMessage());
                    }
                }

                writeExportToFile(export, outputFile, options.isCompressed());

                ExportResult result = new ExportResult();
                result.setSuccess(true);
                result.setFilePath(outputFile.toString());
                result.setTotalTables(totalTables);
                result.setTotalEntities(totalEntities);
                result.setFileSize(Files.size(outputFile));
                result.setDuration(System.currentTimeMillis() - export.getMetadata().getTimestamp());

                logInternalInfo("Database export completed: " + totalEntities + " entities from " +
                        totalTables + " tables exported to " + outputFile.getFileName());

                return result;

            } catch (Exception e) {
                ExportResult result = new ExportResult();
                result.setSuccess(false);
                result.setError(e.getMessage());
                return result;
            }
        });
    }

    public CompletableFuture<ImportResult> importDatabase(ImportOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path importFile = Paths.get(options.getFilePath());
                if (!Files.exists(importFile)) {
                    throw new FileNotFoundException("Import file not found: " + options.getFilePath());
                }

                DatabaseExport export = readExportFromFile(importFile, options.isCompressed());
                ImportResult result = new ImportResult();
                result.setImportedTables(new ArrayList<>());
                result.setSkippedTables(new ArrayList<>());
                result.setErrors(new ArrayList<>());

                int totalImported = 0;
                long startTime = System.currentTimeMillis();

                for (Map.Entry<String, EntityExport> entry : export.getData().entrySet()) {
                    String tableName = entry.getKey();
                    EntityExport entityExport = entry.getValue();

                    try {
                        Class<?> entityClass = Class.forName(entityExport.getEntityClass());

                        if (options.getEntityClasses() != null && !options.getEntityClasses().contains(entityClass)) {
                            result.getSkippedTables().add(tableName + " (not in filter)");
                            continue;
                        }

                        Repository<Object> repository = (Repository<Object>) databaseManager.getRepository(entityClass);

                        if (options.isClearBeforeImport()) {
                            List<Object> existingEntities = repository.findAll();
                            if (!existingEntities.isEmpty()) {
                                repository.deleteAll(existingEntities);
                                logInternalInfo("Cleared " + existingEntities.size() + " existing entities from " + tableName);
                            }
                        }

                        List<Object> entities = convertMapsToEntities(entityExport.getEntities(), entityClass);

                        if (options.getBatchSize() > 0 && entities.size() > options.getBatchSize()) {
                            importInBatches(repository, entities, options.getBatchSize(), options.isUseUpsert());
                        } else {
                            if (options.isUseUpsert()) {
                                repository.saveOrUpdateAll(entities);
                            } else {
                                repository.saveAll(entities);
                            }
                        }

                        result.getImportedTables().add(tableName + " (" + entities.size() + " entities)");
                        totalImported += entities.size();

                        logInternalInfo("Imported " + entities.size() + " entities to " + tableName);

                    } catch (Exception e) {
                        String error = "Failed to import " + tableName + ": " + e.getMessage();
                        result.getErrors().add(error);

                        if (!options.isContinueOnError()) {
                            throw new DatabaseException("Import", tableName, getAdapterType(), error, e);
                        }
                        logInternalWarn(error);
                    }
                }

                result.setSuccess(true);
                result.setTotalImported(totalImported);
                result.setDuration(System.currentTimeMillis() - startTime);
                result.setSourceMetadata(export.getMetadata());

                logInternalInfo("Database import completed: " + totalImported + " entities imported from " +
                        result.getImportedTables().size() + " tables");

                return result;

            } catch (Exception e) {
                ImportResult result = new ImportResult();
                result.setSuccess(false);
                result.setTotalImported(0);
                result.setErrors(Arrays.asList(e.getMessage()));
                return result;
            }
        });
    }

    public CompletableFuture<ExportResult> exportEntity(Class<?> entityClass, ExportOptions options) {
        Set<Class<?>> singleEntity = new HashSet<>();
        singleEntity.add(entityClass);
        options.setEntityClasses(singleEntity);
        return exportDatabase(options);
    }

    public CompletableFuture<ImportResult> importEntity(Class<?> entityClass, ImportOptions options) {
        Set<Class<?>> singleEntity = new HashSet<>();
        singleEntity.add(entityClass);
        options.setEntityClasses(singleEntity);
        return importDatabase(options);
    }

    public List<String> listExports() {
        try {
            return Files.list(exportDirectory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".gz"))
                    .map(path -> path.getFileName().toString())
                    .sorted(Collections.reverseOrder())
                    .toList();
        } catch (IOException e) {
            logInternalWarn("Failed to list exports: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public ExportMetadata getExportInfo(String fileName) {
        try {
            Path exportFile = exportDirectory.resolve(fileName);
            if (!Files.exists(exportFile)) {
                return null;
            }

            boolean isCompressed = fileName.endsWith(".gz");
            DatabaseExport export = readExportFromFile(exportFile, isCompressed);
            return export.getMetadata();

        } catch (Exception e) {
            logInternalWarn("Failed to read export info for " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    public boolean deleteExport(String fileName) {
        try {
            Path exportFile = exportDirectory.resolve(fileName);
            return Files.deleteIfExists(exportFile);
        } catch (IOException e) {
            logInternalWarn("Failed to delete export " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<ExportResult> createBackup() {
        ExportOptions options = new ExportOptions();
        options.setCompressed(true);
        options.setIncludeEmptyTables(false);
        options.setContinueOnError(true);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        options.setFileName("backup_" + timestamp + ".gz");

        return exportDatabase(options);
    }

    public CompletableFuture<ImportResult> restoreBackup(String backupFileName, boolean clearExisting) {
        ImportOptions options = new ImportOptions();
        options.setFilePath(exportDirectory.resolve(backupFileName).toString());
        options.setCompressed(backupFileName.endsWith(".gz"));
        options.setClearBeforeImport(clearExisting);
        options.setUseUpsert(true);
        options.setContinueOnError(false);
        options.setBatchSize(100);

        return importDatabase(options);
    }

    private ExportMetadata createExportMetadata(ExportOptions options) {
        ExportMetadata metadata = new ExportMetadata();
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setDatabaseType(getAdapterType());
        metadata.setPluginName(plugin.getName());
        metadata.setPluginVersion(plugin.getDescription().getVersion());
        metadata.setServerVersion(plugin.getServer().getVersion());
        metadata.setExportVersion("1.0");
        metadata.setOptions(options);
        return metadata;
    }

    private void writeExportToFile(DatabaseExport export, Path outputFile, boolean compressed) throws IOException {
        String jsonData = gson.toJson(export);

        if (compressed) {
            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile());
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 OutputStreamWriter writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
                writer.write(jsonData);
            }
        } else {
            Files.write(outputFile, jsonData.getBytes(StandardCharsets.UTF_8));
        }
    }

    private DatabaseExport readExportFromFile(Path inputFile, boolean compressed) throws IOException {
        String jsonData;

        if (compressed) {
            try (FileInputStream fis = new FileInputStream(inputFile.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 InputStreamReader reader = new InputStreamReader(gzis, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(reader)) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                jsonData = sb.toString();
            }
        } else {
            jsonData = Files.readString(inputFile, StandardCharsets.UTF_8);
        }

        return gson.fromJson(jsonData, DatabaseExport.class);
    }

    private List<Map<String, Object>> convertEntitiesToMaps(List<?> entities) {
        List<Map<String, Object>> maps = new ArrayList<>();
        DatabaseAdapter adapter = databaseManager.getAdapter();

        for (Object entity : entities) {
            try {
                Map<String, Object> entityMap = adapter.entityToMap(entity);
                maps.add(entityMap);
            } catch (Exception e) {
                logInternalWarn("Failed to convert entity to map: " + e.getMessage());
            }
        }

        return maps;
    }

    private List<Object> convertMapsToEntities(List<Map<String, Object>> maps, Class<?> entityClass) {
        List<Object> entities = new ArrayList<>();
        DatabaseAdapter adapter = databaseManager.getAdapter();

        for (Map<String, Object> map : maps) {
            try {
                Object entity = adapter.mapToEntity(map, entityClass);
                entities.add(entity);
            } catch (Exception e) {
                logInternalWarn("Failed to convert map to entity for " + entityClass.getSimpleName() + ": " + e.getMessage());
            }
        }

        return entities;
    }

    private void importInBatches(Repository<Object> repository, List<Object> entities, int batchSize, boolean useUpsert) {
        for (int i = 0; i < entities.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entities.size());
            List<Object> batch = entities.subList(i, endIndex);

            try {
                if (useUpsert) {
                    repository.saveOrUpdateAll(batch);
                } else {
                    repository.saveAll(batch);
                }
                logInternalInfo("Imported batch " + (i / batchSize + 1) + ": " + batch.size() + " entities");
            } catch (Exception e) {
                logInternalWarn("Failed to import batch starting at index " + i + ": " + e.getMessage());
                throw e;
            }
        }
    }

    private String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    private String getAdapterType() {
        DatabaseAdapter adapter = databaseManager.getAdapter();
        return adapter != null ? adapter.getClass().getSimpleName().replace("Adapter", "") : "Unknown";
    }
}