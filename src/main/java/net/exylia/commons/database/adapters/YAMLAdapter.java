package net.exylia.commons.database.adapters;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.exceptions.ConnectionException;
import net.exylia.commons.database.exceptions.DatabaseErrorHandler;
import net.exylia.commons.database.exceptions.DatabaseException;
import net.exylia.commons.database.exceptions.SerializationException;
import net.exylia.commons.database.serialization.CollectionUtils;
import net.exylia.commons.database.serialization.EnumSafetyHandler;
import net.exylia.commons.database.serialization.SerializationHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.*;

public class YAMLAdapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final ExyliaPlugin plugin;
    private final DatabaseErrorHandler errorHandler;
    private File dataDirectory;
    private final Map<Class<?>, YamlConfiguration> entityConfigs = new ConcurrentHashMap<>();
    private final Map<Class<?>, File> entityFiles = new ConcurrentHashMap<>();
    private boolean connected = false;

    public YAMLAdapter(FileConfiguration config, ExyliaPlugin plugin, DatabaseErrorHandler errorHandler) {
        this.config = config;
        this.plugin = plugin;
        this.errorHandler = errorHandler;
    }

    @Override
    public void connect() throws Exception {
        try {
            String dataPath = config.getString("database.yaml.directory", "data");
            dataDirectory = new File(plugin.getDataFolder(), dataPath);

            if (!dataDirectory.exists()) {
                if (!dataDirectory.mkdirs()) {
                    throw new ConnectionException("YAML", dataDirectory.getAbsolutePath(),
                            "Failed to create YAML data directory", null);
                }
            }

            if (!dataDirectory.canWrite()) {
                throw new ConnectionException("YAML", dataDirectory.getAbsolutePath(),
                        "No write permission for YAML data directory", null);
            }

            connected = true;
            logInternalInfo("YAML adapter connected successfully. Data directory: " + dataDirectory.getAbsolutePath());

        } catch (Exception e) {
            throw new ConnectionException("YAML", "file system", "Failed to initialize YAML adapter", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            // Save all configurations before disconnecting
            for (Map.Entry<Class<?>, YamlConfiguration> entry : entityConfigs.entrySet()) {
                try {
                    File file = entityFiles.get(entry.getKey());
                    if (file != null) {
                        entry.getValue().save(file);
                    }
                } catch (IOException e) {
                    errorHandler.logWarning("Disconnect", entry.getKey().getSimpleName(),
                            "Failed to save YAML file during disconnect: " + e.getMessage());
                }
            }

            entityConfigs.clear();
            entityFiles.clear();
            connected = false;
            logInternalInfo("YAML adapter disconnected successfully");

        } catch (Exception e) {
            errorHandler.logWarning("Disconnect", "YAMLAdapter", "Error during YAML adapter disconnect: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return connected && dataDirectory != null && dataDirectory.exists() && dataDirectory.canWrite();
    }

    private YamlConfiguration getEntityConfig(Class<?> entityClass) throws Exception {
        YamlConfiguration yamlConfig = entityConfigs.get(entityClass);
        if (yamlConfig == null) {
            String fileName = getTableName(entityClass) + ".yml";
            File entityFile = new File(dataDirectory, fileName);

            yamlConfig = YamlConfiguration.loadConfiguration(entityFile);
            entityConfigs.put(entityClass, yamlConfig);
            entityFiles.put(entityClass, entityFile);

            logInternalDebug(debug(), "Loaded YAML configuration for entity: " + entityClass.getSimpleName());
        }
        return yamlConfig;
    }

    private void saveEntityConfig(Class<?> entityClass) throws Exception {
        YamlConfiguration yamlConfig = entityConfigs.get(entityClass);
        File entityFile = entityFiles.get(entityClass);

        if (yamlConfig != null && entityFile != null) {
            try {
                yamlConfig.save(entityFile);
                logInternalDebug(debug(), "Saved YAML configuration for entity: " + entityClass.getSimpleName());
            } catch (IOException e) {
                throw new DatabaseException("SaveConfig", entityClass.getSimpleName(), "YAML",
                        "Failed to save YAML configuration file", e);
            }
        }
    }

    @Override
    public <T> void save(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entity.getClass());
            Map<String, Object> entityMap = entityToMap(entity);

            String primaryKey = getPrimaryKeyField(entity.getClass());
            Object primaryKeyValue = entityMap.get(primaryKey);

            if (primaryKeyValue == null) {
                // Generate a UUID for entities without primary key
                primaryKeyValue = UUID.randomUUID().toString();
                setPrimaryKeyValue(entity, primaryKey, primaryKeyValue);
                entityMap.put(primaryKey, primaryKeyValue);
            }

            String entityPath = "entities." + primaryKeyValue;

            // Check if entity already exists
            if (yamlConfig.isConfigurationSection(entityPath)) {
                throw new DatabaseException("Save", entityClassName, "YAML",
                        "Entity with primary key " + primaryKeyValue + " already exists. Use update() instead.");
            }

            // Save entity data
            for (Map.Entry<String, Object> entry : entityMap.entrySet()) {
                yamlConfig.set(entityPath + "." + entry.getKey(), entry.getValue());
            }

            saveEntityConfig(entity.getClass());
            logInternalDebug(debug(), "Entity saved to YAML: " + entityClassName + " with ID: " + primaryKeyValue);

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Save", entityClassName, "YAML",
                        "Unexpected error during save operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> void saveOrUpdateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String entityClassName = entities.get(0).getClass().getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entities.get(0).getClass());
            String primaryKey = getPrimaryKeyField(entities.get(0).getClass());

            int successCount = 0;
            int failureCount = 0;

            for (T entity : entities) {
                try {
                    Map<String, Object> entityMap = entityToMap(entity);
                    Object primaryKeyValue = entityMap.get(primaryKey);

                    if (primaryKeyValue == null) {
                        // Generate a UUID for entities without primary key
                        primaryKeyValue = UUID.randomUUID().toString();
                        setPrimaryKeyValue(entity, primaryKey, primaryKeyValue);
                        entityMap.put(primaryKey, primaryKeyValue);
                    }

                    String entityPath = "entities." + primaryKeyValue;

                    // Save/update entity data
                    for (Map.Entry<String, Object> entry : entityMap.entrySet()) {
                        yamlConfig.set(entityPath + "." + entry.getKey(), entry.getValue());
                    }

                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                            "Failed to process entity in batch: " + e.getMessage());
                }
            }

            saveEntityConfig(entities.get(0).getClass());
            logInternalInfo("saveOrUpdateAll completed: " + successCount + " of " + entities.size() + " entities processed");

            if (failureCount > 0) {
                errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                        String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("SaveOrUpdateAll", entityClassName, "YAML",
                        "Unexpected error during batch saveOrUpdate operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> void updateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String entityClassName = entities.get(0).getClass().getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entities.get(0).getClass());
            String primaryKey = getPrimaryKeyField(entities.get(0).getClass());

            int successCount = 0;
            int failureCount = 0;

            for (T entity : entities) {
                try {
                    Map<String, Object> entityMap = entityToMap(entity);
                    Object primaryKeyValue = entityMap.get(primaryKey);

                    if (primaryKeyValue == null) {
                        failureCount++;
                        errorHandler.logWarning("UpdateAll", entityClassName,
                                "Cannot update entity without primary key value");
                        continue;
                    }

                    String entityPath = "entities." + primaryKeyValue;

                    if (!yamlConfig.isConfigurationSection(entityPath)) {
                        failureCount++;
                        errorHandler.logWarning("UpdateAll", entityClassName,
                                "Entity with primary key " + primaryKeyValue + " does not exist");
                        continue;
                    }

                    // Update entity data
                    for (Map.Entry<String, Object> entry : entityMap.entrySet()) {
                        yamlConfig.set(entityPath + "." + entry.getKey(), entry.getValue());
                    }

                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorHandler.logWarning("UpdateAll", entityClassName,
                            "Failed to update entity in batch: " + e.getMessage());
                }
            }

            saveEntityConfig(entities.get(0).getClass());
            logInternalInfo("updateAll completed: " + successCount + " of " + entities.size() + " entities updated");

            if (failureCount > 0) {
                errorHandler.logWarning("UpdateAll", entityClassName,
                        String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("UpdateAll", entityClassName, "YAML",
                        "Unexpected error during batch update operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> void update(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entity.getClass());
            Map<String, Object> entityMap = entityToMap(entity);

            String primaryKey = getPrimaryKeyField(entity.getClass());
            Object primaryKeyValue = entityMap.get(primaryKey);

            if (primaryKeyValue == null) {
                throw new DatabaseException("Update", entityClassName, "YAML",
                        "Cannot update entity without primary key value");
            }

            String entityPath = "entities." + primaryKeyValue;

            if (!yamlConfig.isConfigurationSection(entityPath)) {
                throw new DatabaseException("Update", entityClassName, "YAML",
                        "Entity with primary key " + primaryKeyValue + " does not exist");
            }

            // Update entity data
            for (Map.Entry<String, Object> entry : entityMap.entrySet()) {
                yamlConfig.set(entityPath + "." + entry.getKey(), entry.getValue());
            }

            saveEntityConfig(entity.getClass());
            logInternalDebug(debug(), "Entity updated in YAML: " + entityClassName + " with ID: " + primaryKeyValue);

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Update", entityClassName, "YAML",
                        "Unexpected error during update operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> void delete(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entity.getClass());
            String primaryKey = getPrimaryKeyField(entity.getClass());
            Object primaryKeyValue = entityToMap(entity).get(primaryKey);

            if (primaryKeyValue == null) {
                throw new DatabaseException("Delete", entityClassName, "YAML",
                        "Cannot delete entity without primary key value");
            }

            String entityPath = "entities." + primaryKeyValue;

            if (!yamlConfig.isConfigurationSection(entityPath)) {
                errorHandler.logWarning("Delete", entityClassName,
                        "Entity with primary key " + primaryKeyValue + " does not exist");
                return;
            }

            yamlConfig.set(entityPath, null);
            saveEntityConfig(entity.getClass());
            logInternalDebug(debug(), "Entity deleted from YAML: " + entityClassName + " with ID: " + primaryKeyValue);

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Delete", entityClassName, "YAML",
                        "Unexpected error during delete operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityClass, Object id) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entityClass);
            String entityPath = "entities." + id;

            ConfigurationSection entitySection = yamlConfig.getConfigurationSection(entityPath);
            if (entitySection == null) {
                return Optional.empty();
            }

            try {
                Map<String, Object> entityMap = new HashMap<>();
                for (String key : entitySection.getKeys(false)) {
                    entityMap.put(key, entitySection.get(key));
                }

                T entity = mapToEntity(entityMap, entityClass);
                return Optional.of(entity);
            } catch (Exception e) {
                throw new DatabaseException("FindById", entityClassName, "YAML",
                        "Failed to map YAML data to entity for ID: " + id, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindById", entityClassName, "YAML",
                        "Unexpected error during findById operation for ID: " + id, e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            YamlConfiguration yamlConfig = getEntityConfig(entityClass);
            ConfigurationSection entitiesSection = yamlConfig.getConfigurationSection("entities");

            List<T> results = new ArrayList<>();

            if (entitiesSection != null) {
                for (String entityId : entitiesSection.getKeys(false)) {
                    try {
                        ConfigurationSection entitySection = entitiesSection.getConfigurationSection(entityId);
                        if (entitySection != null) {
                            Map<String, Object> entityMap = new HashMap<>();
                            for (String key : entitySection.getKeys(false)) {
                                entityMap.put(key, entitySection.get(key));
                            }

                            results.add(mapToEntity(entityMap, entityClass));
                        }
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAll", entityClassName,
                                "Failed to map one YAML entity to object: " + e.getMessage());
                    }
                }
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAll", entityClassName, "YAML",
                        "Unexpected error during findAll operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findBy(Class<T> entityClass, String field, Object value) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            List<T> allEntities = findAll(entityClass);
            return allEntities.stream()
                    .filter(entity -> {
                        try {
                            Object fieldValue = getFieldValue(entity, field);
                            return Objects.equals(fieldValue, value);
                        } catch (Exception e) {
                            errorHandler.logWarning("FindBy", entityClassName,
                                    "Failed to get field value for filtering: " + e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindBy", entityClassName, "YAML",
                        String.format("Unexpected error during findBy operation for field '%s'", field), e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> executeQuery(Class<T> entityClass, String query, Object... params) throws Exception {
        // For YAML, we interpret query as a simple field:value filter
        String entityClassName = entityClass.getSimpleName();

        try {
            if (query.contains(":") && params.length == 0) {
                String[] parts = query.split(":", 2);
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim();
                    return findBy(entityClass, field, value);
                }
            }

            throw new DatabaseException("ExecuteQuery", entityClassName, "YAML",
                    "YAML adapter only supports simple field:value queries. Example: 'name:john'");

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("ExecuteQuery", entityClassName, "YAML",
                        "Unexpected error during custom query execution", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public int executeUpdate(String query, Object... params) throws Exception {
        // YAML adapter doesn't support raw update queries
        throw new UnsupportedOperationException("executeUpdate is not supported for YAML adapter. Use specific repository methods.");
    }

    @Override
    public void createTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            // For YAML, "creating a table" means creating the YAML file
            YamlConfiguration yamlConfig = getEntityConfig(entityClass);

            // Initialize empty entities section if it doesn't exist
            if (!yamlConfig.isConfigurationSection("entities")) {
                yamlConfig.createSection("entities");
            }

            // Add metadata about the entity
            yamlConfig.set("metadata.entityClass", entityClass.getName());
            yamlConfig.set("metadata.tableName", getTableName(entityClass));
            yamlConfig.set("metadata.created", System.currentTimeMillis());

            // Add field definitions for documentation
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    String fieldName = column.name().isEmpty() ? field.getName() : column.name();

                    yamlConfig.set("metadata.fields." + fieldName + ".type", field.getType().getSimpleName());
                    yamlConfig.set("metadata.fields." + fieldName + ".primaryKey", column.primaryKey());
                    yamlConfig.set("metadata.fields." + fieldName + ".nullable", column.nullable());
                    yamlConfig.set("metadata.fields." + fieldName + ".autoSerialize", column.autoSerialize());
                }
            }

            saveEntityConfig(entityClass);
            logInternalInfo("YAML table (file) created: " + getTableName(entityClass) + ".yml");

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("CreateTable", entityClassName, "YAML",
                        "Unexpected error during table creation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public void updateTable(Class<?> entityClass) throws Exception {
        // For YAML, table update is the same as creation since YAML is schema-less
        try {
            createTable(entityClass);
        } catch (Exception e) {
            String entityClassName = entityClass.getSimpleName();
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("UpdateTable", entityClassName, "YAML",
                        "Unexpected error during table update", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public boolean tableExists(Class<?> entityClass) throws Exception {
        try {
            String fileName = getTableName(entityClass) + ".yml";
            File entityFile = new File(dataDirectory, fileName);
            return entityFile.exists();
        } catch (Exception e) {
            String entityClassName = entityClass.getSimpleName();
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("TableExists", entityClassName, "YAML",
                        "Unexpected error while checking table existence", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public List<String> getTableColumns(Class<?> entityClass) throws Exception {
        // Return fields defined in the entity class
        List<String> fields = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                fields.add(fieldName);
            }
        }

        return fields;
    }

    @Override
    public void beginTransaction() throws Exception {
        // YAML doesn't support transactions, but we can implement a simple backup mechanism
        throw new UnsupportedOperationException("Transactions not supported for YAML adapter");
    }

    @Override
    public void commit() throws Exception {
        // Save all pending configurations
        for (Class<?> entityClass : entityConfigs.keySet()) {
            saveEntityConfig(entityClass);
        }
    }

    @Override
    public void rollback() throws Exception {
        // Reload all configurations from disk
        for (Class<?> entityClass : entityConfigs.keySet()) {
            File entityFile = entityFiles.get(entityClass);
            if (entityFile != null && entityFile.exists()) {
                YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(entityFile);
                entityConfigs.put(entityClass, yamlConfig);
            }
        }
    }

    @Override
    public String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    @Override
    public Map<String, Object> entityToMap(Object entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();
        Map<String, Object> map = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();

                try {
                    Object value = field.get(entity);

                    // Handle enum serialization
                    if (value != null && value.getClass().isEnum()) {
                        value = ((Enum<?>) value).name();
                    } else if (value != null && column.autoSerialize()) {
                        try {
                            value = SerializationHelper.autoSerializeValue(value, field, column.serializationType());
                        } catch (SerializationException e) {
                            errorHandler.handleError(e);
                            throw e;
                        } catch (Exception e) {
                            SerializationException serException = new SerializationException("Serialize",
                                    entityClassName, fieldName, column.serializationType().toString(), value,
                                    "Failed to auto-serialize field during entityToMap", e);
                            errorHandler.handleError(serException);
                            throw serException;
                        }
                    }

                    map.put(fieldName, value);

                } catch (IllegalAccessException e) {
                    throw new DatabaseException("EntityToMap", entityClassName, "YAML",
                            "Failed to access field: " + fieldName, e);
                }
            }
        }

        return map;
    }

    @Override
    public <T> T mapToEntity(Map<String, Object> map, Class<T> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            Field[] fields = entityClass.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    String columnName = column.name().isEmpty() ? field.getName() : column.name();

                    Object value = map.get(columnName);

                    // Initialize empty collections automatically
                    if (value == null && CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                        try {
                            Object emptyCollection = CollectionUtils.createEmptyCollection(field);
                            if (emptyCollection != null) {
                                field.set(entity, emptyCollection);
                                continue;
                            }
                        } catch (Exception e) {
                            errorHandler.logWarning("MapToEntity", entityClassName,
                                    "Failed to initialize empty collection for field " + columnName + ": " + e.getMessage());
                        }
                    }

                    if (value != null) {
                        try {
                            // Handle enum deserialization
                            if (field.getType().isEnum() && value instanceof String) {
                                value = EnumSafetyHandler.handleEnumDeserialization(value, field,
                                        entityClassName, columnName, errorHandler);
                            } else if (column.autoSerialize()) {
                                try {
                                    value = SerializationHelper.autoDeserializeValue(value, field, column.serializationType());
                                } catch (SerializationException e) {
                                    errorHandler.handleError(e);

                                    // Fallback for collections
                                    if (CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                                        errorHandler.logWarning("MapToEntity", entityClassName,
                                                "Deserialization failed for collection " + columnName + ", initializing empty collection");
                                        value = CollectionUtils.createEmptyCollection(field);
                                    } else {
                                        throw e;
                                    }
                                } catch (Exception e) {
                                    SerializationException serException = new SerializationException("Deserialize",
                                            entityClassName, columnName, column.serializationType().toString(), value,
                                            "Failed to auto-deserialize field during mapToEntity", e);
                                    errorHandler.handleError(serException);

                                    // Fallback for collections
                                    if (CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                                        errorHandler.logWarning("MapToEntity", entityClassName,
                                                "Deserialization failed for collection " + columnName + ", initializing empty collection");
                                        value = CollectionUtils.createEmptyCollection(field);
                                    } else {
                                        throw serException;
                                    }
                                }
                            } else {
                                value = convertValue(value, field.getType());
                            }

                            field.set(entity, value);

                        } catch (IllegalAccessException e) {
                            throw new DatabaseException("MapToEntity", entityClassName, "YAML",
                                    "Failed to set field value: " + columnName, e);
                        }
                    }
                }
            }

            return entity;

        } catch (Exception e) {
            if (e instanceof DatabaseException || e instanceof SerializationException) {
                throw e;
            } else {
                throw new DatabaseException("MapToEntity", entityClassName, "YAML",
                        "Failed to create entity instance or map fields", e);
            }
        }
    }

    // Helper methods
    private String getPrimaryKeyField(Class<?> entityClass) {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    return column.name().isEmpty() ? field.getName() : column.name();
                }
            }
        }
        return "id"; // Default primary key field name
    }

    private void setPrimaryKeyValue(Object entity, String primaryKeyField, Object value) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                if (fieldName.equals(primaryKeyField)) {
                    field.setAccessible(true);
                    try {
                        field.set(entity, convertValue(value, field.getType()));
                    } catch (IllegalAccessException e) {
                        throw new DatabaseException("SetPrimaryKey", entity.getClass().getSimpleName(), "YAML",
                                "Failed to set primary key field: " + primaryKeyField, e);
                    }
                    return;
                }
            }
        }
    }

    private Object getFieldValue(Object entity, String fieldName) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();
                if (columnName.equals(fieldName)) {
                    field.setAccessible(true);
                    try {
                        return field.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new DatabaseException("GetFieldValue", entity.getClass().getSimpleName(), "YAML",
                                "Failed to get field value: " + fieldName, e);
                    }
                }
            }
        }
        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        try {
            if (targetType == String.class) {
                return value.toString();
            } else if (targetType == int.class || targetType == Integer.class) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return Integer.valueOf(value.toString());
            } else if (targetType == long.class || targetType == Long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return Long.valueOf(value.toString());
            } else if (targetType == double.class || targetType == Double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.valueOf(value.toString());
            } else if (targetType == float.class || targetType == Float.class) {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                return Float.valueOf(value.toString());
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
                return Boolean.valueOf(value.toString());
            } else if (targetType.isEnum() && value instanceof String) {
                @SuppressWarnings("unchecked")
                Class<Enum> enumClass = (Class<Enum>) targetType;
                return Enum.valueOf(enumClass, (String) value);
            }

            return value;
        } catch (Exception e) {
            throw new DatabaseException("Value Conversion", "Unknown", "YAML",
                    String.format("Failed to convert value '%s' to type %s", value, targetType.getSimpleName()), e);
        }
    }
}