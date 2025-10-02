package net.exylia.commons.database.adapters;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.repository.Repository.SortOrder;
import net.exylia.commons.database.exceptions.ConnectionException;
import net.exylia.commons.database.exceptions.DatabaseErrorHandler;
import net.exylia.commons.database.exceptions.DatabaseException;
import net.exylia.commons.database.exceptions.SerializationException;
import net.exylia.commons.database.serialization.CollectionUtils;
import net.exylia.commons.database.serialization.EnumSafetyHandler;
import net.exylia.commons.database.serialization.SerializationHelper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.util.*;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

public class MongoDBAdapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final ExyliaPlugin plugin;
    private final DatabaseErrorHandler errorHandler;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBAdapter(FileConfiguration config, ExyliaPlugin plugin, DatabaseErrorHandler errorHandler) {
        this.config = config;
        this.plugin = plugin;
        this.errorHandler = errorHandler;
    }

    @Override
    public void connect() throws Exception {
        String host = config.getString("database.mongodb.host", "localhost");
        int port = config.getInt("database.mongodb.port", 27017);
        String databaseName = config.getString("database.mongodb.database", "minecraft");
        String username = config.getString("database.mongodb.username", "");
        String password = config.getString("database.mongodb.password", "");
        String authDatabase = config.getString("database.mongodb.auth-database", "admin");

        String connectionString;

        try {
            if (!username.isEmpty() && !password.isEmpty()) {
                connectionString = String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s",
                        username, password, host, port, databaseName, authDatabase);
            } else {
                connectionString = String.format("mongodb://%s:%d", host, port);
            }

            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(databaseName);

            // Test connection
            database.runCommand(new Document("ping", 1));
            logInternalInfo("MongoDB connection established successfully to " + host + ":" + port + "/" + databaseName);

        } catch (Exception e) {
            throw new ConnectionException("MongoDB",
                    String.format("mongodb://%s:%d/%s", host, port, databaseName),
                    "Failed to establish MongoDB connection", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (mongoClient != null) {
                mongoClient.close();
                logInternalInfo("MongoDB connection closed successfully");
            }
        } catch (Exception e) {
            errorHandler.logWarning("Disconnect", "MongoDBAdapter", "Error closing MongoDB connection: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        if (mongoClient == null || database == null) {
            return false;
        }

        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public <T> void save(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            String collectionName = getTableName(entity.getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document document = entityToDocument(entity);
            collection.insertOne(document);

            // If has _id field and is ObjectId, set it back in the entity
            setIdFromDocument(entity, document);

            logInternalDebug("Entity saved successfully to MongoDB collection: " + collectionName);

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Save", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entities.get(0).getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<ReplaceOneModel<Document>> operations = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (T entity : entities) {
                try {
                    Object id = getEntityId(entity);
                    Document document = entityToDocument(entity);

                    if (id != null) {
                        // If has ID, do upsert (update or insert)
                        Document filter = new Document("_id", convertToObjectId(id));
                        ReplaceOptions options = new ReplaceOptions().upsert(true);
                        operations.add(new ReplaceOneModel<>(filter, document, options));
                    } else {
                        // If no ID, it's an insert (MongoDB will generate _id)
                        operations.add(new ReplaceOneModel<>(
                                new Document("_id", new ObjectId()), // Filter that will never match
                                document,
                                new ReplaceOptions().upsert(true)
                        ));
                    }
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                            "Failed to prepare entity for bulk operation: " + e.getMessage());
                }
            }

            if (!operations.isEmpty()) {
                try {
                    BulkWriteResult result = collection.bulkWrite(operations);
                    logInternalDebug("saveOrUpdateAll completed: " +
                            result.getInsertedCount() + " inserted, " +
                            result.getModifiedCount() + " updated, " +
                            result.getUpserts().size() + " upserts");
                } catch (Exception e) {
                    throw new DatabaseException("SaveOrUpdateAll", entityClassName, "MongoDB",
                            "MongoDB bulk write operation failed", e);
                }
            }

            if (failureCount > 0) {
                errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                        String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("SaveOrUpdateAll", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entities.get(0).getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<UpdateOneModel<Document>> operations = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (T entity : entities) {
                try {
                    Object id = getEntityId(entity);

                    if (id == null) {
                        failureCount++;
                        errorHandler.logWarning("UpdateAll", entityClassName,
                                "Cannot update entity without ID in updateAll");
                        continue;
                    }

                    Document filter = new Document("_id", convertToObjectId(id));
                    Document updateDocument = entityToDocument(entity);

                    // Remove _id from update document to avoid errors
                    updateDocument.remove("_id");

                    Document update = new Document("$set", updateDocument);
                    operations.add(new UpdateOneModel<>(filter, update));
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorHandler.logWarning("UpdateAll", entityClassName,
                            "Failed to prepare entity for bulk update: " + e.getMessage());
                }
            }

            if (!operations.isEmpty()) {
                try {
                    BulkWriteResult result = collection.bulkWrite(operations);
                    logInternalInfo("updateAll completed: " +
                            result.getModifiedCount() + " of " + entities.size() + " entities updated");
                } catch (Exception e) {
                    throw new DatabaseException("UpdateAll", entityClassName, "MongoDB",
                            "MongoDB bulk update operation failed", e);
                }
            }

            if (failureCount > 0) {
                errorHandler.logWarning("UpdateAll", entityClassName,
                        String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("UpdateAll", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entity.getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Object id = getEntityId(entity);
            if (id == null) {
                throw new DatabaseException("Update", entityClassName, "MongoDB",
                        "Cannot update entity without ID");
            }

            Document document = entityToDocument(entity);
            Document filter = new Document("_id", convertToObjectId(id));

            try {
                collection.replaceOne(filter, document, new ReplaceOptions().upsert(false));
                logInternalDebug("Entity updated successfully in MongoDB collection: " + collectionName);
            } catch (Exception e) {
                throw new DatabaseException("Update", entityClassName, "MongoDB",
                        "MongoDB update operation failed for ID: " + id, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Update", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entity.getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Object id = getEntityId(entity);
            if (id == null) {
                throw new DatabaseException("Delete", entityClassName, "MongoDB",
                        "Cannot delete entity without ID");
            }

            Document filter = new Document("_id", convertToObjectId(id));

            try {
                collection.deleteOne(filter);
                logInternalDebug("Entity deleted successfully from MongoDB collection: " + collectionName);
            } catch (Exception e) {
                throw new DatabaseException("Delete", entityClassName, "MongoDB",
                        "MongoDB delete operation failed for ID: " + id, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Delete", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document filter = new Document("_id", convertToObjectId(id));

            try {
                Document document = collection.find(filter).first();

                if (document != null) {
                    try {
                        T entity = documentToEntity(document, entityClass);
                        return Optional.of(entity);
                    } catch (Exception e) {
                        throw new DatabaseException("FindById", entityClassName, "MongoDB",
                                "Failed to map document to entity for ID: " + id, e);
                    }
                }

                return Optional.empty();
            } catch (Exception e) {
                throw new DatabaseException("FindById", entityClassName, "MongoDB",
                        "MongoDB find operation failed for ID: " + id, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindById", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();

            try (MongoCursor<Document> cursor = collection.find().iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAll", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindAll", entityClassName, "MongoDB",
                        "MongoDB find operation failed", e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAll", entityClassName, "MongoDB",
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
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();
            Document filter = new Document(field, value);

            try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindBy", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindBy", entityClassName, "MongoDB",
                        String.format("MongoDB findBy operation failed for field '%s' with value: %s", field, value), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindBy", entityClassName, "MongoDB",
                        String.format("Unexpected error during findBy operation for field '%s'", field), e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> executeQuery(Class<T> entityClass, String query, Object... params) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            // For MongoDB, interpret query as JSON filter
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document filter;
            try {
                filter = Document.parse(query);
            } catch (Exception e) {
                throw new DatabaseException("ExecuteQuery", entityClassName, "MongoDB",
                        "Failed to parse query as JSON filter: " + query, e);
            }

            List<T> results = new ArrayList<>();

            try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("ExecuteQuery", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("ExecuteQuery", entityClassName, "MongoDB",
                        "MongoDB query execution failed: " + query, e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("ExecuteQuery", entityClassName, "MongoDB",
                        "Unexpected error during custom query execution", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public int executeUpdate(String query, Object... params) throws Exception {
        // For bulk update operations in MongoDB
        throw new UnsupportedOperationException("executeUpdate is not implemented for MongoDB. Use specific repository methods.");
    }

    @Override
    public void createTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            // MongoDB creates collections automatically, but we can create indexes here
            String collectionName = getTableName(entityClass);

            // Check if collection already exists
            boolean exists = false;
            for (String name : database.listCollectionNames()) {
                if (name.equals(collectionName)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                try {
                    database.createCollection(collectionName);
                    logInternalInfo("MongoDB collection created: " + collectionName);
                } catch (Exception e) {
                    throw new DatabaseException("CreateTable", entityClassName, "MongoDB",
                            "Failed to create MongoDB collection: " + collectionName, e);
                }
            }

            // Create indexes for unique fields
            MongoCollection<Document> collection = database.getCollection(collectionName);
            Field[] fields = entityClass.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    if (column.unique() && !column.primaryKey()) {
                        String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                        try {
                            collection.createIndex(new Document(fieldName, 1));
                            logInternalInfo("Index created for unique field: " + fieldName);
                        } catch (Exception e) {
                            errorHandler.logWarning("CreateTable", entityClassName,
                                    "Failed to create index for field " + fieldName + ": " + e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("CreateTable", entityClassName, "MongoDB",
                        "Unexpected error during collection creation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public void updateTable(Class<?> entityClass) throws Exception {
        // MongoDB is schema-less, so we only need to ensure collection exists
        try {
            createTable(entityClass);
        } catch (Exception e) {
            String entityClassName = entityClass.getSimpleName();
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("UpdateTable", entityClassName, "MongoDB",
                        "Unexpected error during collection update", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public boolean tableExists(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);

            try {
                for (String name : database.listCollectionNames()) {
                    if (name.equals(collectionName)) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                throw new DatabaseException("TableExists", entityClassName, "MongoDB",
                        "Failed to check collection existence: " + collectionName, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("TableExists", entityClassName, "MongoDB",
                        "Unexpected error while checking collection existence", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public List<String> getTableColumns(Class<?> entityClass) throws Exception {
        // In MongoDB, return fields defined in the entity
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
        // MongoDB supports transactions only in replica sets
        // For simplicity, we don't implement transactions in this version
        throw new UnsupportedOperationException("Transactions not implemented for MongoDB in this version");
    }

    @Override
    public void commit() throws Exception {
        // Not implemented
        throw new UnsupportedOperationException("Transaction commit not implemented for MongoDB");
    }

    @Override
    public void rollback() throws Exception {
        // Not implemented
        throw new UnsupportedOperationException("Transaction rollback not implemented for MongoDB");
    }

    @Override
    public String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    // Enhanced entityToMap with better error handling
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

                    // AUTO-SERIALIZATION with enhanced error handling
                    if (value != null && column.autoSerialize()) {
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

                    if (value != null) {
                        map.put(fieldName, value);
                    }

                } catch (IllegalAccessException e) {
                    throw new DatabaseException("EntityToMap", entityClassName, "MongoDB",
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

                    if (value == null && CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                        try {
                            Object emptyCollection = CollectionUtils.createEmptyCollection(field);
                            if (emptyCollection != null) {
                                field.set(entity, emptyCollection);
                                continue; // Skip further processing for this field
                            }
                        } catch (Exception e) {
                            errorHandler.logWarning("MapToEntity", entityClassName,
                                    "Failed to initialize empty collection for field " + columnName + ": " + e.getMessage());
                        }
                    }

                    if (value != null) {
                        try {
                            if (field.getType().isEnum() && value instanceof String) {
                                @SuppressWarnings("unchecked")
                                Class<Enum> enumClass = (Class<Enum>) field.getType();
                                value = Enum.valueOf(enumClass, (String) value);
                            }
                            else if (column.autoSerialize()) {
                                try {
                                    value = SerializationHelper.autoDeserializeValue(value, field, column.serializationType());
                                } catch (SerializationException e) {
                                    errorHandler.handleError(e);

                                    // FALLBACK: Si falla la deserialización de una colección, crear una vacía
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

                                    // FALLBACK: Si falla la deserialización de una colección, crear una vacía
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
                            throw new DatabaseException("MapToEntity", entityClassName, getAdapterType(),
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
                throw new DatabaseException("MapToEntity", entityClassName, getAdapterType(),
                        "Failed to create entity instance or map fields", e);
            }
        }
    }

    private String getAdapterType() {
        return this.getClass().getSimpleName().replace("Adapter", "");
    }

    // MongoDB-specific methods with enhanced error handling
    private Document entityToDocument(Object entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();
        Document document = new Document();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName;

                if (column.primaryKey()) {
                    fieldName = "_id";
                } else {
                    fieldName = column.name().isEmpty() ? field.getName() : column.name();
                }

                try {
                    Object value = field.get(entity);

                    // Skip autoIncrement fields with value 0 (not yet generated)
                    if (column.autoIncrement() && value != null) {
                        if ((value instanceof Integer && (Integer) value == 0) ||
                            (value instanceof Long && (Long) value == 0L)) {
                            continue;
                        }
                    }

                    if (value != null && value.getClass().isEnum()) {
                        value = ((Enum<?>) value).name(); // Convertir a string
                    } else if (value != null && column.autoSerialize()) {
                        try {
                            value = SerializationHelper.autoSerializeValue(value, field, column.serializationType());
                        } catch (SerializationException e) {
                            errorHandler.handleError(e);
                            throw e;
                        } catch (Exception e) {
                            SerializationException serException = new SerializationException("Serialize",
                                    entityClassName, fieldName, column.serializationType().toString(), value,
                                    "Failed to auto-serialize field during document creation", e);
                            errorHandler.handleError(serException);
                            throw serException;
                        }
                    }

                    if (value != null) {
                        if (fieldName.equals("_id") && value instanceof String) {
                            // Convert String to ObjectId if necessary
                            try {
                                document.put(fieldName, new ObjectId(value.toString()));
                            } catch (IllegalArgumentException e) {
                                // If not a valid ObjectId, use as String
                                document.put(fieldName, value);
                            }
                        } else {
                            document.put(fieldName, value);
                        }
                    }

                } catch (IllegalAccessException e) {
                    throw new DatabaseException("EntityToDocument", entityClassName, "MongoDB",
                            "Failed to access field: " + fieldName, e);
                }
            }
        }

        return document;
    }

    private <T> T documentToEntity(Document document, Class<T> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            Field[] fields = entityClass.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    String fieldName;

                    if (column.primaryKey()) {
                        fieldName = "_id";
                    } else {
                        fieldName = column.name().isEmpty() ? field.getName() : column.name();
                    }

                    Object value = document.get(fieldName);
                    if (value != null) {
                        try {
                            // Manejo especial para enums
                            if (field.getType().isEnum() && value instanceof String) {
                                value = EnumSafetyHandler.handleEnumDeserialization(value, field,
                                        entityClassName, fieldName, errorHandler);
                            }
                            else if (column.autoSerialize()) {
                                try {
                                    value = SerializationHelper.autoDeserializeValue(value, field, column.serializationType());
                                } catch (SerializationException e) {
                                    errorHandler.handleError(e);
                                    throw e;
                                } catch (Exception e) {
                                    SerializationException serException = new SerializationException("Deserialize",
                                            entityClassName, fieldName, column.serializationType().toString(), value,
                                            "Failed to auto-deserialize field during entity creation", e);
                                    errorHandler.handleError(serException);
                                    throw serException;
                                }
                            } else {
                                if (fieldName.equals("_id") && value instanceof ObjectId && field.getType() == String.class) {
                                    // Convert ObjectId to String
                                    value = value.toString();
                                } else {
                                    value = convertValue(value, field.getType());
                                }
                            }

                            field.set(entity, value);

                        } catch (IllegalAccessException e) {
                            throw new DatabaseException("DocumentToEntity", entityClassName, "MongoDB",
                                    "Failed to set field value: " + fieldName, e);
                        }
                    }
                }
            }

            return entity;

        } catch (Exception e) {
            if (e instanceof DatabaseException || e instanceof SerializationException) {
                throw e;
            } else {
                throw new DatabaseException("DocumentToEntity", entityClassName, "MongoDB",
                        "Failed to create entity instance or map fields", e);
            }
        }
    }

    private Object getEntityId(Object entity) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    field.setAccessible(true);
                    try {
                        return field.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new DatabaseException("GetEntityId", entity.getClass().getSimpleName(), "MongoDB",
                                "Failed to access primary key field", e);
                    }
                }
            }
        }

        return null;
    }

    private void setIdFromDocument(Object entity, Document document) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    field.setAccessible(true);
                    Object id = document.get("_id");

                    if (id != null) {
                        try {
                            if (field.getType() == String.class && id instanceof ObjectId) {
                                field.set(entity, id.toString());
                            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (id instanceof Number) {
                                    field.set(entity, ((Number) id).intValue());
                                } else if (id instanceof ObjectId) {
                                    // For autoIncrement int fields, MongoDB generates ObjectId
                                    // We need a different strategy - use a counter collection
                                    field.set(entity, id.hashCode());
                                }
                            } else if (field.getType() == long.class || field.getType() == Long.class) {
                                if (id instanceof Number) {
                                    field.set(entity, ((Number) id).longValue());
                                } else if (id instanceof ObjectId) {
                                    field.set(entity, (long) id.hashCode());
                                }
                            } else {
                                field.set(entity, id);
                            }
                        } catch (IllegalAccessException e) {
                            throw new DatabaseException("SetIdFromDocument", entity.getClass().getSimpleName(), "MongoDB",
                                    "Failed to set ID field from document", e);
                        }
                    }
                    break;
                }
            }
        }
    }

    private Object convertToObjectId(Object id) {
        if (id instanceof String) {
            try {
                return new ObjectId(id.toString());
            } catch (IllegalArgumentException e) {
                return id;
            }
        }
        return id;
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
                return Boolean.valueOf(value.toString());
            }

            return value;
        } catch (Exception e) {
            throw new DatabaseException("Value Conversion", "Unknown", "MongoDB",
                    String.format("Failed to convert value '%s' to type %s", value, targetType.getSimpleName()), e);
        }
    }

    @Override
    public <T> List<T> findAllOrderedBy(Class<T> entityClass, String field, SortOrder order) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();
            int sortDirection = (order == SortOrder.DESC) ? -1 : 1;

            try (MongoCursor<Document> cursor = collection.find().sort(sortDirection == -1 ? Sorts.descending(field) : Sorts.ascending(field)).iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllOrderedBy", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindAllOrderedBy", entityClassName, "MongoDB",
                        String.format("MongoDB ordered find operation failed for field '%s'", field), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllOrderedBy", entityClassName, "MongoDB",
                        "Unexpected error during ordered find operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAllOrderedBy(Class<T> entityClass, String field, SortOrder order, int limit) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();
            int sortDirection = (order == SortOrder.DESC) ? -1 : 1;

            try (MongoCursor<Document> cursor = collection.find()
                    .sort(sortDirection == -1 ? Sorts.descending(field) : Sorts.ascending(field))
                    .limit(limit)
                    .iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllOrderedBy", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindAllOrderedBy", entityClassName, "MongoDB",
                        String.format("MongoDB limited ordered find operation failed for field '%s' with limit %d", field, limit), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllOrderedBy", entityClassName, "MongoDB",
                        "Unexpected error during limited ordered find operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAllPaged(Class<T> entityClass, int page, int size) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();
            int skip = page * size;

            try (MongoCursor<Document> cursor = collection.find()
                    .skip(skip)
                    .limit(size)
                    .iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllPaged", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindAllPaged", entityClassName, "MongoDB",
                        String.format("MongoDB paged find operation failed: page %d, size %d", page, size), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllPaged", entityClassName, "MongoDB",
                        "Unexpected error during paged find operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAllPagedOrderedBy(Class<T> entityClass, String field, SortOrder order, int page, int size) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();
            int sortDirection = (order == SortOrder.DESC) ? -1 : 1;
            int skip = page * size;

            try (MongoCursor<Document> cursor = collection.find()
                    .sort(sortDirection == -1 ? Sorts.descending(field) : Sorts.ascending(field))
                    .skip(skip)
                    .limit(size)
                    .iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllPagedOrderedBy", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindAllPagedOrderedBy", entityClassName, "MongoDB",
                        String.format("MongoDB paged ordered find operation failed: field '%s', page %d, size %d", field, page, size), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllPagedOrderedBy", entityClassName, "MongoDB",
                        "Unexpected error during paged ordered find operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> long getRankByField(Class<T> entityClass, String field, Object value, SortOrder order) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            try {
                Document filter;
                if (order == SortOrder.DESC) {
                    filter = new Document(field, new Document("$gt", value));
                } else {
                    filter = new Document(field, new Document("$lt", value));
                }

                long betterCount = collection.countDocuments(filter);
                return betterCount + 1;

            } catch (Exception e) {
                throw new DatabaseException("GetRankByField", entityClassName, "MongoDB",
                        String.format("MongoDB rank calculation failed for field '%s' with value %s", field, value), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("GetRankByField", entityClassName, "MongoDB",
                        "Unexpected error during rank calculation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> Optional<T> getByRank(Class<T> entityClass, String field, long rank, SortOrder order) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            int sortDirection = (order == SortOrder.DESC) ? -1 : 1;
            int skip = (int) Math.max(0, rank - 1);

            try {
                Document document = collection.find()
                        .sort(sortDirection == -1 ? Sorts.descending(field) : Sorts.ascending(field))
                        .skip(skip)
                        .limit(1)
                        .first();

                if (document != null) {
                    try {
                        T entity = documentToEntity(document, entityClass);
                        return Optional.of(entity);
                    } catch (Exception e) {
                        throw new DatabaseException("GetByRank", entityClassName, "MongoDB",
                                "Failed to map document to entity for rank: " + rank, e);
                    }
                }

                return Optional.empty();

            } catch (Exception e) {
                throw new DatabaseException("GetByRank", entityClassName, "MongoDB",
                        String.format("MongoDB rank query failed for rank %d", rank), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("GetByRank", entityClassName, "MongoDB",
                        "Unexpected error during rank query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public void dropTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);

            try {
                // Check if collection exists before dropping
                boolean exists = false;
                for (String name : database.listCollectionNames()) {
                    if (name.equals(collectionName)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    MongoCollection<Document> collection = database.getCollection(collectionName);
                    collection.drop();
                    logInternalInfo("MongoDB collection dropped successfully: " + collectionName);
                } else {
                    logInternalInfo("MongoDB collection does not exist, nothing to drop: " + collectionName);
                }
            } catch (Exception e) {
                throw new DatabaseException("DropTable", entityClassName, "MongoDB",
                        "Failed to drop MongoDB collection: " + collectionName, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("DropTable", entityClassName, "MongoDB",
                        "Unexpected error during collection drop", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }
}