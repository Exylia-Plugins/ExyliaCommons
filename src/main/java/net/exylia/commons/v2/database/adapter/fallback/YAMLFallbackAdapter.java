package net.exylia.commons.v2.database.adapter.fallback;

import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;
import net.exylia.commons.utils.DebugUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YAMLFallbackAdapter implements DatabaseAdapter {

    private final Path basePath;
    private final Map<String, List<Map<String, Object>>> tableCache = new ConcurrentHashMap<>();
    private final Yaml yaml = new Yaml();

    public YAMLFallbackAdapter(AdapterConfig config) {
        this.basePath = Paths.get(config.getFile()).resolve("yaml");
    }

    @Override
    public void connect() throws Exception {
        Files.createDirectories(basePath);
        DebugUtils.logInternalInfo("YAML Fallback adapter initialized at " + basePath);
    }

    @Override
    public void disconnect() throws Exception {
        saveAll();
        tableCache.clear();
        DebugUtils.logInternalInfo("YAML Fallback adapter disconnected");
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void createTable(EntityMetadata metadata) throws Exception {
        Path filePath = basePath.resolve(metadata.getTableName() + ".yml");
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
            tableCache.put(metadata.getTableName(), new ArrayList<>());
        } else {
            loadTableFromFile(metadata.getTableName());
        }
    }

    @Override
    public void updateTable(EntityMetadata metadata) throws Exception {
        // YAML is schema-less
    }

    @Override
    public boolean tableExists(EntityMetadata metadata) throws Exception {
        return Files.exists(basePath.resolve(metadata.getTableName() + ".yml"));
    }

    @Override
    public <T extends Entity> void insert(T entity, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        Map<String, Object> record = entityToMap(entity, metadata);
        table.add(record);
        saveTable(metadata.getTableName());
    }

    @Override
    public <T extends Entity> void update(T entity, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        for (int i = 0; i < table.size(); i++) {
            if (table.get(i).get("id").equals(entity.getId())) {
                table.set(i, entityToMap(entity, metadata));
                saveTable(metadata.getTableName());
                return;
            }
        }
    }

    @Override
    public <T extends Entity> void delete(T entity, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        table.removeIf(record -> record.get("id").equals(entity.getId()));
        saveTable(metadata.getTableName());
    }

    @Override
    public <T extends Entity> Optional<T> findById(Object id, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        for (Map<String, Object> record : table) {
            if (record.get("id").equals(id)) {
                return Optional.of(mapToEntity(record, entityClass, metadata));
            }
        }
        return Optional.empty();
    }

    @Override
    public <T extends Entity> List<T> findAll(Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        List<T> results = new ArrayList<>();
        for (Map<String, Object> record : table) {
            results.add(mapToEntity(record, entityClass, metadata));
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        List<T> results = new ArrayList<>();
        for (Map<String, Object> record : table) {
            if (record.get(fieldName).equals(value)) {
                results.add(mapToEntity(record, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> long count(Class<T> entityClass, EntityMetadata metadata) throws Exception {
        return getTable(metadata.getTableName()).size();
    }

    @Override
    public <T extends Entity> long countByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        return table.stream().filter(record -> record.get(fieldName).equals(value)).count();
    }

    @Override
    public <T extends Entity> List<T> executeQuery(String query, List<Object> params, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        throw new UnsupportedOperationException("Query execution not supported in YAML adapter");
    }

    @Override
    public <T extends Entity> List<T> findByFieldPaged(String fieldName, Object value, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        List<T> results = new ArrayList<>();
        int skip = page * pageSize;
        int count = 0;
        for (Map<String, Object> record : table) {
            if (fieldName == null || record.get(fieldName).equals(value)) {
                if (count >= skip && count < skip + pageSize) {
                    results.add(mapToEntity(record, entityClass, metadata));
                }
                count++;
                if (count >= skip + pageSize) break;
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> void insertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        for (T entity : entities) {
            table.add(entityToMap(entity, metadata));
        }
        saveTable(metadata.getTableName());
    }

    @Override
    public <T extends Entity> void updateBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        for (T entity : entities) {
            for (int i = 0; i < table.size(); i++) {
                if (table.get(i).get("id").equals(entity.getId())) {
                    table.set(i, entityToMap(entity, metadata));
                }
            }
        }
        saveTable(metadata.getTableName());
    }

    @Override
    public <T extends Entity> void deleteBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        for (T entity : entities) {
            table.removeIf(record -> record.get("id").equals(entity.getId()));
        }
        saveTable(metadata.getTableName());
    }

    @Override
    public <T extends Entity> List<T> findAllSorted(String orderByField, boolean ascending, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        List<T> results = new ArrayList<>();
        for (Map<String, Object> record : table) {
            results.add(mapToEntity(record, entityClass, metadata));
        }
        // TODO: Implement sorting
        return results;
    }

    @Override
    public <T extends Entity> List<T> findAllSortedPaged(String orderByField, boolean ascending, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<T> all = findAllSorted(orderByField, ascending, entityClass, metadata);
        int skip = page * pageSize;
        return new ArrayList<>(all.subList(skip, Math.min(skip + pageSize, all.size())));
    }

    @Override
    public <T extends Entity> List<T> findByFieldSorted(String whereField, Object whereValue, String orderField, boolean ascending, int limit, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<Map<String, Object>> table = getTable(metadata.getTableName());
        List<T> results = new ArrayList<>();
        for (Map<String, Object> record : table) {
            Object val = record.get(whereField);
            if (val != null && val.equals(whereValue)) {
                results.add(mapToEntity(record, entityClass, metadata));
            }
        }
        return results.size() > limit ? new ArrayList<>(results.subList(0, limit)) : results;
    }

    @Override
    public String getAdapterName() {
        return "YAML";
    }

    @Override
    public boolean supportsBatchOperations() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return false;
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    private List<Map<String, Object>> getTable(String tableName) {
        return tableCache.computeIfAbsent(tableName, k -> {
            try {
                loadTableFromFile(k);
                return tableCache.get(k);
            } catch (Exception e) {
                DebugUtils.logInternalWarn("Failed to load table " + k);
                return new ArrayList<>();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void loadTableFromFile(String tableName) throws IOException {
        Path filePath = basePath.resolve(tableName + ".yml");
        if (Files.exists(filePath)) {
            try (InputStream input = Files.newInputStream(filePath)) {
                Object data = yaml.load(input);
                if (data instanceof List) {
                    tableCache.put(tableName, (List<Map<String, Object>>) data);
                } else {
                    tableCache.put(tableName, new ArrayList<>());
                }
            }
        } else {
            tableCache.put(tableName, new ArrayList<>());
        }
    }

    private void saveTable(String tableName) throws IOException {
        Path filePath = basePath.resolve(tableName + ".yml");
        List<Map<String, Object>> table = tableCache.get(tableName);
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            yaml.dump(table, writer);
        }
    }

    private void saveAll() throws IOException {
        for (String tableName : tableCache.keySet()) {
            saveTable(tableName);
        }
    }

    private Map<String, Object> entityToMap(Entity entity, EntityMetadata metadata) {
        Map<String, Object> map = new HashMap<>();
        for (FieldDescriptor field : metadata.getFields()) {
            Object value = field.getValue(entity);
            map.put(field.getColumnName(), value);
        }
        return map;
    }

    private <T extends Entity> T mapToEntity(Map<String, Object> map, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        for (FieldDescriptor field : metadata.getFields()) {
            Object value = map.get(field.getColumnName());
            if (value != null) {
                field.setValue(entity, value);
            }
        }
        return entity;
    }
}
