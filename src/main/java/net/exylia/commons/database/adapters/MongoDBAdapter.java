package net.exylia.commons.database.adapters;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.result.DeleteResult;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

@Deprecated
public class MongoDBAdapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final JavaPlugin plugin;
    private final DatabaseErrorHandler errorHandler;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBAdapter(FileConfiguration config, JavaPlugin plugin, DatabaseErrorHandler errorHandler) {
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
            errorHandler.logInternalWarning("Disconnect", "MongoDBAdapter", "Error closing MongoDB connection: " + e.getMessage());
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
    public <T> T save(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            String collectionName = getTableName(entity.getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            setAutoIncrementId(entity, collectionName);

            Document document = entityToDocument(entity);
            collection.insertOne(document);

            setIdFromDocument(entity, document);

            logInternalDebug("Entity saved successfully to MongoDB collection: " + collectionName);

            return entity;

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
    public <T> List<T> saveOrUpdateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return entities;
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

                    boolean isNew = id == null ||
                            (id instanceof Integer && (Integer) id == 0) ||
                            (id instanceof Long && (Long) id == 0L);

                    if (isNew) {
                        setAutoIncrementId(entity, collectionName);
                        id = getEntityId(entity);
                    }

                    Document document = entityToDocument(entity);

                    if (id != null) {
                         
                        Document filter = new Document("_id", convertToObjectId(id));
                        ReplaceOptions options = new ReplaceOptions().upsert(true);
                        operations.add(new ReplaceOneModel<>(filter, document, options));
                    } else {
                         
                        operations.add(new ReplaceOneModel<>(
                                new Document("_id", new ObjectId()),  
                                document,
                                new ReplaceOptions().upsert(true)
                        ));
                    }
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorHandler.logInternalWarning("SaveOrUpdateAll", entityClassName,
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
                errorHandler.logInternalWarning("SaveOrUpdateAll", entityClassName,
                        String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
            }

            return entities;

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
    public <T> List<T> updateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return entities;
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
                        errorHandler.logInternalWarning("UpdateAll", entityClassName,
                                "Cannot update entity without ID in updateAll");
                        continue;
                    }

                    Document filter = new Document("_id", convertToObjectId(id));
                    Document updateDocument = entityToDocument(entity);

                    updateDocument.remove("_id");

                    Document update = new Document("$set", updateDocument);
                    operations.add(new UpdateOneModel<>(filter, update));
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorHandler.logInternalWarning("UpdateAll", entityClassName,
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
                errorHandler.logInternalWarning("UpdateAll", entityClassName,
                        String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
            }

            return entities;

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
    public <T> T update(T entity) throws Exception {
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
                return entity;
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
    public <T> boolean delete(T entity) throws Exception {
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
                DeleteResult result = collection.deleteOne(filter);
                boolean deleted = result.getDeletedCount() > 0;

                if (deleted) {
                    logInternalDebug("Entity deleted successfully from MongoDB collection: " + collectionName);
                } else {
                    errorHandler.logInternalWarning("Delete", entityClassName,
                            "No document was deleted for ID: " + id);
                }

                return deleted;
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
                        errorHandler.logInternalWarning("FindAll", entityClassName,
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
                        errorHandler.logInternalWarning("FindBy", entityClassName,
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
                        errorHandler.logInternalWarning("ExecuteQuery", entityClassName,
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
         
        throw new UnsupportedOperationException("executeUpdate is not implemented for MongoDB. Use specific repository methods.");
    }

    @Override
    public void createTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
             
            String collectionName = getTableName(entityClass);

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
                            errorHandler.logInternalWarning("CreateTable", entityClassName,
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
         
        throw new UnsupportedOperationException("Transactions not implemented for MongoDB in this version");
    }

    @Override
    public void commit() throws Exception {
         
        throw new UnsupportedOperationException("Transaction commit not implemented for MongoDB");
    }

    @Override
    public void rollback() throws Exception {
         
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
                                continue;  
                            }
                        } catch (Exception e) {
                            errorHandler.logInternalWarning("MapToEntity", entityClassName,
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

                                    if (CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                                        errorHandler.logInternalWarning("MapToEntity", entityClassName,
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

                                    if (CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                                        errorHandler.logInternalWarning("MapToEntity", entityClassName,
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

    private Document entityToDocument(Object entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();
        Document document = new Document();
        Field[] fields = entity.getClass().getDeclaredFields();

        List<Field> primaryKeyFields = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    primaryKeyFields.add(field);
                }
            }
        }

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName;

                if (column.primaryKey()) {
                    if (primaryKeyFields.size() > 1) {
                        fieldName = column.name().isEmpty() ? field.getName() : column.name();
                    } else {
                        fieldName = "_id";
                    }
                } else {
                    fieldName = column.name().isEmpty() ? field.getName() : column.name();
                }

                try {
                    Object value = field.get(entity);

                    if (column.autoIncrement() && value != null) {
                        if ((value instanceof Integer && (Integer) value == 0) ||
                            (value instanceof Long && (Long) value == 0L)) {
                            continue;
                        }
                    }

                    if (value != null && value.getClass().isEnum()) {
                        value = ((Enum<?>) value).name();
                    } else if (value instanceof java.util.UUID) {
                        value = value.toString();
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
                             
                            try {
                                document.put(fieldName, new ObjectId(value.toString()));
                            } catch (IllegalArgumentException e) {
                                 
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

        if (primaryKeyFields.size() > 1) {
            StringBuilder compositeId = new StringBuilder();
            for (int i = 0; i < primaryKeyFields.size(); i++) {
                Field pkField = primaryKeyFields.get(i);
                pkField.setAccessible(true);
                try {
                    Object pkValue = pkField.get(entity);
                    if (pkValue != null) {
                        if (i > 0) {
                            compositeId.append(":");
                        }
                        compositeId.append(pkValue.toString());
                    }
                } catch (IllegalAccessException e) {
                    throw new DatabaseException("EntityToDocument", entityClassName, "MongoDB",
                            "Failed to build composite ID", e);
                }
            }
            document.put("_id", compositeId.toString());
        }

        return document;
    }

    private <T> T documentToEntity(Document document, Class<T> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            Field[] fields = entityClass.getDeclaredFields();

            List<Field> primaryKeyFields = new ArrayList<>();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    if (column.primaryKey()) {
                        primaryKeyFields.add(field);
                    }
                }
            }

            String[] compositeIdParts = null;
            if (primaryKeyFields.size() > 1) {
                Object idValue = document.get("_id");
                if (idValue != null) {
                    compositeIdParts = idValue.toString().split(":", primaryKeyFields.size());
                }
            }

            int pkIndex = 0;
            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    String fieldName;
                    Object value;

                    if (column.primaryKey()) {
                        if (primaryKeyFields.size() > 1 && compositeIdParts != null && pkIndex < compositeIdParts.length) {
                            value = compositeIdParts[pkIndex++];
                            fieldName = column.name().isEmpty() ? field.getName() : column.name();
                        } else {
                            fieldName = "_id";
                            value = document.get(fieldName);
                        }
                    } else {
                        fieldName = column.name().isEmpty() ? field.getName() : column.name();
                        value = document.get(fieldName);
                    }
                    if (value != null) {
                        try {
                             
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

        List<Field> primaryKeyFields = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    primaryKeyFields.add(field);
                }
            }
        }

        if (primaryKeyFields.isEmpty()) {
            return null;
        }

        if (primaryKeyFields.size() == 1) {
            Field pkField = primaryKeyFields.get(0);
            pkField.setAccessible(true);
            try {
                return pkField.get(entity);
            } catch (IllegalAccessException e) {
                throw new DatabaseException("GetEntityId", entity.getClass().getSimpleName(), "MongoDB",
                        "Failed to access primary key field", e);
            }
        } else {
            StringBuilder compositeId = new StringBuilder();
            for (int i = 0; i < primaryKeyFields.size(); i++) {
                Field pkField = primaryKeyFields.get(i);
                pkField.setAccessible(true);
                try {
                    Object pkValue = pkField.get(entity);
                    if (pkValue != null) {
                        if (i > 0) {
                            compositeId.append(":");
                        }
                        compositeId.append(pkValue.toString());
                    }
                } catch (IllegalAccessException e) {
                    throw new DatabaseException("GetEntityId", entity.getClass().getSimpleName(), "MongoDB",
                            "Failed to access composite primary key field", e);
                }
            }
            return compositeId.toString();
        }
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
                String strValue = value.toString();
                if (strValue.contains(".")) {
                    return Double.valueOf(strValue).intValue();
                }
                return Integer.valueOf(strValue);
            } else if (targetType == long.class || targetType == Long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                String strValue = value.toString();
                if (strValue.contains(".")) {
                    return Double.valueOf(strValue).longValue();
                }
                return Long.valueOf(strValue);
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
            } else if (targetType == java.util.UUID.class) {
                if (value instanceof String) {
                    return java.util.UUID.fromString((String) value);
                }
                return java.util.UUID.fromString(value.toString());
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
                        errorHandler.logInternalWarning("FindAllOrderedBy", entityClassName,
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
                        errorHandler.logInternalWarning("FindAllOrderedBy", entityClassName,
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
                        errorHandler.logInternalWarning("FindAllPaged", entityClassName,
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
                        errorHandler.logInternalWarning("FindAllPagedOrderedBy", entityClassName,
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

    private int getNextSequenceValue(String sequenceName) throws Exception {
        MongoCollection<Document> counters = database.getCollection("counters");

        Document filter = new Document("_id", sequenceName);
        Document update = new Document("$inc", new Document("sequence_value", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);

        Document result = counters.findOneAndUpdate(filter, update, options);

        if (result != null) {
            Object seqValue = result.get("sequence_value");
            if (seqValue instanceof Number) {
                return ((Number) seqValue).intValue();
            }
        }

        return 1;
    }

    private void setAutoIncrementId(Object entity, String collectionName) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey() && column.autoIncrement()) {
                    field.setAccessible(true);
                    Object currentValue = field.get(entity);

                    if (currentValue != null &&
                        ((currentValue instanceof Integer && (Integer) currentValue == 0) ||
                         (currentValue instanceof Long && (Long) currentValue == 0L))) {

                        int nextId = getNextSequenceValue(collectionName);

                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(entity, nextId);
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            field.set(entity, (long) nextId);
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    public <T> List<T> findByFieldOrderedBy(Class<T> entityClass, String filterField, Object filterValue,
                                              String orderField, SortOrder order, int limit) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();

            Document filter = new Document(filterField, filterValue);
            int sortDirection = (order == SortOrder.DESC) ? -1 : 1;

            try (MongoCursor<Document> cursor = collection.find(filter)
                    .sort(sortDirection == -1 ? Sorts.descending(orderField) : Sorts.ascending(orderField))
                    .limit(limit)
                    .iterator()) {
                while (cursor.hasNext()) {
                    try {
                        Document document = cursor.next();
                        results.add(documentToEntity(document, entityClass));
                    } catch (Exception e) {
                        errorHandler.logInternalWarning("FindByFieldOrderedBy", entityClassName,
                                "Failed to map one document to entity: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException("FindByFieldOrderedBy", entityClassName, "MongoDB",
                        String.format("MongoDB findByFieldOrderedBy operation failed: filter '%s'=%s, order '%s' %s, limit %d",
                                filterField, filterValue, orderField, order, limit), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindByFieldOrderedBy", entityClassName, "MongoDB",
                        "Unexpected error during findByFieldOrderedBy operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> long countByField(Class<T> entityClass, String field, Object value) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String collectionName = getTableName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document filter = new Document(field, value);

            try {
                return collection.countDocuments(filter);
            } catch (Exception e) {
                throw new DatabaseException("CountByField", entityClassName, "MongoDB",
                        String.format("MongoDB countByField operation failed for field '%s' with value %s", field, value), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("CountByField", entityClassName, "MongoDB",
                        "Unexpected error during countByField operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }
}
